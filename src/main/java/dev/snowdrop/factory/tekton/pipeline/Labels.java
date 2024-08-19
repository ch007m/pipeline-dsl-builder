package dev.snowdrop.factory.tekton.pipeline;

import dev.snowdrop.factory.LabelsProvider;
import dev.snowdrop.model.Configurator;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Labels implements LabelsProvider {

   @Override
   public Map<String, String> getPipelineLabels(Configurator cfg) {
      Map<String, String> labels = Map.of(
         "app.kubernetes.io/version", "0.1"
      );

      return labels.entrySet()
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
