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

public class Pipelines {

   private static final Logger logger = LoggerFactory.getLogger(Pipelines.class);

   public static Pipeline createExample(Configurator cfg) {
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