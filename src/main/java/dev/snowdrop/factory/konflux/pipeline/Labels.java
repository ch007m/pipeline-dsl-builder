package dev.snowdrop.factory.konflux.pipeline;

import dev.snowdrop.factory.LabelsProvider;
import dev.snowdrop.model.Configurator;

import java.util.Map;

public class Labels implements LabelsProvider {

   public static Map<String, String> KONFLUX_PIPELINE_LABELS() {
      return Map.of(
         "pipelines.openshift.io/used-by", "build-cloud",
         "pipelines.openshift.io/runtime", "java",
         "pipelines.openshift.io/strategy", "buildpack"
      );
   }

   @Override
   public Map<String, String> getPipelineLabels(Configurator cfg) {
      return Map.of(
         "pipelines.openshift.io/used-by", "build-cloud",
         "pipelines.openshift.io/runtime", "java",
         "pipelines.openshift.io/strategy", cfg.getPipeline().getDomain()
      );
   }
}
