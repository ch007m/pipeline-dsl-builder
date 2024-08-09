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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static dev.snowdrop.factory.Bundles.getBundleURL;

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
        List<PipelineTask> tasks = new ArrayList<>();
        List<Param> pipelineParams = new ArrayList<>();
        List<WorkspaceBinding> pipelineWorkspaces = new ArrayList<>();
        PipelineTask aTask;

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

        for (Action action : actions) {
            if (action.getRef() != null) {
                aTask = createTaskUsingRef(action, cfg.getJob().getWorkspaces());
                tasks.add(aTask);
            }

            if (action.getScript() != null || action.getScriptFileUrl() != null) {
                aTask = createTaskWithEmbeddedScript(action, cfg.getJob().getWorkspaces());
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

    private static List<WorkspacePipelineTaskBinding> populateTaskWorkspaces(Action action, List<Workspace> wksJob) {
        List<WorkspacePipelineTaskBinding> wksPipeline = new ArrayList<>();
        List<Workspace> wksAction = action.getWorkspaces();

        if (wksJob != null && !wksJob.isEmpty()) {
            if (wksAction != null && !wksAction.isEmpty()) {
                // Create a map of the Job's workspaces
                Map<String, Workspace> parentWksMap = wksJob.stream()
                    .collect(Collectors.toMap(Workspace::getName, name -> name));

                // Merge the job workspaces's list, overriding with the child list and
                // replacing only if the workspace name is different from the job's workspace name
                List<Workspace> mergedList = new ArrayList<>(wksJob);
                for (Workspace childWks : wksAction) {
                    if (parentWksMap.containsKey(childWks.getWorkspace())) {
                        Workspace parentWorkspace = parentWksMap.get(childWks.getWorkspace());

                        // If the names are different, replace the parent entry with the child entry,
                        // but keep the parent's workspace field
                        if (!parentWorkspace.getName().equals(childWks.getName())) {
                            mergedList.remove(parentWorkspace);
                            mergedList.add(new Workspace()
                                .name(childWks.getName())
                                .workspace(parentWorkspace.getName()));
                        }
                    }
                }

                for (Workspace wks : mergedList) {
                    WorkspacePipelineTaskBindingBuilder wuBuilder = new WorkspacePipelineTaskBindingBuilder();
                    wuBuilder
                        .withName(wks.getName())
                        .withWorkspace(wks.getWorkspace() != null ? wks.getWorkspace() : wks.getName())
                        .build();
                    wksPipeline.add(wuBuilder.build());
                }
            } else {
                for (Workspace wks : wksJob) {
                    WorkspacePipelineTaskBindingBuilder wuBuilder = new WorkspacePipelineTaskBindingBuilder();
                    wuBuilder
                        .withName(wks.getName())
                        .withWorkspace(wks.getName())
                        .build();
                    wksPipeline.add(wuBuilder.build());
                }
            }
        }
        return wksPipeline;
    }

    private static PipelineTask createTaskWithEmbeddedScript(Action action, List<Workspace> workspaces) {
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
            .withWorkspaces(populateTaskWorkspaces(action, workspaces))
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

    private static PipelineTask createTaskUsingRef(Action action, List<Workspace> workspaces) {
        Bundle b = UriParserSvc.extract(action.getRef());
        if (b == null) {
            //logger.error("Bundle reference ws not parsed properly");
            throw new RuntimeException("Bundle reference was not parsed properly");
        } else {
            PipelineTask pipelineTask = new PipelineTaskBuilder()
                // @formatter:off
                .withName(action.getName())
                .withNewTaskRef()
                  .withResolver("bundles")
                  .withParams()
                    .addNewParam().withName("bundle").withValue(new ParamValue(b.getUri())).endParam()
                    // The name of the task to be fetched should be equal to the name of the Action's name !!
                    .addNewParam().withName("name").withValue(new ParamValue(action.getName())).endParam()
                    .addNewParam().withName("kind").withValue(new ParamValue("task")).endParam()
                  .endTaskRef()
                  .withWorkspaces(populateTaskWorkspaces(action, workspaces))
                .build();
            // @formatter:on
            return pipelineTask;
        }
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
                    String newVal;
                    if (val instanceof String) {
                        newVal = String.valueOf(val);
                    } else if (val instanceof Boolean) {
                        newVal = Boolean.toString((Boolean) val);
                    } else {
                        newVal = String.valueOf(val); // Default to String representation
                    }

                    paramList.add(new ParamBuilder().withName(key).withValue(new ParamValue(newVal)).build());
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

/*   public static Pipeline createPackBuilder(Configurator cfg) {
      TYPE = Type.valueOf(cfg.getType().toUpperCase());

      // @formatter:off
      Pipeline pipeline = new PipelineBuilder()
          .withNewMetadata()
             .withName(cfg.getPipeline().getName())
             .withLabels(LabelsProviderFactory.getProvider(TYPE).getPipelineLabels())
             .withAnnotations(AnnotationsProviderFactory.getProvider(TYPE).getPipelineAnnotations(cfg))
          .endMetadata()
          .withNewSpec()
             .withDescription(cfg.getPipeline().getDescription())
             .withWorkspaces()
                .addNewWorkspace().withName("source-dir").endWorkspace()
                .addNewWorkspace().withName("pack-workspace").endWorkspace()
             .withParams()
                .addNewParam().withName("debug").withDescription("A boolean value used to execute or not a task").withType("string").withDefault(new ParamValue("false")).endParam()
                .addNewParam().withName("git-url").withDescription("Source Repository URL").withType("string").endParam()
                .addNewParam().withName("source-dir").withDescription("Source directory where code is cloned").withType("string").withDefault(new ParamValue(".")).endParam()
                .addNewParam().withName("imageUrl").withDescription("Pack client image url").withType("string").withDefault(new ParamValue("buildpacksio/pack")).endParam()
                .addNewParam().withName("imageTag").withDescription("Pack client image tag").withType("string").withDefault(new ParamValue("latest")).endParam()
                .addNewParam().withName("output-image").withDescription("Name of the Builder image to create").withType("string").withDefault(new ParamValue("latest")).endParam()
                .addNewParam().withName("packCmdBuilderFlags").withDescription("Pack builder command flags").withType("array").withDefault(new ParamValue(List.of(""))).endParam()
          .withTasks()
             .addNewTask()
                .withName("git-clone")
                .withNewTaskRef()
                   .withName("git-clone")
                .endTaskRef()
                .withParams().addNewParam().withName("GIT_PROJECT_URL").withValue(new ParamValue("$(params.git-url)")).endParam()
                .withWorkspaces()
                   .addNewWorkspace().withName("source-dir").withWorkspace("source-dir").endWorkspace()
             .endTask()

             .addNewTask()
                .withName("fetch-packconfig-registrysecret")
                .withRunAfter("git-clone")
                .withTaskSpec(
                    new EmbeddedTaskBuilder()
                    .addNewStep()
                       .withImage("quay.io/centos/centos:latest")
                       .withScript(FileUtilSvc.loadFileAsString("copy-packconfig-registrysecret.sh"))
                    .endStep()
                    .build())
                .withWorkspaces()
                   .addNewWorkspace().withName("data-store").withWorkspace("data-store").endWorkspace()
                   .addNewWorkspace().withName("pack-workspace").withWorkspace("pack-workspace").endWorkspace()
             .endTask()

             .addNewTask()
                .withName("list-source-workspace")
                .withRunAfter("fetch-packconfig-registrysecret")
                .withTaskSpec(
                    new EmbeddedTaskBuilder()
                    .addNewStep()
                       .withImage("quay.io/centos/centos:latest")
                       .withScript(FileUtilSvc.loadFileAsString("list-source-workspace.sh"))
                    .endStep()
                    .build())
                .withWorkspaces()
                   .addNewWorkspace().withName("source-dir").withWorkspace("source-dir").endWorkspace()
                   .addNewWorkspace().withName("pack-workspace").withWorkspace("pack-workspace").endWorkspace()
             .endTask()

             .addNewTask()
                .withName("pack-builder")
                .withRunAfter("fetch-packconfig-registrysecret")
                .withNewTaskRef()
                   .withResolver("git")
                   .withParams()
                     .addNewParam().withName("url").withValue(new ParamValue("https://github.com/redhat-buildpacks/catalog.git")).endParam()
                     .addNewParam().withName("revision").withValue(new ParamValue("main")).endParam()
                     .addNewParam().withName("pathInRepo").withValue(new ParamValue("/tekton/task/pack-builder/0.1/pack-builder.yml")).endParam()
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

          .endSpec()
          .build();
      // @formatter:on

      return pipeline;
   }*/
}