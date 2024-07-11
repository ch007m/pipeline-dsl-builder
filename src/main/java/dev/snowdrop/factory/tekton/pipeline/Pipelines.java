package dev.snowdrop.factory.tekton.pipeline;

import dev.snowdrop.factory.AnnotationsProviderFactory;
import dev.snowdrop.factory.Flavor;
import dev.snowdrop.factory.LabelsProviderFactory;
import dev.snowdrop.model.Configurator;
import dev.snowdrop.service.FileUtilSvc;
import io.fabric8.tekton.pipeline.v1.EmbeddedTaskBuilder;
import io.fabric8.tekton.pipeline.v1.Pipeline;
import io.fabric8.tekton.pipeline.v1.PipelineBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.snowdrop.factory.konflux.pipeline.Finally.KONFLUX_PIPELINE_FINALLY;
import static dev.snowdrop.factory.konflux.pipeline.Params.KONFLUX_PIPELINE_PARAMS;
import static dev.snowdrop.factory.konflux.pipeline.Results.KONFLUX_PIPELINE_RESULTS;
import static dev.snowdrop.factory.konflux.pipeline.Tasks.*;
import static dev.snowdrop.factory.konflux.pipeline.Workspaces.KONFLUX_PIPELINE_WORKSPACES;

public class Pipelines {

   private static final Logger logger = LoggerFactory.getLogger(Pipelines.class);

   public static Pipeline createDemo(Configurator cfg) {
      final Flavor FLAVOR = Flavor.valueOf(cfg.getFlavor().toUpperCase());
      // @formatter:off
      Pipeline pipeline = new PipelineBuilder()
               .withNewMetadata()
                   .withName(cfg.getPipeline().getName())
                   .withLabels(LabelsProviderFactory.getProvider(FLAVOR).getPipelineLabels())
                   .withAnnotations(AnnotationsProviderFactory.getProvider(FLAVOR).getPipelineAnnotations())
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
}