package dev.snowdrop.factory.konflux.pipeline;

import dev.snowdrop.factory.AnnotationsProvider;

import java.util.Map;

public class Annotations implements AnnotationsProvider {
   @Override
   public Map<String, String> getPipelineAnnotations() {
      return Map.of();
   }
}