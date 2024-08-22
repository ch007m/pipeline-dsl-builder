package dev.snowdrop.factory.konflux.pipeline;

import dev.snowdrop.factory.AnnotationsProviderFactory;
import dev.snowdrop.factory.JobProvider;
import dev.snowdrop.factory.LabelsProviderFactory;
import dev.snowdrop.factory.Type;
import dev.snowdrop.factory.tekton.pipeline.TaskRefResolver;
import dev.snowdrop.model.Action;
import dev.snowdrop.model.Bundle;
import dev.snowdrop.model.Configurator;
import dev.snowdrop.model.Workspace;
import dev.snowdrop.service.UriParserSvc;
import io.fabric8.kubernetes.api.model.Duration;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.tekton.pipeline.v1.*;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.snowdrop.factory.TektonResource.populateTimeOut;
import static dev.snowdrop.factory.konflux.pipeline.Finally.KONFLUX_PIPELINE_FINALLY;
import static dev.snowdrop.factory.konflux.pipeline.Params.KONFLUX_PIPELINERUN_PARAMS;
import static dev.snowdrop.factory.konflux.pipeline.Results.KONFLUX_PIPELINE_RESULTS;
import static dev.snowdrop.factory.konflux.pipeline.Tasks.*;
import static dev.snowdrop.factory.konflux.pipeline.Workspaces.KONFLUX_PIPELINERUN_WORKSPACES;
import static dev.snowdrop.factory.tekton.pipeline.Pipelines.populatePipelineParams;
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

        if (cfg.getRepository() == null) {
            throw new RuntimeException("Git repository is missing");
        }

        if (actions.isEmpty()) {
            throw new RuntimeException("Actions are missing from the configuration");
        }

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
                            Constructor constructor = new Constructor(Task.class, new LoaderOptions());
                            Yaml yaml = new Yaml(constructor);
                            try {
                                Path yamlPath = Path.of(file);
                                Task task = yaml.load(Files.readString(yamlPath));

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
                    aTask = createTaskUsingRef(action, runAfter, bundle, taskRefMap);
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
                .endMetadata()
                .withNewSpec()
                   .withWorkspaces(KONFLUX_PIPELINERUN_WORKSPACES())
                   .withParams(KONFLUX_PIPELINERUN_PARAMS())
                   .withTimeouts(populateTimeOut("1h0m0s"))
                   .withNewPipelineSpec()
                      .withResults(KONFLUX_PIPELINE_RESULTS())
                      .withFinally(KONFLUX_PIPELINE_FINALLY())
                      .withTasks(pipelineTasks.toArray(new PipelineTask[0]))
                   .endPipelineSpec()
                .endSpec()
                .build();
                // @formatter:on

        // TODO: Add like with Tekton a switch to handle: Pipeline vs PipelineRun
        return pipeline;
    }

    private static PipelineTask createTaskUsingRef(Action action, String runAfter, Bundle bundle, Map<String, Task> taskRefMap) {
        // List of workspaces defined for the referenced's task
        // List<WorkspaceDeclaration> taskWorkspaces = taskRefMap.get(action.getName()).getSpec().getWorkspaces();

        // Generate the Pipeline's task
        PipelineTask pipelineTask = new PipelineTaskBuilder()
            // @formatter:off
            .withName(action.getName())
            .withRunAfter(runAfter != null ? Collections.singletonList(runAfter) : null)
            // TODO: Include needed workspaces
            .withTaskRef(TaskRefResolver.withReference(bundle, action.getName()))
            .withParams(action.getParams() != null ? populatePipelineParams(action.getParams()) : null)
            .build();
        // @formatter:on
        return pipelineTask;
    }
}