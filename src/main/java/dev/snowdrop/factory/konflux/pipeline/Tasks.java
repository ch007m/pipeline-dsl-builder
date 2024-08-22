package dev.snowdrop.factory.konflux.pipeline;

import io.fabric8.tekton.pipeline.v1.ParamValue;
import io.fabric8.tekton.pipeline.v1.PipelineTask;
import io.fabric8.tekton.pipeline.v1.PipelineTaskBuilder;

import java.util.List;

import static dev.snowdrop.factory.Bundles.getBundleURL;

public class Tasks {

   public static PipelineTask INIT() {
      PipelineTask task = new PipelineTaskBuilder()
         .withName("init")
         .withParams()
           .addNewParam().withName("image-url").withValue(new ParamValue("$(params.output-image")).endParam()
           .addNewParam().withName("rebuild").withValue(new ParamValue("$(params.rebuild")).endParam()
           .addNewParam().withName("skip-checks").withValue(new ParamValue("$(params.skip-checks")).endParam()
         .withNewTaskRef()
           .withResolver("bundles")
           .withParams()
             .addNewParam().withName("bundle").withValue(new ParamValue(getBundleURL("quay.io/konflux-ci/tekton-catalog","task-init","0.2"))).endParam()
             .addNewParam().withName("name").withValue(new ParamValue("init")).endParam()
             .addNewParam().withName("kind").withValue(new ParamValue("task")).endParam()
         .endTaskRef()
         .build();
      return task;
   }

   public static PipelineTask CLONE_REPOSITORY() {
      PipelineTask task = new PipelineTaskBuilder()
         .withName("clone-repository")
         .withRunAfter("init")
         .addNewWhen()
           .withInput("$(tasks.init.results.build)")
           .withOperator("in")
           .withValues("true")
         .endWhen()
         .withParams()
           .addNewParam().withName("url").withValue(new ParamValue("$(params.git-url)")).endParam()
         .withNewTaskRef()
           .withResolver("bundles")
           .withParams()
             .addNewParam().withName("bundle").withValue(new ParamValue(getBundleURL("quay.io/konflux-ci/tekton-catalog","task-git-clone","0.1"))).endParam()
             .addNewParam().withName("name").withValue(new ParamValue("git-clone")).endParam()
             .addNewParam().withName("kind").withValue(new ParamValue("task")).endParam()
         .endTaskRef()
         .withWorkspaces()
           .addNewWorkspace().withName("output").withWorkspace("workspace").endWorkspace()
           .addNewWorkspace().withName("basic-auth").withWorkspace("git-auth").endWorkspace()
         .build();
      return task;
   }

   public static PipelineTask PREFETCH_DEPENDENCIES() {
      PipelineTask task = new PipelineTaskBuilder()
         .withName("prefetch-dependencies")
         .withRunAfter("clone-repository")
         .addNewWhen()
           .withInput("$(params.prefetch-input)")
           .withOperator("notin")
           .withValues("")
         .endWhen()
         .withParams()
           .addNewParam().withName("input").withValue(new ParamValue("$(params.prefetch-input)")).endParam()
         .withNewTaskRef()
           .withResolver("bundles")
           .withParams()
             .addNewParam().withName("bundle").withValue(new ParamValue(getBundleURL("quay.io/konflux-ci/tekton-catalog","task-prefetch-dependencies","0.1"))).endParam()
             .addNewParam().withName("name").withValue(new ParamValue("prefetch-dependencies")).endParam()
             .addNewParam().withName("kind").withValue(new ParamValue("task")).endParam()
         .endTaskRef()
         .withWorkspaces()
           .addNewWorkspace().withName("source").withWorkspace("workspace").endWorkspace()
           //.addNewWorkspace().withName("netrc").withWorkspace("netrc").endWorkspace()
           .addNewWorkspace().withName("git-basic-auth").withWorkspace("git-auth").endWorkspace()
         .build();
      return task;
   }

   public static PipelineTask BUILD_IMAGE_INDEX() {
      return new PipelineTaskBuilder()
         .withName("build-image-index")
         .withRunAfter("build-container")
         .addNewWhen()
           .withInput("$(tasks.init.results.build)")
           .withOperator("in")
           .withValues("true")
         .endWhen()
         .withParams()
           .addNewParam().withName("IMAGE").withValue(new ParamValue("$(params.output-image)")).endParam()
           .addNewParam().withName("COMMIT_SHA").withValue(new ParamValue("$(tasks.clone-repository.results.commit)")).endParam()
           .addNewParam().withName("IMAGE_EXPIRES_AFTER").withValue(new ParamValue("$(params.image-expires-after)")).endParam()
           .addNewParam().withName("ALWAYS_BUILD_INDEX").withValue(new ParamValue("$(params.build-image-index)")).endParam()
           .addNewParam().withName("IMAGES").withValue(new ParamValue(List.of("$(tasks.build-container.results.IMAGE_URL)@$(tasks.build-container.results.IMAGE_DIGEST)"))).endParam()
         .withNewTaskRef()
           .withResolver("bundles")
           .withParams()
             .addNewParam().withName("bundle").withValue(new ParamValue(getBundleURL("quay.io/konflux-ci/tekton-catalog","task-build-image-index","0.1"))).endParam()
             .addNewParam().withName("name").withValue(new ParamValue("build-image-index")).endParam()
             .addNewParam().withName("kind").withValue(new ParamValue("task")).endParam()
         .endTaskRef()
         .withWorkspaces()
           .addNewWorkspace().withName("workspace").withWorkspace("workspace").endWorkspace()
         .build();
   }

   public static PipelineTask BUILD_SOURCE_IMAGE() {
      PipelineTask task = new PipelineTaskBuilder()
         .withName("build-source-image")
         .withRunAfter("build-container")
         .addNewWhen()
           .withInput("$(tasks.init.results.build)")
           .withOperator("in")
           .withValues("true")
           .withInput("$(params.build-source-image)")
           .withOperator("in")
           .withValues("true")
         .endWhen()
         .withParams()
           .addNewParam().withName("BINARY_IMAGE").withValue(new ParamValue("$(params.output-image)")).endParam()
           .addNewParam().withName("BASE_IMAGES").withValue(new ParamValue("$(tasks.build-container.results.BASE_IMAGES_DIGESTS)")).endParam()
         .withNewTaskRef()
           .withResolver("bundles")
           .withParams()
             .addNewParam().withName("bundle").withValue(new ParamValue(getBundleURL("quay.io/konflux-ci/tekton-catalog","task-source-build","0.1"))).endParam()
             .addNewParam().withName("name").withValue(new ParamValue("source-build")).endParam()
             .addNewParam().withName("kind").withValue(new ParamValue("task")).endParam()
         .endTaskRef()
         .withWorkspaces()
           .addNewWorkspace().withName("workspace").withWorkspace("workspace").endWorkspace()
         .build();
      return task;
   }

   public static PipelineTask DEPRECATED_BASE_IMAGE_CHECK() {
      PipelineTask task = new PipelineTaskBuilder()
         .withName("deprecated-image-check")
         .withRunAfter("build-container")
         .addNewWhen()
           .withInput("$(params.skip-checks)")
           .withOperator("in")
           .withValues("false")
         .endWhen()
         .withParams()
           .addNewParam().withName("IMAGE_URL").withValue(new ParamValue("$(tasks.build-container.results.IMAGE_URL)")).endParam()
           .addNewParam().withName("IMAGE_DIGEST").withValue(new ParamValue("$(tasks.build-container.results.IMAGE_DIGEST)")).endParam()
           .addNewParam().withName("BASE_IMAGES_DIGESTS").withValue(new ParamValue("$(tasks.build-container.results.BASE_IMAGES_DIGESTS)")).endParam()
         .withNewTaskRef()
           .withResolver("bundles")
           .withParams()
             .addNewParam().withName("bundle").withValue(new ParamValue(getBundleURL("quay.io/konflux-ci/tekton-catalog","task-deprecated-image-check","0.4"))).endParam()
             .addNewParam().withName("name").withValue(new ParamValue("deprecated-image-check")).endParam()
             .addNewParam().withName("kind").withValue(new ParamValue("task")).endParam()
         .endTaskRef()
         .build();
      return task;
   }

   public static PipelineTask CLAIR_SCAN() {
      PipelineTask task = new PipelineTaskBuilder()
         .withName("clair-scan")
         .withRunAfter("build-container")
         .addNewWhen()
           .withInput("$(params.skip-checks)")
           .withOperator("in")
           .withValues("false")
         .endWhen()
         .withParams()
           .addNewParam().withName("image-digest").withValue(new ParamValue("$(tasks.build-container.results.IMAGE_DIGEST)")).endParam()
           .addNewParam().withName("image-url").withValue(new ParamValue("$(tasks.build-container.results.IMAGE_URL)")).endParam()
         .withNewTaskRef()
           .withResolver("bundles")
           .withParams()
             .addNewParam().withName("bundle").withValue(new ParamValue(getBundleURL("quay.io/konflux-ci/tekton-catalog","task-clair-scan","0.1"))).endParam()
             .addNewParam().withName("name").withValue(new ParamValue("clair-scan")).endParam()
             .addNewParam().withName("kind").withValue(new ParamValue("task")).endParam()
         .endTaskRef()
         .build();
      return task;
   }

   public static PipelineTask ECOSYSTEM_CERT_PREFLIGHT_CHECKS() {
      PipelineTask task = new PipelineTaskBuilder()
         .withName("ecosystem-cert-preflight-checks")
         .withRunAfter("build-container")
         .addNewWhen()
           .withInput("$(params.skip-checks)")
           .withOperator("in")
           .withValues("false")
         .endWhen()
         .withParams()
           .addNewParam().withName("image-url").withValue(new ParamValue("$(tasks.build-container.results.IMAGE_URL)")).endParam()
         .withNewTaskRef()
           .withResolver("bundles")
           .withParams()
             .addNewParam().withName("bundle").withValue(new ParamValue(getBundleURL("quay.io/konflux-ci/tekton-catalog","task-ecosystem-cert-preflight-checks","0.1"))).endParam()
             .addNewParam().withName("name").withValue(new ParamValue("ecosystem-cert-preflight-checks")).endParam()
             .addNewParam().withName("kind").withValue(new ParamValue("task")).endParam()
         .endTaskRef()
         .build();
      return task;
   }

   public static PipelineTask SAST_SNYK_CHECK() {
      PipelineTask task = new PipelineTaskBuilder()
         .withName("sast-snyk-check")
         .withRunAfter("build-container")
         .addNewWhen()
           .withInput("$(params.skip-checks)")
           .withOperator("in")
           .withValues("true")
         .endWhen()
         .withParams()
           .addNewParam().withName("image-digest").withValue(new ParamValue("$(tasks.build-container.results.IMAGE_DIGEST)")).endParam()
           .addNewParam().withName("image-url").withValue(new ParamValue("$(tasks.build-container.results.IMAGE_URL)")).endParam()
         .withNewTaskRef()
           .withResolver("bundles")
           .withParams()
             .addNewParam().withName("bundle").withValue(new ParamValue(getBundleURL("quay.io/konflux-ci/tekton-catalog","task-sast-snyk-check","0.1"))).endParam()
             .addNewParam().withName("name").withValue(new ParamValue("sast-snyk-check")).endParam()
             .addNewParam().withName("kind").withValue(new ParamValue("task")).endParam()
         .endTaskRef()
         .withWorkspaces()
           .addNewWorkspace().withName("workspace").withWorkspace("workspace").endWorkspace()
         .build();
      return task;
   }

   public static PipelineTask CLAMAV_SCAN() {
      PipelineTask task = new PipelineTaskBuilder()
         .withName("clamav-scan")
         .withRunAfter("build-container")
         .addNewWhen()
           .withInput("$(params.skip-checks)")
           .withOperator("in")
           .withValues("false")
         .endWhen()
         .withParams()
           .addNewParam().withName("image-digest").withValue(new ParamValue("$(tasks.build-container.results.IMAGE_DIGEST)")).endParam()
           .addNewParam().withName("image-url").withValue(new ParamValue("$(tasks.build-container.results.IMAGE_URL)")).endParam()
         .withNewTaskRef()
           .withResolver("bundles")
           .withParams()
             .addNewParam().withName("bundle").withValue(new ParamValue(getBundleURL("quay.io/konflux-ci/tekton-catalog","task-clamav-scan","0.1"))).endParam()
             .addNewParam().withName("name").withValue(new ParamValue("clamav-scan")).endParam()
             .addNewParam().withName("kind").withValue(new ParamValue("task")).endParam()
         .endTaskRef()
         .build();

      return task;
   }

   public static PipelineTask SBOM_JSON_CHECK() {
      PipelineTask task = new PipelineTaskBuilder()
         .withName("sbom-json-check")
         .withRunAfter("build-container")
         .addNewWhen()
           .withInput("$(params.skip-checks)")
           .withOperator("in")
           .withValues("false")
         .endWhen()
         .withParams()
           .addNewParam().withName("IMAGE_URL").withValue(new ParamValue("$(tasks.build-container.results.IMAGE_URL)")).endParam()
           .addNewParam().withName("IMAGE_DIGEST").withValue(new ParamValue("$(tasks.build-container.results.IMAGE_DIGEST)")).endParam()
         .withNewTaskRef()
           .withResolver("bundles")
           .withParams()
             .addNewParam().withName("bundle").withValue(new ParamValue(getBundleURL("quay.io/konflux-ci/tekton-catalog","task-sbom-json-check","0.1"))).endParam()
             .addNewParam().withName("name").withValue(new ParamValue("sbom-json-check")).endParam()
             .addNewParam().withName("kind").withValue(new ParamValue("task")).endParam()
         .endTaskRef()
         .build();
      return task;
   }

   public static PipelineTask BUILD_CONTAINER() {
      // @formatter:off
      PipelineTask task = new PipelineTaskBuilder()
          .withName("build-container")
          .withRunAfter("prefetch-dependencies")
          .addNewWhen()
             .withInput("tasks.init.results.build")
             .withOperator("in")
             .withValues("true")
          .endWhen()
          .withParams()
             .addNewParam().withName("IMAGE").withValue(new ParamValue("$(params.output-image)")).endParam()
             .addNewParam().withName("DOCKERFILE").withValue(new ParamValue("$(params.dockerfile)")).endParam()
             .addNewParam().withName("CONTEXT").withValue(new ParamValue("$(params.context-path)")).endParam()
             .addNewParam().withName("HERMETIC").withValue(new ParamValue("$(params.hermetic)")).endParam()
             .addNewParam().withName("PREFETCH_INPUT").withValue(new ParamValue("$(params.prefetch-input)")).endParam()
             .addNewParam().withName("IMAGE_EXPIRES_AFTER").withValue(new ParamValue("$(params.image-expires-after)")).endParam()
             .addNewParam().withName("COMMIT_SHA").withValue(new ParamValue("$(tasks.clone-repository.results.commit)")).endParam()
             .addNewParam().withName("BUILD_ARGS_FILE").withValue(new ParamValue(List.of("$(params.build-args[*])"))).endParam()
             .addNewParam().withName("BUILD_ARGS_FILE").withValue(new ParamValue("$(params.build-args-file)")).endParam()
          .withNewTaskRef()
             .withResolver("bundles")
             .withParams()
                .addNewParam().withName("bundle").withValue(new ParamValue(getBundleURL("quay.io/konflux-ci/tekton-catalog","task-buildah","0.1"))).endParam()
                .addNewParam().withName("name").withValue(new ParamValue("buildah")).endParam()
                .addNewParam().withName("kind").withValue(new ParamValue("task")).endParam()
          .endTaskRef()
          .withWorkspaces()
             .addNewWorkspace()
                .withName("source")
                .withWorkspace("workspace")
             .endWorkspace()
          .build();
      // @formatter:on
      return task;
   }

   public static PipelineTask USER_BUILD() {
      // TODO Convert the config actions to tasks
      PipelineTask task = new PipelineTaskBuilder()
          .withName("build-container")
          .build();
      return task;
   }

   public static PipelineTask BUILDPACKS_BUILDER() {
      PipelineTask task = new PipelineTaskBuilder()
         .withName("buildpacks-builder")
         .build();
      return task;
   }

}
