package dev.snowdrop.factory.konflux;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dev.snowdrop.factory.Provider;
import dev.snowdrop.factory.Type;
import dev.snowdrop.model.*;
import dev.snowdrop.service.ConfiguratorSvc;
import dev.snowdrop.service.UriParserSvc;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.tekton.pipeline.v1.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static dev.snowdrop.factory.WorkflowResource.*;
//import static dev.snowdrop.factory.konflux.pipeline.Finally.KONFLUX_PIPELINE_FINALLY;
//import static dev.snowdrop.factory.konflux.pipeline.Results.KONFLUX_PIPELINE_RESULTS;
// import static dev.snowdrop.factory.konflux.pipeline.Tasks.*;
import static dev.snowdrop.factory.WorkfowResourceBuilder.*;
import static dev.snowdrop.factory.konflux.pipeline.Annotations.generateAnnotations;
import static dev.snowdrop.factory.konflux.pipeline.Labels.generateLabels;
import static dev.snowdrop.service.RemoteTaskSvc.BUNDLE_PREFIX;
import static dev.snowdrop.service.RemoteTaskSvc.fetchExtractTask;

public class KonfluxProvider implements Provider {

    private static final Logger logger = LoggerFactory.getLogger(KonfluxProvider.class);
    private static Type TYPE = null;
    private static final String BUILD_TASK_NAME = "build-container";

    ConfiguratorSvc configuratorSvc = ConfiguratorSvc.getInstance();
    List<PipelineTask> tasks = new ArrayList<>();

    @Override
    public HasMetadata buildResource(Configurator cfg, String resourceType) {
        TYPE = Type.valueOf(cfg.getType().toUpperCase());

        PipelineTask aTask;
        List<Action> actions = cfg.getJob().getActions();
        List<PipelineTask> finallyTasks = new ArrayList<>();
        List<Param> pipelineParams = new ArrayList<>();
        List<WorkspaceBinding> pipelineWorkspaces = new ArrayList<>();
        List<PipelineResult> pipelineResults = new ArrayList<>();

        if (cfg.getRepository() == null) {
            throw new RuntimeException("Git repository is missing");
        }

        if (actions.isEmpty()) {
            throw new RuntimeException("Actions are missing from the configuration");
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

            /* Check if there is a runAfter defined for the action, otherwise
               set the value to "prefetch-dependencies"

               TODO: Find a way to get the id of the previous's konflux task => git-clone

                Skip this step for finally tasks as not needed
             */
            String runAfter = null;
            if (!action.isFinally() && action.getRunAfter() != null) {
                runAfter = action.getRunAfter();
            }

            List<String> args = new ArrayList<>();
            if (action.getArgs() != null && action.getArgs().size() > 0) {
                args = action.getArgs();
            }

            List<When> whenList = populateWhenList(action);
            List<TaskResult> taskResults = new ArrayList<>();
            if (action.getResults() != null && action.getResults().size() > 0) {
                taskResults = populateTaskResults(action.getResults());
            }

            if (action.getScript() != null || action.getScriptFileUrl() != null) {
                aTask = createTaskWithEmbeddedScript(action, runAfter, args, whenList, jobWorkspacesMap, taskResults);
                if (action.isFinally()) {
                    finallyTasks.add(aTask);
                } else {
                    tasks.add(aTask);
                }
            }

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
                    if (action.isFinally()) {
                        finallyTasks.add(aTask);
                    } else {
                        tasks.add(aTask);
                    }
                }
            }
        }

        /* TODO: Code to be reviewed to avoid hard code values
         */
        tasks.stream()
            .filter(t ->
                t.getName().equals("buildpacks-extension-phases") ||
                    t.getName().equals("buildpacks-phases") ||
                    t.getName().equals("jam") ||
                    t.getName().equals("pack"))
            .map(t -> {
                t.setName(BUILD_TASK_NAME);
                return t;
            }).collect(Collectors.toList());

        /*
           If a default pipeline exists, then we will merge it with the new pipeline to be created but where we will
           remove from the default pipeline the dummy task "build-container" and insert the build tasks of the user
        */
        List<PipelineTask> pipelineTasks = new ArrayList<>();

        if (configuratorSvc.defaultPipeline != null) {
            PipelineRun defaultPipelineRun = (PipelineRun) configuratorSvc.defaultPipeline;
            List<PipelineTask> defaultPipelineTasks = defaultPipelineRun.getSpec().getPipelineSpec().getTasks();

            int indexToSearch = findBuildContainerIndex(BUILD_TASK_NAME, defaultPipelineTasks);

            if (indexToSearch != -1) {
                // Split the original list into two parts: before and after the "build-container" task
                List<PipelineTask> before = defaultPipelineTasks.subList(0, indexToSearch);
                List<PipelineTask> after = defaultPipelineTasks.subList(indexToSearch + 1, defaultPipelineTasks.size());

                // Create a new list by merging before, new tasks, and after
                pipelineTasks = Stream.concat(Stream.concat(before.stream(), tasks.stream()), after.stream())
                    .collect(Collectors.toList());
            } else {
                throw new RuntimeException("No build-container task found in the default Pipeline !");
            }

            // @formatter:off
            return new PipelineRunBuilder()
                .withNewMetadata()
                   .withName(cfg.getJob().getName()) // User's job name
                   .addToLabels(
                       Stream.concat(defaultPipelineRun.getMetadata().getLabels().entrySet().stream(),generateLabels(cfg).entrySet().stream())
                         .collect(Collectors.toMap(
                             Map.Entry::getKey,               // Key mapper
                             Map.Entry::getValue,             // Value mapper
                             (v1, v2) -> v1 + v2))
                   ) // Merge calculated new labels (based on Application/Component name, domain, etc.) with defaults
                   .addToAnnotations(
                       Stream.concat(defaultPipelineRun.getMetadata().getAnnotations().entrySet().stream(),generateAnnotations(cfg).entrySet().stream())
                           .collect(Collectors.toMap(
                               Map.Entry::getKey,               // Key mapper
                               Map.Entry::getValue,             // Value mapper
                               (v1, v2) -> v1 + v2))
                   ) // Merge calculated new annotation generated: build.appstudio.openshift.io/repo with defaults
                   .withNamespace(cfg.getNamespace()) // User's namespace
                .endMetadata()
                .withNewSpec()
                   .withWorkspaces(
                       Stream.concat(defaultPipelineRun.getSpec().getWorkspaces().stream(), pipelineWorkspaces.stream())
                           .collect(Collectors.toList())
                   )  // Merge default workspaces with user's workspace
                   .withParams(
                       Stream.concat(defaultPipelineRun.getSpec().getParams().stream(), pipelineParams.stream())
                           .collect(Collectors.toList())
                   ) // Merge default params with user's parameters
                   .withTimeouts( cfg.getJob().getTimeout() != null ? populateTimeOut(cfg.getJob().getTimeout()) : defaultPipelineRun.getSpec().getTimeouts()) // Default timeout
                   .withNewPipelineSpec()
                      .withResults(defaultPipelineRun.getSpec().getPipelineSpec().getResults())
                      .withFinally(defaultPipelineRun.getSpec().getPipelineSpec().getFinally())
                      .withTasks(pipelineTasks) // Default tasks where user's build tasks have been inserted
                   .endPipelineSpec()
                .endSpec()
                .build();
                // @formatter:on
        } else {
            Configurator defaultCfg = configuratorSvc.defaultConfigurator;

            // Add all the tasks found from the default pipeline configuration
            pipelineTasks.addAll(tasks);

            // @formatter:off
            return new PipelineRunBuilder()
                .withNewMetadata()
                   .withName(defaultCfg.getJob().getName())
                   .withLabels(populateMap(defaultCfg.getJob().getLabels()))
                   .withAnnotations(populateMap(defaultCfg.getJob().getAnnotations()))
                .endMetadata()
                .withNewSpec()
                   .withWorkspaces(pipelineWorkspaces)
                   .withParams(pipelineParams)
                   .withTimeouts(populateTimeOut(defaultCfg.getJob().getTimeout()))
                   .withNewPipelineSpec()
                      .withResults(pipelineResults)
                      .withFinally(finallyTasks)
                      .withTasks(pipelineTasks)
                   .endPipelineSpec()
                .endSpec()
                .build();
                // @formatter:on
        }
    }

    // Find the index of the "build-container" task in the list of the default pipeline
    private int findBuildContainerIndex(String taskToSearch, List<PipelineTask> tasks) {
        return IntStream.range(0, tasks.size())
            .filter(i -> tasks.get(i).getName().equals(taskToSearch))
            .findFirst()
            .orElse(-1);  // If not found, returns -1
    }

}