package dev.snowdrop.factory.tekton.pipeline;

import dev.snowdrop.factory.AnnotationsProvider;
import dev.snowdrop.model.Configurator;

import java.util.Map;

public class Annotations implements AnnotationsProvider {
   @Override
   public Map<String, String> getPipelineAnnotations(Configurator cfg) {
      return Map.of(
      "tekton.dev/pipelines.minVersion", "0.40.0",
      "tekton.dev/displayName", cfg.getPipeline().getDescription(),
      "tekton.dev/platforms", "linux/amd64"
      );
   }
}