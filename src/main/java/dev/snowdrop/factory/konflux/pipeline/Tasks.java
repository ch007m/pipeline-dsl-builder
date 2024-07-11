package dev.snowdrop.factory.konflux.pipeline;

import dev.snowdrop.model.Bundle;
import io.fabric8.tekton.pipeline.v1.*;

import static dev.snowdrop.factory.konflux.pipeline.Bundles.addBundle;
import static dev.snowdrop.factory.konflux.pipeline.Bundles.getBundleURL;

public class Tasks {

   public void Tasks() {
      addBundle(new Bundle("task-init","0.2","ceed8b7d5a3583cd21e7eea32498992824272a5436f17ce24c56c75919839e42"));
      addBundle(new Bundle("task-git-clone","0.1","f8b2f37c67c77909e8f0288f38d468d76cc0e5130f8f917c85fe2f0c06dbdcdb"));
      addBundle(new Bundle("task-prefetch-dependencies","0.1","03e8293e6cc7d70a5f899751c6a4c2a25c3e3a6cfa7c437f9beca69638ce6988"));
      addBundle(new Bundle("task-source-build","0.1","d1fe83481466a3b8ca91ba952f842689c9b9a63183b20fad6927cca10372f08a"));
      addBundle(new Bundle("task-deprecated-image-check","0.4","sha256:48f8a4da120a4dec29da6e4faacee81d024324861474e10e0a7fcfcf56677249"));
      addBundle(new Bundle("task-clair-scan","0.1","07f56dc7b7d77d394c6163f2682b3a72f8bd53e0f43854d848ee0173feb2b25d"));
      addBundle(new Bundle("task-sast-snyk-check","0.1","d501cb1ff0f999a478a7fb8811fb501300be3f158aaedee663d230624d74d2b4"));
      addBundle(new Bundle("task-clamav-scan","0.1","45deb2d3cc6a23166831c7471882a0c8cc8a754365e0598e3e2022cbb1866375"));
      addBundle(new Bundle("task-sbom-json-check","0.1","03322cc79854aeba2a4f6ba48b35a97701297f153398a03917d166cfeebd2c08"));
   }

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
             .addNewParam().withName("bundle").withValue(new ParamValue(getBundleURL("task-init","0.2"))).endParam()
             .addNewParam().withName("name").withValue(new ParamValue("init")).endParam()
             .addNewParam().withName("kind").withValue(new ParamValue("task")).endParam()
         .endTaskRef()
         .build();
      return task;
   }

   public static PipelineTask CLONE_REPOSITORY() {
      PipelineTask task = new PipelineTaskBuilder()
         .withName("clone-repository")
         .withRunAfter("git-clone")
         .addNewWhen()
           .withInput("$(tasks.init.results.build)")
           .withOperator("in")
           .withValues("true")
         .endWhen()
         .withParams()
           .addNewParam().withName("url").withValue(new ParamValue("$(params.git-url")).endParam()
         .withNewTaskRef()
           .withResolver("bundles")
           .withParams()
             .addNewParam().withName("bundle").withValue(new ParamValue(getBundleURL("task-git-clone","0.1"))).endParam()
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
             .addNewParam().withName("bundle").withValue(new ParamValue(getBundleURL("task-prefetch-dependencies","0.1"))).endParam()
             .addNewParam().withName("name").withValue(new ParamValue("prefetch-dependencies")).endParam()
             .addNewParam().withName("kind").withValue(new ParamValue("task")).endParam()
         .endTaskRef()
         .withWorkspaces()
           .addNewWorkspace().withName("source").withWorkspace("workspace").endWorkspace()
           .addNewWorkspace().withName("netrc").withWorkspace("netrc").endWorkspace()
           .addNewWorkspace().withName("git-basic-auth").withWorkspace("git-auth").endWorkspace()
         .build();
      return task;
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
             .addNewParam().withName("bundle").withValue(new ParamValue(getBundleURL("task-source-build","0.1"))).endParam()
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
             .addNewParam().withName("bundle").withValue(new ParamValue(getBundleURL("deprecated-image-check","0.4"))).endParam()
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
             .addNewParam().withName("bundle").withValue(new ParamValue(getBundleURL("task-clair-scan","0.1"))).endParam()
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
             .addNewParam().withName("bundle").withValue(new ParamValue(getBundleURL("task-ecosystem-cert-preflight-checks","0.1"))).endParam()
             .addNewParam().withName("name").withValue(new ParamValue("clair-scan")).endParam()
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
             .addNewParam().withName("bundle").withValue(new ParamValue(getBundleURL("task-sast-snyk-check","0.1"))).endParam()
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
             .addNewParam().withName("bundle").withValue(new ParamValue(getBundleURL("task-clamav-scan","0.1"))).endParam()
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
             .addNewParam().withName("bundle").withValue(new ParamValue(getBundleURL("task-sbom-json-check","0.1"))).endParam()
             .addNewParam().withName("name").withValue(new ParamValue("sbom-json-check")).endParam()
             .addNewParam().withName("kind").withValue(new ParamValue("task")).endParam()
         .endTaskRef()
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
