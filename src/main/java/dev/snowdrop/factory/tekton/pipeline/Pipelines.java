package dev.snowdrop.factory.tekton.pipeline;

import dev.snowdrop.factory.AnnotationsProviderFactory;
import dev.snowdrop.factory.LabelsProviderFactory;
import dev.snowdrop.factory.Type;
import dev.snowdrop.model.Volume;
import dev.snowdrop.model.*;
import dev.snowdrop.service.FileUtilSvc;
import dev.snowdrop.service.UriParserSvc;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.tekton.pipeline.v1.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.snowdrop.factory.Bundles.getBundleURL;
import static dev.snowdrop.service.RemoteTaskSvc.BUNDLE_PREFIX;
import static dev.snowdrop.service.RemoteTaskSvc.fetchExtractTask;

public class Pipelines {

    private static final Logger logger = LoggerFactory.getLogger(Pipelines.class);
    private static Type TYPE;

    public static <T> T createResource(Configurator cfg) {
        Class<T> type;
        List<Action> actions = cfg.getJob().getActions();
        String domain = cfg.getDomain().toUpperCase();

        if (domain == null) {
            throw new RuntimeException("Missing domain");
        }

        if (actions.isEmpty()) {
            throw new RuntimeException("Missing actions");
        }

        return createJob(cfg, actions);
    }

    public static <T> T createJob(Configurator cfg, List<Action> actions) {
        TYPE = Type.valueOf(cfg.getType().toUpperCase());

        Class<T> type;
        PipelineTask aTask;
        List<PipelineTask> tasks = new ArrayList<>();
        List<Param> pipelineParams = new ArrayList<>();
        List<WorkspaceBinding> pipelineWorkspaces = new ArrayList<>();

        Map<Integer, Action> actionOrderMap = Optional.ofNullable(cfg.getJob().getActions())
            .orElse(Collections.emptyList()) // Handle null case by providing an empty list
            .stream()
            .collect(Collectors.toMap(Action::getId, id -> id));

        String tektonResourceType = cfg.getJob().getResourceType().toLowerCase();
        if (tektonResourceType == null) {
            throw new RuntimeException("Missing tekton resource type");
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

            List<When> whenList = new ArrayList<>();
            if (action.getWhen() != null && action.getWhen().size() > 0) {
                action.getWhen().stream().forEach(w -> {
                    String operator = "in";
                    String[] res= w.split(":");
                    whenList.add(
                        new When()
                            .input(res[0])
                            .operator(operator)
                            .values(List.of(res[1].trim()))
                    );
                });
            }

            if (action.getRef() != null) {
                // Create a Bundle using the action reference
                // bundle://<REGISTRY>/<ORG>/<BUNDLE_TASK-NAME>:<VERSION>@sha256:<SHA256>
                // or
                // git://<GIT_URL>/<TASK>.yaml
                Bundle bundle = UriParserSvc.extract(action.getRef());

                if (bundle == null) {
                    //logger.error("Bundle reference was not parsed properly");
                    throw new RuntimeException("Bundle reference was not parsed properly");
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
                    aTask = createTaskUsingRef(action, runAfter, bundle, jobWorkspacesMap, taskRefMap);
                    tasks.add(aTask);
                }

            }

            if (action.getScript() != null || action.getScriptFileUrl() != null) {
                aTask = createTaskWithEmbeddedScript(action, runAfter, whenList, jobWorkspacesMap);
                tasks.add(aTask);
            }
        }

        switch (tektonResourceType) {
            case "pipelinerun":
                type = (Class<T>) PipelineRun.class;
                return type.cast(generatePipelineRun(cfg, tasks, pipelineParams, pipelineWorkspaces));

            case "pipeline":
                type = (Class<T>) Pipeline.class;
                return type.cast(generatePipeline(cfg, tasks, pipelineWorkspaces));

            default:
                throw new RuntimeException("Invalid type not supported: " + tektonResourceType);
        }
    }

    private static List<WorkspacePipelineTaskBinding> populateTaskWorkspaces(Action action, Map<String, Workspace> jobWorkspacesMap, List<WorkspaceDeclaration> taskWorkspaces) {
        // List of workspaces to be generated by this method
        List<WorkspacePipelineTaskBinding> wksPipelineTask = new ArrayList<>();

        // List of workspaces declared within the Configuration's action
        List<Workspace> wksAction = action.getWorkspaces();

        // Map of workspaces from Job and action (= task) using as key, the workspace's name
        Map<String, Workspace> wksMerged = new HashMap<>();

        // Create a Map of the action's workspaces using as key the workspace's name
        Map<String, Workspace> actionWorkspacesMap = Optional.ofNullable(action.getWorkspaces())
            .orElse(Collections.emptyList()) // Handle null case by providing an empty list
            .stream()
            .collect(Collectors.toMap(Workspace::getName, name -> name));

        wksMerged.putAll(jobWorkspacesMap);
        wksMerged.putAll(actionWorkspacesMap);

        // As the task referenced includes workspaces, we will try to match them with the job's workspaces
        if (taskWorkspaces != null && !taskWorkspaces.isEmpty()) {
            taskWorkspaces.stream().forEach(wks -> {
                String wksNameToSearch = wks.getName();

                if (wksMerged.containsKey(wksNameToSearch)) {
                    // Job's workspace and task's workspace matches
                    // We will now check if name
                    logger.info("Match found using as key: " + wksNameToSearch);

                    Workspace wksMatching = wksMerged.get(wksNameToSearch);

                    WorkspacePipelineTaskBindingBuilder wuBuilder = new WorkspacePipelineTaskBindingBuilder();
                    wuBuilder
                        .withName(wksMatching.getName())
                        .withWorkspace(wksMatching.getWorkspace() != null ? wksMatching.getWorkspace() : wksMatching.getName())
                        .build();
                    wksPipelineTask.add(wuBuilder.build());
                }
            });

        } else {
            // taskWorkspaces can be null - empty when we generate a Pipeline using TaskSpec and embedded script
            // In this case, we will check if wks have been defined at the level of the action
            // and if the wks exisyts within the Job's workspace
            actionWorkspacesMap.keySet().forEach(k -> {
                if (wksMerged.containsKey(k) && jobWorkspacesMap.containsKey(k)) {
                    // Job's workspace and task's workspace matches
                    // We will now check if name
                    logger.info("Match found using as key: " + k);

                    Workspace wksMatching = wksMerged.get(k);

                    WorkspacePipelineTaskBindingBuilder wuBuilder = new WorkspacePipelineTaskBindingBuilder();
                    wuBuilder
                        .withName(wksMatching.getName())
                        .withWorkspace(wksMatching.getWorkspace() != null ? wksMatching.getWorkspace() : wksMatching.getName())
                        .build();
                    wksPipelineTask.add(wuBuilder.build());
                } else {
                    throw new RuntimeException("The following workspace has not defined at the job's level: " + k);
                }
            });

            return wksPipelineTask;
        }
        return wksPipelineTask;
    }

    private static PipelineTask createTaskWithEmbeddedScript(Action action, String runAfter, List<When> when, Map<String, Workspace> jobWorkspacesMap) {
        String embeddedScript;

        if (action.getScript() != null) {
            embeddedScript = action.getScript();
        } else if (action.getScriptFileUrl() != null) {
            try {
                embeddedScript = FileUtilSvc.fetchScriptFileContent(action.getScriptFileUrl());
            } catch (IOException e) {
                throw new RuntimeException("Cannot fetch the script file: " + action.getScriptFileUrl(), e);
            }
        } else {
            throw new RuntimeException("No embedded script configured");
        }

        PipelineTask pipelineTask = new PipelineTaskBuilder()
            // @formatter:off
            .withName(action.getName())
            .withRunAfter(runAfter != null ? Collections.singletonList(runAfter) : null)
            .withWhen().addToWhen(
                when.stream()
                .map(w -> new WhenExpressionBuilder()
                        .withInput(w.getInput())
                        .withOperator(w.getOperator())
                        .withValues(w.getValues())
                        .build())
                    .toArray(WhenExpression[]::new))
            .withParams(action.getParams() != null ? populatePipelineParams(action.getParams()) : null)
            .withWorkspaces(populateTaskWorkspaces(action, jobWorkspacesMap, null))
            .withTaskSpec(
                new EmbeddedTaskBuilder()
                  .addNewStep()
                    .withName("run-script")
                    .withImage(action.STEP_SCRIPT_IMAGE)
                    .withScript(embeddedScript)
                  .endStep()
                  .build())
            .build();
        // @formatter:on
        return pipelineTask;
    }

    private static PipelineTask createTaskUsingRef(Action action, String runAfter, Bundle bundle, Map<String, Workspace> jobWorkspacesMap, Map<String, Task> taskRefMap) {
        // List of workspaces defined for the referenced's task
        List<WorkspaceDeclaration> taskWorkspaces = taskRefMap.get(action.getName()).getSpec().getWorkspaces();

        // Generate the Pipeline's task
        PipelineTask pipelineTask = new PipelineTaskBuilder()
            // @formatter:off
                .withName(action.getName())
                .withRunAfter(runAfter != null ? Collections.singletonList(runAfter) : null)
                .withNewTaskRef()
                  .withResolver("bundles")
                  .withParams()
                    .addNewParam().withName("bundle").withValue(new ParamValue(bundle.getUri())).endParam()
                    // The name of the task to be fetched should be equal to the name of the Action's name !!
                    .addNewParam().withName("name").withValue(new ParamValue(action.getName())).endParam()
                    .addNewParam().withName("kind").withValue(new ParamValue("task")).endParam()
                  .endTaskRef()
                .withWorkspaces(populateTaskWorkspaces(action, jobWorkspacesMap, taskWorkspaces))
                .withParams(action.getParams() != null ? populatePipelineParams(action.getParams()) : null)
                .build();
            // @formatter:on
        return pipelineTask;
    }

    private static List<WorkspaceBinding> populatePipelineWorkspaces(List<Workspace> wks) {
        List<WorkspaceBinding> workspaceList = new ArrayList<>();
        for (Workspace wk : wks) {
            WorkspaceBindingBuilder binding = new WorkspaceBindingBuilder();
            binding.withName(wk.getName());

            if (wk.getVolumeClaimTemplate() != null) {
                dev.snowdrop.model.Volume v = wk.getVolumeClaimTemplate();
                // @formatter:off
                binding.withVolumeClaimTemplate(
                    new PersistentVolumeClaimBuilder()
                    .editOrNewSpec()
                      .withResources(
                        new VolumeResourceRequirementsBuilder()
                          .addToRequests(
                              v.STORAGE,
                              new Quantity(v.getStorage()))
                          .build()
                        )
                        .addToAccessModes(v.getAccessMode())
                       .endSpec()
                    .build()
                );
                // @formatter:on
                workspaceList.add(binding.build());
            }

            // If volumes size > 0 than we assume that the user would like to use: Projected
            if (wk.getVolumeSources() != null && wk.getVolumeSources().size() > 0) {
                ProjectedVolumeSourceBuilder pvsb = new ProjectedVolumeSourceBuilder();
                for (Volume v : wk.getVolumeSources()) {
                    if (v.getSecret() != null) {
                        pvsb.addNewSource()
                            .withSecret(new SecretProjectionBuilder().withName(v.getSecret()).build())
                            .endSource();
                    }
                }
                binding.withProjected(pvsb.build());
                workspaceList.add(binding.build());
            }

        }
        return workspaceList;
    }

    private static List<Param> populatePipelineParams(List<Map<String, Object>> params) {
        List<Param> paramList = new ArrayList<>();
        for (Map<String, Object> hash : params) {
            hash.forEach((key, val) -> {
                    ParamValue paramValue;
                    if (val instanceof String) {
                        paramValue = new ParamValue((String) val);
                    } else if (val instanceof Boolean) {
                        paramValue = new ParamValue(Boolean.toString((Boolean) val));
                    } else if (val instanceof List<?>) {
                        paramValue = new ParamValue((List<String>) val);
                    } else {
                        paramValue = new ParamValue(String.valueOf(val)); // Default to String representation
                    }

                    paramList.add(new ParamBuilder().withName(key).withValue(paramValue).build());
                }
            );
        }
        return paramList;
    }

    public static PipelineRun generatePipelineRun(Configurator cfg, List<PipelineTask> tasks, List<Param> params, List<WorkspaceBinding> pipelineWorkspaces) {
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
             .withNewPipelineSpec()
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

    public static PipelineRun createPackBuilder(Configurator cfg) {
        TYPE = Type.valueOf(cfg.getType().toUpperCase());

        // @formatter:off
      PipelineRun pr = new PipelineRunBuilder()
          .withNewMetadata()
             .withName(cfg.getJob().getName())
             .withLabels(LabelsProviderFactory.getProvider(TYPE).getPipelineLabels(cfg))
             .withAnnotations(AnnotationsProviderFactory.getProvider(TYPE).getPipelineAnnotations(cfg))
          .endMetadata()
          .withNewSpec()
             .withParams()
                .addNewParam().withName("debug").withValue(new ParamValue("true")).endParam()
                .addNewParam().withName("git-url").withValue(new ParamValue("https://github.com/redhat-buildpacks/ubi-image-builder.git")).endParam()
                .addNewParam().withName("source-dir").withValue(new ParamValue(".")).endParam()
                .addNewParam().withName("output-image").withValue(new ParamValue("quay.io/snowdrop/ubi-builder")).endParam()
                .addNewParam().withName("imageUrl").withValue(new ParamValue("buildpacksio/pack")).endParam()
                .addNewParam().withName("imageTag").withValue(new ParamValue("latest")).endParam()
                .addNewParam().withName("packCmdBuilderFlags").withValue(new ParamValue(List.of("-v", "--publish"))).endParam()
             .withNewPipelineSpec()
                .addNewTask()
                   .withName("git-clone")
                   .withNewTaskRef()
                     .withResolver("bundles")
                     .withParams()
                       .addNewParam().withName("bundle").withValue(new ParamValue(getBundleURL("quay.io/konflux-ci/tekton-catalog", "task-git-clone","0.1"))).endParam()
                       .addNewParam().withName("name").withValue(new ParamValue("git-clone")).endParam()
                       .addNewParam().withName("kind").withValue(new ParamValue("task")).endParam()
                   .endTaskRef()
                   .withParams()
                      .addNewParam().withName("url").withValue(new ParamValue("$(params.git-url)")).endParam()
                      .addNewParam().withName("subdirectory").withValue(new ParamValue(".")).endParam()
                   .withWorkspaces()
                      .addNewWorkspace().withName("output").withWorkspace("source-dir").endWorkspace()
                .endTask()

                .addNewTask()
                   .withName("fetch-packconfig-registrysecret")
                   .withRunAfter("git-clone")
                   .withNewTaskRef()
                     .withResolver("bundles")
                     .withParams()
                       .addNewParam().withName("bundle").withValue(new ParamValue(getBundleURL("quay.io/ch007m", "tekton-bundle","latest"))).endParam()
                       .addNewParam().withName("name").withValue(new ParamValue("fetch-packconfig-registrysecret")).endParam()
                       .addNewParam().withName("kind").withValue(new ParamValue("task")).endParam()
                   .endTaskRef()
                   .withWorkspaces()
                      .addNewWorkspace().withName("data-store").withWorkspace("data-store").endWorkspace()
                      .addNewWorkspace().withName("pack-workspace").withWorkspace("pack-workspace").endWorkspace()
                .endTask()

                .addNewTask()
                   .withName("list-source-workspace")
                   .withRunAfter("fetch-packconfig-registrysecret")
                   .withNewTaskRef()
                     .withResolver("bundles")
                     .withParams()
                       .addNewParam().withName("bundle").withValue(new ParamValue(getBundleURL("quay.io/ch007m", "tekton-bundle","latest"))).endParam()
                       .addNewParam().withName("name").withValue(new ParamValue("list-source-workspace")).endParam()
                       .addNewParam().withName("kind").withValue(new ParamValue("task")).endParam()
                   .endTaskRef()
                   .withWorkspaces()
                      .addNewWorkspace().withName("source-dir").withWorkspace("source-dir").endWorkspace()
                      .addNewWorkspace().withName("pack-workspace").withWorkspace("pack-workspace").endWorkspace()
                .endTask()

                .addNewTask()
                   .withName("pack-builder")
                   .withRunAfter("fetch-packconfig-registrysecret")
                   .withNewTaskRef()
                     .withResolver("bundles")
                     .withParams()
                       .addNewParam().withName("bundle").withValue(new ParamValue(getBundleURL("quay.io/ch007m", "tekton-bundle","latest"))).endParam()
                       .addNewParam().withName("name").withValue(new ParamValue("pack-builder")).endParam()
                       .addNewParam().withName("kind").withValue(new ParamValue("task")).endParam()
                   .endTaskRef()
                   .withParams()
                      .addNewParam().withName("PACK_SOURCE_DIR").withValue(new ParamValue("$(params.source-dir)")).endParam()
                      .addNewParam().withName("PACK_CLI_IMAGE").withValue(new ParamValue("$(params.imageUrl)")).endParam()
                      .addNewParam().withName("PACK_CLI_IMAGE_VERSION").withValue(new ParamValue("$(params.imageTag)")).endParam()
                      .addNewParam().withName("BUILDER_IMAGE_NAME").withValue(new ParamValue("$(params.output-image)")).endParam()
                      .addNewParam().withName("PACK_BUILDER_TOML").withValue(new ParamValue("ubi-builder.toml")).endParam()
                      .addNewParam().withName("PACK_CMD_FLAGS").withValue(new ParamValue(List.of( "$(params.packCmdBuilderFlags)"))).endParam()
                   .withWorkspaces()
                      .addNewWorkspace().withName("source-dir").withWorkspace("source-dir").endWorkspace()
                      .addNewWorkspace().withName("pack-workspace").withWorkspace("pack-workspace").endWorkspace()
                .endTask()
             .endPipelineSpec()
             .withWorkspaces()
                .addNewWorkspace()
                   .withName("source-dir")
                   .withVolumeClaimTemplate(
                       new PersistentVolumeClaimBuilder()
                           .editOrNewSpec().withResources(
                               new VolumeResourceRequirementsBuilder()
                                   .addToRequests("storage",new Quantity("1Gi"))
                                   .build())
                           .addToAccessModes("ReadWriteOnce").endSpec()
                           .build()
                   )
                .endWorkspace()
                .addNewWorkspace()
                   .withName("pack-workspace")
                   .withVolumeClaimTemplate(
                       new PersistentVolumeClaimBuilder()
                           .editOrNewSpec().withResources(
                               new VolumeResourceRequirementsBuilder()
                                   .addToRequests("storage",new Quantity("1Gi"))
                                   .build())
                           .addToAccessModes("ReadWriteOnce").endSpec()
                           .build()
                   )
                .endWorkspace()
                .addNewWorkspace()
                   .withName("data-store")
                   .withProjected(new ProjectedVolumeSourceBuilder()
                       .addNewSource()
                           .withSecret(new SecretProjectionBuilder().withName("pack-config-toml").build())
                       .endSource()
                       .addNewSource()
                           .withSecret(new SecretProjectionBuilder().withName("quay-creds").build())
                       .endSource().build())
             .endWorkspace()
          .endSpec()
          .build();

      // @formatter:on
        return pr;
    }
}