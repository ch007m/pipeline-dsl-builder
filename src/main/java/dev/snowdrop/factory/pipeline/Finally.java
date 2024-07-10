package dev.snowdrop.factory.pipeline;

import io.fabric8.tekton.pipeline.v1.ParamValue;
import io.fabric8.tekton.pipeline.v1.PipelineTask;
import io.fabric8.tekton.pipeline.v1.PipelineTaskBuilder;

import java.util.ArrayList;
import java.util.List;

public class Finally {

   public static List<PipelineTask> KONFLUX_PIPELINE_FINALLY() {
      List<PipelineTask> finallyTasks = new ArrayList<PipelineTask>();
      finallyTasks.add(new PipelineTaskBuilder()
         .withName("show-sbom")
         .withNewTaskRef()
           // There is a problem here as konflux pipeline uses as fields: name + version
           .withName("show-sbom")
           .withApiVersion("0.1")
           .withKind("")
         .endTaskRef()
         .withParams()
           .addNewParam().withName("IMAGE_URL").withValue(new ParamValue("$(tasks.build-container.results.IMAGE_URL)")).endParam()
         .build());

      finallyTasks.add(new PipelineTaskBuilder()
         .withName("show-summary")
         .withNewTaskRef()
            .withName("summary")
            .withApiVersion("0.2")
            .withKind("")
         .endTaskRef()
         .withParams()
            .addNewParam().withName("pipelinerun-name").withValue(new ParamValue("$(context.pipelineRun.name)")).endParam()
            .addNewParam().withName("git-url").withValue(new ParamValue("$(tasks.clone-repository.results.url)?rev=$(tasks.clone-repository.results.commit)")).endParam()
            .addNewParam().withName("image-url").withValue(new ParamValue("$(params.output-image)")).endParam()
            .addNewParam().withName("build-task-status").withValue(new ParamValue("$(tasks.build-container.status)")).endParam()
         .build());

      return finallyTasks;
   }

}


