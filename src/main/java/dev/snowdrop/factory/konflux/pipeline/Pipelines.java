package dev.snowdrop.factory.konflux.pipeline;

import dev.snowdrop.factory.AnnotationsProviderFactory;
import dev.snowdrop.factory.JobProvider;
import dev.snowdrop.factory.LabelsProviderFactory;
import dev.snowdrop.factory.Type;
import dev.snowdrop.model.Configurator;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.tekton.pipeline.v1.PipelineRun;
import io.fabric8.tekton.pipeline.v1.PipelineRunBuilder;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import static dev.snowdrop.factory.konflux.pipeline.Finally.KONFLUX_PIPELINE_FINALLY;
import static dev.snowdrop.factory.konflux.pipeline.Params.KONFLUX_PIPELINERUN_PARAMS;
import static dev.snowdrop.factory.konflux.pipeline.Results.KONFLUX_PIPELINE_RESULTS;
import static dev.snowdrop.factory.konflux.pipeline.Tasks.*;
import static dev.snowdrop.factory.konflux.pipeline.Workspaces.KONFLUX_PIPELINERUN_WORKSPACES;

public class Pipelines implements JobProvider {

   //private static final Logger logger = LoggerFactory.getLogger(Pipelines.class);
   private static Type TYPE = null;

   @Override
   public HasMetadata generatePipeline(Configurator cfg) {
      TYPE = Type.valueOf(cfg.getType().toUpperCase());

      if (cfg.getRepository() == null) {
         throw new RuntimeException("Git repository is missing");
      } else {
         // Set default values for the Repository when not defined part of the configuration yaml
         // TODO: To be improved as it should be defaulted when yaml is parsed to objects
         cfg.getRepository().setDefaultValues();
      }

      // @formatter:off
      PipelineRun pipeline = new PipelineRunBuilder()
                .withNewMetadata()
                   .withName(cfg.getJob().getName())
                   .withLabels(LabelsProviderFactory.getProvider(TYPE).getPipelineLabels(cfg))
                   .withAnnotations(AnnotationsProviderFactory.getProvider(TYPE).getPipelineAnnotations(cfg))
                .endMetadata()
                .withNewSpec()
                   .withWorkspaces(KONFLUX_PIPELINERUN_WORKSPACES())
                   .withParams(KONFLUX_PIPELINERUN_PARAMS())
                   .withNewPipelineSpec()
                      .withResults(KONFLUX_PIPELINE_RESULTS())
                      .withFinally(KONFLUX_PIPELINE_FINALLY())

                      .withTasks(
                         INIT(),
                         CLONE_REPOSITORY(),
                         PREFETCH_DEPENDENCIES(),
                         USER_BUILD(), // TODO: This task should be developed
                         BUILD_SOURCE_IMAGE(),
                         DEPRECATED_BASE_IMAGE_CHECK(),
                         CLAIR_SCAN(),
                         ECOSYSTEM_CERT_PREFLIGHT_CHECKS(),
                         SAST_SNYK_CHECK(),
                         CLAMAV_SCAN(),
                         SBOM_JSON_CHECK()
                      )
                   .endPipelineSpec()
                .endSpec()
                .build();
      // @formatter:on

      // TODO: Add like with Tekton a switch to handle: Pipeline vs PipelineRun
      return pipeline;
   }

   // TODO: To be parked or ...
      public static PipelineRun createBuild(Configurator cfg) {
      TYPE = Type.valueOf(cfg.getType().toUpperCase());
      // @formatter:off
      return new PipelineRunBuilder()
          .withNewMetadata()
             .withName(cfg.getJob().getName())
             .withLabels(LabelsProviderFactory.getProvider(TYPE).getPipelineLabels(cfg))
             .withAnnotations(AnnotationsProviderFactory.getProvider(TYPE).getPipelineAnnotations(cfg))
          .endMetadata()
          .withNewSpec()
             .withWorkspaces(KONFLUX_PIPELINERUN_WORKSPACES())
             .withParams(KONFLUX_PIPELINERUN_PARAMS())
             .withNewPipelineSpec()
                .withResults(KONFLUX_PIPELINE_RESULTS())
                .withFinally(KONFLUX_PIPELINE_FINALLY())
                .withTasks(
                    INIT(),
                    CLONE_REPOSITORY(),
                    PREFETCH_DEPENDENCIES(),
                    BUILD_CONTAINER(),
                    BUILD_SOURCE_IMAGE(),
                    DEPRECATED_BASE_IMAGE_CHECK(),
                    CLAIR_SCAN(),
                    ECOSYSTEM_CERT_PREFLIGHT_CHECKS(),
                    SAST_SNYK_CHECK(),
                    CLAMAV_SCAN(),
                    SBOM_JSON_CHECK()
                )
             .endPipelineSpec()
          .endSpec()
          .build();
      // @formatter:on
   }
}