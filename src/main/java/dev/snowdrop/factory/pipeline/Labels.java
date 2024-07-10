package dev.snowdrop.factory.pipeline;

import java.util.Map;

public class Labels {

   public static Map<String, String> KONFLUX_PIPELINE_LABELS() {
      return Map.of(
         "pipelines.openshift.io/used-by", "build-cloud",
         "pipelines.openshift.io/runtime", "java",
         "pipelines.openshift.io/strategy", "buildpack"
      );
   }
}
