package dev.snowdrop.factory.konflux.pipeline;

import io.fabric8.tekton.pipeline.v1.ParamValue;
import io.fabric8.tekton.pipeline.v1.PipelineTask;
import io.fabric8.tekton.pipeline.v1.PipelineTaskBuilder;

import java.util.ArrayList;
import java.util.List;

import static dev.snowdrop.factory.Bundles.getBundleURL;

public class Finally {

   public static List<PipelineTask> KONFLUX_PIPELINE_FINALLY() {
      List<PipelineTask> finallyTasks = new ArrayList<PipelineTask>();
      finallyTasks.add(new PipelineTaskBuilder()
         .withName("show-sbom")
         .withNewTaskRef()
           .withResolver("bundles")
           .withParams()
             .addNewParam().withName("bundle").withValue(new ParamValue(getBundleURL("quay.io/konflux-ci/tekton-catalog","task-show-sbom","0.1"))).endParam()
             .addNewParam().withName("name").withValue(new ParamValue("show-sbom")).endParam()
             .addNewParam().withName("kind").withValue(new ParamValue("task")).endParam()
         .endTaskRef()
         .withParams()
           .addNewParam().withName("IMAGE_URL").withValue(new ParamValue("$(tasks.build-container.results.IMAGE_URL)")).endParam()
         .build());

      finallyTasks.add(new PipelineTaskBuilder()
         .withName("show-summary")
         .withNewTaskRef()
           .withResolver("bundles")
           .withParams()
             .addNewParam().withName("bundle").withValue(new ParamValue(getBundleURL("quay.io/konflux-ci/tekton-catalog","task-summary","0.2"))).endParam()
             .addNewParam().withName("name").withValue(new ParamValue("summary")).endParam()
             .addNewParam().withName("kind").withValue(new ParamValue("task")).endParam()
         .endTaskRef()
         .withParams()
            .addNewParam().withName("pipelinerun-name").withValue(new ParamValue("$(context.pipelineRun.name)")).endParam()
            .addNewParam().withName("git-url").withValue(new ParamValue("$(tasks.clone-repository.results.url)?rev=$(tasks.clone-repository.results.commit)")).endParam()
            .addNewParam().withName("image-url").withValue(new ParamValue("$(params.output-image)")).endParam()
            .addNewParam().withName("build-task-status").withValue(new ParamValue("$(tasks.build-container.status)")).endParam()
         .withWorkspaces()
              // TODO: Fix hard coded values
              .addNewWorkspace().withName("workspace").withWorkspace("workspace").endWorkspace()
         .build());

      return finallyTasks;
   }

}


