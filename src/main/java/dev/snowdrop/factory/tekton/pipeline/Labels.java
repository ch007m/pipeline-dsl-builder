package dev.snowdrop.factory.tekton.pipeline;

import dev.snowdrop.factory.LabelsProvider;
import dev.snowdrop.model.Configurator;

import java.util.Map;

public class Labels implements LabelsProvider {

   @Override
   public Map<String, String> getPipelineLabels(Configurator cfg) {
      return Map.of(
         "app.kubernetes.io/version", "0.2"
      );
   }
}
