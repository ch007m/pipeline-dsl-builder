package dev.snowdrop.factory.konflux.pipeline;

import dev.snowdrop.factory.LabelsProvider;
import dev.snowdrop.model.Configurator;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Labels implements LabelsProvider {

   public static Map<String, String> KONFLUX_PIPELINE_LABELS() {
      Map<String, String> labels = Map.of(
         "pipelines.openshift.io/used-by", "build-cloud",
         "pipelines.openshift.io/runtime", "java",
         "pipelines.openshift.io/strategy", "buildpack"
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

   @Override
   public Map<String, String> getPipelineLabels(Configurator cfg) {
      Map<String, String> labels = Map.of(
         "pipelines.openshift.io/used-by", "build-cloud",
         "pipelines.openshift.io/runtime", "java",
         "pipelines.openshift.io/strategy", cfg.getDomain()
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
