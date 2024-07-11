package dev.snowdrop.factory.konflux.pipeline;

import io.fabric8.tekton.pipeline.v1.*;

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
             .addNewParam().withName("bundle").withValue(new ParamValue("quay.io/konflux-ci/tekton-catalog/task-init:0.2@sha256:ceed8b7d5a3583cd21e7eea32498992824272a5436f17ce24c56c75919839e42")).endParam()
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
           .addNewParam().withName("url").withValue(new ParamValue("$(params.git-url")).endParam()
         .withNewTaskRef()
           .withResolver("bundles")
           .withParams()
             .addNewParam().withName("bundle").withValue(new ParamValue("quay.io/konflux-ci/tekton-catalog/task-git-clone:0.1@sha256:f8b2f37c67c77909e8f0288f38d468d76cc0e5130f8f917c85fe2f0c06dbdcdb")).endParam()
             .addNewParam().withName("name").withValue(new ParamValue("init")).endParam()
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
             .addNewParam().withName("bundle").withValue(new ParamValue("quay.io/konflux-ci/tekton-catalog/task-prefetch-dependencies:0.1@sha256:03e8293e6cc7d70a5f899751c6a4c2a25c3e3a6cfa7c437f9beca69638ce6988")).endParam()
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

}
