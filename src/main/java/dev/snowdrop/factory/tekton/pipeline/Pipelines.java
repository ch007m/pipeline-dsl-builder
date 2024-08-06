package dev.snowdrop.factory.tekton.pipeline;

import dev.snowdrop.factory.AnnotationsProviderFactory;
import dev.snowdrop.factory.LabelsProviderFactory;
import dev.snowdrop.factory.Type;
import dev.snowdrop.model.Configurator;
import dev.snowdrop.service.FileUtilSvc;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.tekton.pipeline.v1.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static dev.snowdrop.factory.Bundles.getBundleURL;

public class Pipelines {

   private static final Logger logger = LoggerFactory.getLogger(Pipelines.class);
   private static Type TYPE;

   public static Pipeline createExample(Configurator cfg) {
      TYPE = Type.valueOf(cfg.getType().toUpperCase());
      // @formatter:off
      Pipeline pipeline = new PipelineBuilder()
          .withNewMetadata()
                   .withName(cfg.getJob().getName())
                   .withLabels(LabelsProviderFactory.getProvider(TYPE).getPipelineLabels(cfg))
                   .withAnnotations(AnnotationsProviderFactory.getProvider(TYPE).getPipelineAnnotations(cfg))
          .endMetadata()
          .withNewSpec()
             .withTasks()
                // Embedded Task with script
                .addNewTask()
                   .withName("task-embedded-script")
                   .withTaskSpec(
                      new EmbeddedTaskBuilder()
                       .addNewStep()
                          .withName("run-script")
                          .withImage("ubuntu")
                          .withScript(FileUtilSvc.loadFileAsString("echo.sh"))
                       .endStep()
                       .build()
                   )
                .endTask()
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
             .withName(cfg.getJob().getName() + "-run")
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