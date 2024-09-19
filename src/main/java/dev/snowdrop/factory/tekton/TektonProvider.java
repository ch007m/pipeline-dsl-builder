package dev.snowdrop.factory.tekton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dev.snowdrop.factory.Provider;
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
import static dev.snowdrop.factory.tekton.PipelineBuilder.generatePipeline;
import static dev.snowdrop.factory.tekton.PipelineRunBuilder.generatePipelineRun;
import static dev.snowdrop.factory.tekton.TaskRunBuilder.generateTaskRun;
import static dev.snowdrop.service.RemoteTaskSvc.BUNDLE_PREFIX;
import static dev.snowdrop.service.RemoteTaskSvc.fetchExtractTask;

public class TektonProvider implements Provider {

    private static final Logger logger = LoggerFactory.getLogger(TektonProvider.class);
    private static Type TYPE;

    @Override
    public HasMetadata buildResource(Configurator cfg, String tektonResourceType) {
        TYPE = Type.valueOf(cfg.getType().toUpperCase());
        @SuppressWarnings("unchecked")
        PipelineTask aTask;

        List<Action> actions = cfg.getJob().getActions();
        List<PipelineTask> tasks = new ArrayList<>();
        List<PipelineTask> finallyTasks = new ArrayList<>();
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
            List<TaskResult> taskResults = new ArrayList<>();
            if (action.getResults() != null && action.getResults().size() > 0 ) {
                taskResults = populateTaskResults(action.getResults());
            }

            if (action.getRef() != null) {
                /* Create a Bundle using the action reference
                   bundle://<REGISTRY>/<ORG>/<BUNDLE_TASK-NAME>:<VERSION>@sha256:<SHA256>
                   or
                   git://<GIT_URL>/<TASK>.yaml
                   or
                   url://https://<RAW_GIT_URL>/<TASK>.yaml
                 */

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

            if (action.getScript() != null || action.getScriptFileUrl() != null) {
                aTask = createTaskWithEmbeddedScript(action, runAfter, argList, whenList, jobWorkspacesMap, taskResults);
                if (action.isFinally()) {
                    finallyTasks.add(aTask);
                } else {
                    tasks.add(aTask);
                }
            }
        }

        switch (tektonResourceType) {
            case "pipelinerun":
                return generatePipelineRun(TYPE, cfg, tasks, pipelineParams, pipelineWorkspaces, pipelineResults);

            // TODO: To be reviewed
            case "pipeline":
                return generatePipeline(TYPE, cfg, tasks, pipelineWorkspaces);

            // TODO: To be developed
            case "taskrun":
                return generateTaskRun(TYPE, cfg, pipelineWorkspaces);

            default:
                throw new RuntimeException("Invalid type not supported: " + tektonResourceType);
        }
    }

}