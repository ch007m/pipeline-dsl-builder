package dev.snowdrop.factory.konflux.pipeline;

import dev.snowdrop.factory.AnnotationsProviderFactory;
import dev.snowdrop.factory.JobProvider;
import dev.snowdrop.factory.LabelsProviderFactory;
import dev.snowdrop.factory.Type;
import dev.snowdrop.factory.tekton.pipeline.TaskRefResolver;
import dev.snowdrop.model.Action;
import dev.snowdrop.model.Bundle;
import dev.snowdrop.model.Configurator;
import dev.snowdrop.service.UriParserSvc;
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
import static dev.snowdrop.factory.konflux.pipeline.Params.KONFLUX_PIPELINE_PARAMS;
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
        PipelineRun pipeline = new PipelineRunBuilder()
                .withNewMetadata()
                   .withName(cfg.getJob().getName())
                   .withLabels(LabelsProviderFactory.getProvider(TYPE).getPipelineLabels(cfg))
                   .withAnnotations(AnnotationsProviderFactory.getProvider(TYPE).getPipelineAnnotations(cfg))
                   .withNamespace(cfg.getNamespace())
                .endMetadata()
                .withNewSpec()
                   .withWorkspaces(KONFLUX_PIPELINERUN_WORKSPACES())
                   .withParams(KONFLUX_PIPELINERUN_PARAMS())
                   .withTimeouts(populateTimeOut("1h0m0s"))
                   .withNewPipelineSpec()
                      .withResults(KONFLUX_PIPELINE_RESULTS())
                      .withFinally(KONFLUX_PIPELINE_FINALLY())
                      .withParams(KONFLUX_PIPELINE_PARAMS())
                      .withTasks(
                         INIT(),
                         CLONE_REPOSITORY(),
                         PREFETCH_DEPENDENCIES(),
                         // We add here the build container action(s) of the user
                         // TODO
                         /*
                          As the list of the tasks to be passed to the Tekton PipelineRun cannot be passed
                          as a list of objects but instead as a list created according to the following logic:

                          from("git-clone")
                            .to("t1").runAfter("git-clone")
                            .to("t2").runAfter("t1")
                            .to("user-build-task1").runAfter("t2")
                            .to("user-build-task2").runAfter("user-build-task1")
                            ...
                            .to("build-image-index").runAfter("last-user-task => build-user-task2")
                          ...
                          then we should find a way to modelize such an Array of PipelineTask
                          */
                         tasks.get(0),
                         BUILD_IMAGE_INDEX(),
                         BUILD_SOURCE_IMAGE(),
                         DEPRECATED_BASE_IMAGE_CHECK(),
                         CLAIR_SCAN(),
                         ECOSYSTEM_CERT_PREFLIGHT_CHECKS(),
                         SAST_SNYK_CHECK(),
                         CLAMAV_SCAN(),
                         SBOM_JSON_CHECK()
                      )
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
            .withName("build-container") // TODO: Find a way to avoid to hard code it here
            .withRunAfter(runAfter != null ? Collections.singletonList(runAfter) : null)
            // TODO: Avoid to hard code
            .withWorkspaces()
              .addNewWorkspace().withName("source-dir").withWorkspace("workspace").endWorkspace()
              .addNewWorkspace().withName("pack-workspace").withWorkspace("workspace").endWorkspace()
            .withTaskRef(TaskRefResolver.withReference(bundle, action.getName()))
            .withParams(action.getParams() != null ? populatePipelineParams(action.getParams()) : null)
            .build();
        // @formatter:on
        return pipelineTask;
    }
}