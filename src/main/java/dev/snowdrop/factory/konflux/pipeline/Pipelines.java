package dev.snowdrop.factory.konflux.pipeline;

import dev.snowdrop.factory.AnnotationsProviderFactory;
import dev.snowdrop.factory.Flavor;
import dev.snowdrop.factory.LabelsProviderFactory;
import dev.snowdrop.model.Configurator;
import io.fabric8.tekton.pipeline.v1.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.snowdrop.factory.konflux.pipeline.Finally.*;
import static dev.snowdrop.factory.konflux.pipeline.Params.*;
import static dev.snowdrop.factory.konflux.pipeline.Results.*;
import static dev.snowdrop.factory.konflux.pipeline.Tasks.*;
import static dev.snowdrop.factory.konflux.pipeline.Workspaces.*;

public class Pipelines {

   private static final Logger logger = LoggerFactory.getLogger(Pipelines.class);

   public static Pipeline createBuilder(Configurator cfg) {
      final Flavor FLAVOR = Flavor.valueOf(cfg.getFlavor().toUpperCase());
      // @formatter:off
      Pipeline pipeline = new PipelineBuilder()
                .withNewMetadata()
                   .withName(cfg.getName())
                   .withLabels(LabelsProviderFactory.getProvider(FLAVOR).getPipelineLabels())
                   .withAnnotations(AnnotationsProviderFactory.getProvider(FLAVOR).getPipelineAnnotations(cfg))
                .endMetadata()
                .withNewSpec()
                   .withWorkspaces(KONFLUX_PIPELINE_WORKSPACES())
                   .withParams(KONFLUX_PIPELINE_PARAMS())
                   .withResults(KONFLUX_PIPELINE_RESULTS())
                   .withFinally(KONFLUX_PIPELINE_FINALLY())

                   .withTasks(
                      INIT(),
                      CLONE_REPOSITORY(),
                      PREFETCH_DEPENDENCIES(),
                      BUILDPACKS_BUILDER(), // TODO: This task should be developed
                      BUILD_SOURCE_IMAGE(),
                      DEPRECATED_BASE_IMAGE_CHECK(),
                      CLAIR_SCAN(),
                      ECOSYSTEM_CERT_PREFLIGHT_CHECKS(),
                      SAST_SNYK_CHECK(),
                      CLAMAV_SCAN(),
                      SBOM_JSON_CHECK()
                   )

                .endSpec()
                .build();
      // @formatter:on
      return pipeline;
   }
}