package dev.snowdrop.factory.tekton.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dev.snowdrop.factory.AnnotationsProviderFactory;
import dev.snowdrop.factory.JobProvider;
import dev.snowdrop.factory.LabelsProviderFactory;
import dev.snowdrop.factory.Type;
import dev.snowdrop.model.*;
import dev.snowdrop.service.UriParserSvc;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.tekton.pipeline.v1.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.snowdrop.factory.TektonResource.*;
import static dev.snowdrop.service.RemoteTaskSvc.BUNDLE_PREFIX;
import static dev.snowdrop.service.RemoteTaskSvc.fetchExtractTask;

public class Pipelines implements JobProvider {

    private static final Logger logger = LoggerFactory.getLogger(Pipelines.class);
    private static Type TYPE;

    @Override
    public HasMetadata generatePipeline(Configurator cfg) {
        TYPE = Type.valueOf(cfg.getType().toUpperCase());
        @SuppressWarnings("unchecked")
        PipelineTask aTask;

        List<Action> actions = cfg.getJob().getActions();
        List<PipelineTask> tasks = new ArrayList<>();
        List<Param> pipelineParams = new ArrayList<>();
        List<WorkspaceBinding> pipelineWorkspaces = new ArrayList<>();
        List<PipelineResult> pipelineResults = new ArrayList<>();

        if (actions.isEmpty()) {
            throw new RuntimeException("Actions are missing from the configuration");
        }

        Map<Integer, Action> actionOrderMap = Optional.ofNullable(cfg.getJob().getActions())
            .orElse(Collections.emptyList()) // Handle null case by providing an empty list
            .stream()
            .collect(Collectors.toMap(Action::getId, id -> id));

        String tektonResourceType = cfg.getJob().getResourceType().toLowerCase();
        if (tektonResourceType == null) {
            throw new RuntimeException("Missing tekton resource type");
        }

        List<Map<String, String>> results = cfg.getJob().getResults();
        if (Optional.ofNullable(results).map(List::size).orElse(0) > 0) {
            pipelineResults = populatePipelineResults(results);
        }

        List<Workspace> wks = cfg.getJob().getWorkspaces();
        if (Optional.ofNullable(wks).map(List::size).orElse(0) > 0) {
            pipelineWorkspaces = populatePipelineWorkspaces(wks);
        }

        List<Map<String, Object>> params = cfg.getJob().getParams();
        if (params != null && !params.isEmpty()) {
            pipelineParams = populatePipelineParams(cfg.getJob().getParams());
        }

        // Create a HashMap of the job workspaces using as's key the workspace's name
        Map<String, Workspace> jobWorkspacesMap = Optional.ofNullable(cfg.getJob().getWorkspaces())
            .orElse(Collections.emptyList())
            .stream()
            .collect(Collectors.toMap(Workspace::getName, name -> name));

        for (Action action : actions) {

            // Check if there is a runAfter defined for the action, otherwise
            // add runAfter if action.id > 1 and get action where id = id -1
            String runAfter = null;
            if (action.getRunAfter() != null) {
                runAfter = action.getRunAfter();
            } else {
                if (action.getId() > 1) {
                    runAfter = actionOrderMap.get(action.getId() - 1).getName();
                }
            }

            List<String> argList = new ArrayList<>();
            if (action.getArgs() != null && action.getArgs().size() > 0 ) {
                argList = action.getArgs();
            }

            List<When> whenList = populateWhenList(action);
            List<TaskResult> taskResults = populateTaskResults(action.getResults());

            if (action.getRef() != null) {
                // Create a Bundle using the action reference
                // bundle://<REGISTRY>/<ORG>/<BUNDLE_TASK-NAME>:<VERSION>@sha256:<SHA256>
                // or
                // git://<GIT_URL>/<TASK>.yaml
                Bundle bundle = UriParserSvc.extract(action.getRef());

                if (bundle == null) {
                    //logger.error("Bundle reference was not parsed properly");
                    throw new RuntimeException("Bundle reference was not parsed properly using: " + action.getRef());
                } else {
                    // Fetch the content of the task using the remote URL: Git url, oci bundle, etc
                    fetchExtractTask(bundle, action.getName(), cfg.getOutputPath());

                    // Walk through the yaml task files and create the Task object
                    String bundlePath = Paths.get(cfg.getOutputPath(), BUNDLE_PREFIX, action.getName()).toString();
                    Path tasksPath = Paths.get(bundlePath, "tasks");
                    Map<String, Task> taskRefMap = new HashMap<>();

                    try (Stream<Path> filesWalk = Files.walk(tasksPath)) {
                        List<String> result = filesWalk
                            .filter(Files::isRegularFile)
                            .distinct() // Remove duplicates
                            .map(x -> x.toString())
                            .collect(Collectors.toList());

                        result.forEach(file -> {
                            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                            try {
                                Task task = mapper.readValue(Path.of(file).toFile(), Task.class);

                                // The task name should be the same as the fileName and will be used as key
                                String taskName = task.getMetadata().getName();
                                taskRefMap.put(taskName, task);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // Generate the Task
                    aTask = createTaskUsingRef(action, runAfter, bundle, jobWorkspacesMap, taskRefMap);
                    tasks.add(aTask);
                }

            }

            if (action.getScript() != null || action.getScriptFileUrl() != null) {
                aTask = createTaskWithEmbeddedScript(action, runAfter, argList, whenList, jobWorkspacesMap, taskResults);
                tasks.add(aTask);
            }
        }

        switch (tektonResourceType) {
            case "pipelinerun":
                return generatePipelineRun(cfg, tasks, pipelineParams, pipelineWorkspaces, pipelineResults);

            case "pipeline":
                return generatePipeline(cfg, tasks, pipelineWorkspaces);

            default:
                throw new RuntimeException("Invalid type not supported: " + tektonResourceType);
        }
    }

    private static PipelineTask createTaskUsingRef(Action action, String runAfter, Bundle bundle, Map<String, Workspace> jobWorkspacesMap, Map<String, Task> taskRefMap) {
        // List of workspaces defined for the referenced's task
        List<WorkspaceDeclaration> taskWorkspaces = taskRefMap.get(action.getName()).getSpec().getWorkspaces();

        // Generate the Pipeline's task
        PipelineTask pipelineTask = new PipelineTaskBuilder()
            // @formatter:off
            .withName(action.getName())
            .withRunAfter(runAfter != null ? Collections.singletonList(runAfter) : null)
            .withTaskRef(TaskRefResolver.withReference(bundle, action.getName()))
            .withWorkspaces(populateTaskWorkspaces(action, jobWorkspacesMap, taskWorkspaces))
            .withParams(action.getParams() != null ? populatePipelineParams(action.getParams()) : null)
            .build();
            // @formatter:on
        return pipelineTask;
    }

    public static PipelineRun generatePipelineRun(Configurator cfg, List<PipelineTask> tasks, List<Param> params, List<WorkspaceBinding> pipelineWorkspaces, List<PipelineResult> pipelineResults) {
        // @formatter:off
        PipelineRun pipelineRun = new PipelineRunBuilder()
          .withNewMetadata()
             .withName(cfg.getJob().getName())
             .withLabels(LabelsProviderFactory.getProvider(TYPE).getPipelineLabels(cfg))
             .withAnnotations(AnnotationsProviderFactory.getProvider(TYPE).getPipelineAnnotations(cfg))
             .withNamespace(cfg.getNamespace())
          .endMetadata()
          .withNewSpec()
             .withParams(params)
             .withWorkspaces(pipelineWorkspaces)
             .withTimeouts(populateTimeOut(cfg.getJob().getTimeout()))
             .withNewPipelineSpec()
                .withResults(pipelineResults)
                .withTasks(tasks)
             .endPipelineSpec()
          .endSpec()
          .build();
        // @formatter:on
        return pipelineRun;
    }

    public static Pipeline generatePipeline(Configurator cfg, List<PipelineTask> tasks, List<WorkspaceBinding> pipelineWorkspaces) {
        // TODO: To be reviewed
        // @formatter:off
        Pipeline pipeline = new PipelineBuilder()
            .withNewMetadata()
               .withName(cfg.getJob().getName())
               .withLabels(LabelsProviderFactory.getProvider(TYPE).getPipelineLabels(cfg))
               .withAnnotations(AnnotationsProviderFactory.getProvider(TYPE).getPipelineAnnotations(cfg))
               .withNamespace(cfg.getNamespace())
            .endMetadata()
            .withNewSpec()
               .withTasks(tasks)
            .endSpec()
            .build();
        // @formatter:on
        return pipeline;
    }


}