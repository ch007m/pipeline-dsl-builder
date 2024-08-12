package dev.snowdrop.factory.tekton.pipeline;

import dev.snowdrop.factory.AnnotationsProvider;
import dev.snowdrop.model.Configurator;

import java.util.Map;

public class Annotations implements AnnotationsProvider {
   @Override
   public Map<String, String> getPipelineAnnotations(Configurator cfg) {
      return Map.of(
      "tekton.dev/pipelines.minVersion", "0.60.x",
      "tekton.dev/displayName", cfg.getJob().getDescription(),
      "tekton.dev/platforms", "linux/amd64"
      );
   }
}