package dev.snowdrop.factory.konflux.pipeline;

import dev.snowdrop.factory.AnnotationsProvider;
import dev.snowdrop.model.Configurator;

import java.util.Map;

public class Annotations implements AnnotationsProvider {
   @Override
   public Map<String, String> getPipelineAnnotations(Configurator cfg) {
      return Map.of();
   }
}