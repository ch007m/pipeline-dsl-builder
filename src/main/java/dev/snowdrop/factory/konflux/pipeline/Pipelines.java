package dev.snowdrop.factory.konflux.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dev.snowdrop.factory.AnnotationsProviderFactory;
import dev.snowdrop.factory.JobProvider;
import dev.snowdrop.factory.LabelsProviderFactory;
import dev.snowdrop.factory.Type;
import dev.snowdrop.factory.tekton.pipeline.TaskRefResolver;
import dev.snowdrop.model.*;
import dev.snowdrop.service.UriParserSvc;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.tekton.pipeline.v1.*;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.snowdrop.factory.TektonResource.*;
import static dev.snowdrop.factory.konflux.pipeline.Finally.KONFLUX_PIPELINE_FINALLY;
import static dev.snowdrop.factory.konflux.pipeline.Results.KONFLUX_PIPELINE_RESULTS;
import static dev.snowdrop.factory.konflux.pipeline.Tasks.*;
import static dev.snowdrop.service.RemoteTaskSvc.BUNDLE_PREFIX;
import static dev.snowdrop.service.RemoteTaskSvc.fetchExtractTask;

public class Pipelines implements JobProvider {

    //private static final Logger logger = LoggerFactory.getLogger(Pipelines.class);
    private static Type TYPE = null;

    @Override
    public HasMetadata generatePipeline(Configurator cfg) {
        TYPE = Type.valueOf(cfg.getType().toUpperCase());

        PipelineTask aTask;
        List<Action> actions = cfg.getJob().getActions();
        List<PipelineTask> tasks = new ArrayList<>();
        List<Param> pipelineParams = new ArrayList<>();
        List<WorkspaceBinding> pipelineWorkspaces = new ArrayList<>();

        if (cfg.getRepository() == null) {
            throw new RuntimeException("Git repository is missing");
        }

        if (actions.isEmpty()) {
            throw new RuntimeException("Actions are missing from the configuration");
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
            // set the value to "prefetch-dependencies"
            String runAfter = null;
            if (action.getRunAfter() != null) {
                runAfter = action.getRunAfter();
            } else {
                // TODO: Find a way to get the id of the previous's konflux task
                runAfter = "prefetch-dependencies";
            }

            List<When> whenList = populateWhenList(action);

            if (action.getScript() != null || action.getScriptFileUrl() != null) {
                aTask = createTaskWithEmbeddedScript(action, runAfter, whenList, jobWorkspacesMap);
                tasks.add(aTask);
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
                    tasks.add(aTask);
                }
            }
        }

        // @formatter:off
        List<PipelineTask> pipelineTasks = new ArrayList<>();
        pipelineTasks.add(INIT());
        pipelineTasks.add(CLONE_REPOSITORY());
        pipelineTasks.add(PREFETCH_DEPENDENCIES());
        pipelineTasks.addAll(tasks);
        pipelineTasks.add(BUILD_IMAGE_INDEX());
        pipelineTasks.add(BUILD_SOURCE_IMAGE());
        pipelineTasks.add(DEPRECATED_BASE_IMAGE_CHECK());
        pipelineTasks.add(CLAIR_SCAN());
        pipelineTasks.add(ECOSYSTEM_CERT_PREFLIGHT_CHECKS());
        pipelineTasks.add(SAST_SNYK_CHECK());
        pipelineTasks.add(CLAMAV_SCAN());
        pipelineTasks.add(SBOM_JSON_CHECK());

        PipelineRun pipeline = new PipelineRunBuilder()
                .withNewMetadata()
                   .withName(cfg.getJob().getName())
                   .withLabels(LabelsProviderFactory.getProvider(TYPE).getPipelineLabels(cfg))
                   .withAnnotations(AnnotationsProviderFactory.getProvider(TYPE).getPipelineAnnotations(cfg))
                   .withNamespace(cfg.getNamespace())
                .endMetadata()
                .withNewSpec()
                   .withWorkspaces(pipelineWorkspaces)
                   .withParams(pipelineParams)
                   .withTimeouts(populateTimeOut("1h0m0s"))
                   .withNewPipelineSpec()
                      .withResults(KONFLUX_PIPELINE_RESULTS())
                      .withFinally(KONFLUX_PIPELINE_FINALLY())
                      //.withParams(KONFLUX_PIPELINESPEC_PARAMS())
                      .withTasks(pipelineTasks.toArray(new PipelineTask[0]))
                   .endPipelineSpec()
                .endSpec()
                .build();
                // @formatter:on

        // TODO: Add like with Tekton a switch to handle: Pipeline vs PipelineRun
        return pipeline;
    }

    // TODO: This method should be replaced with the common one of the TektonResource class
    // That will be possible when we will find a way to swap the name as for konflux, the name should be "build-container"
    // to insert it within the list
    private static PipelineTask createTaskUsingRef(Action action, String runAfter, Bundle bundle, Map<String, Workspace> jobWorkspacesMap, Map<String, Task> taskRefMap) {
        // List of workspaces defined for the referenced's task
        List<WorkspaceDeclaration> taskWorkspaces = taskRefMap.get(action.getName()).getSpec().getWorkspaces();

        // Generate the Pipeline's task
        PipelineTask pipelineTask = new PipelineTaskBuilder()
            // @formatter:off
            .withName("build-container")
            .withRunAfter(runAfter != null ? Collections.singletonList(runAfter) : null)
            .withWorkspaces(populateTaskWorkspaces(action, jobWorkspacesMap, taskWorkspaces))
            .withTaskRef(TaskRefResolver.withReference(bundle, action.getName()))
            .withParams(action.getParams() != null ? populatePipelineParams(action.getParams()) : null)
            .build();
        // @formatter:on
        return pipelineTask;
    }
}