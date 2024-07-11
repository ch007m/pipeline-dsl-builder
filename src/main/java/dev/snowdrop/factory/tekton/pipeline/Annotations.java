package dev.snowdrop.factory.tekton.pipeline;

import dev.snowdrop.factory.AnnotationsProvider;

import java.util.Map;

public class Annotations implements AnnotationsProvider {
   @Override
   public Map<String, String> getPipelineAnnotations() {
      return Map.of(
      "tekton.dev/pipelines.minVersion:", "0.40.0",
      "tekton.dev/tags:image", "build",
      "tekton.dev/displayName", "Buildpacks",
      "tekton.dev/platforms", "linux/amd64"
      );
   }
}