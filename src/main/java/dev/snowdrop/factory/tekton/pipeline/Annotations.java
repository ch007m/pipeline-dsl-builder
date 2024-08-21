package dev.snowdrop.factory.tekton.pipeline;

import dev.snowdrop.factory.AnnotationsProvider;
import dev.snowdrop.model.Configurator;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class Annotations implements AnnotationsProvider {
   @Override
   public Map<String, String> getPipelineAnnotations(Configurator cfg) {
      Map<String, String> annotations = Map.of(
      "tekton.dev/pipelines.minVersion", "0.60.x",
      "tekton.dev/displayName", Objects.toString(cfg.getJob().getDescription(),""),
      "tekton.dev/platforms", "linux/amd64"
      );

      return annotations.entrySet()
          .stream()
          .sorted(Map.Entry.comparingByValue()) // Sort by value
          .collect(Collectors.toMap(
              Map.Entry::getKey,
              Map.Entry::getValue,
              (oldValue, newValue) -> oldValue,
              LinkedHashMap::new  // Use LinkedHashMap to maintain insertion order
          ));
   }
}