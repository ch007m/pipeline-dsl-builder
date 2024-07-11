package dev.snowdrop.factory;

public class LabelsProviderFactory {
   public static LabelsProvider getProvider(Flavor type) {
      switch (type) {
         case KONFLUX:
            return new dev.snowdrop.factory.konflux.pipeline.Labels();
         case TEKTON:
            return new dev.snowdrop.factory.tekton.pipeline.Labels();
         default:
            throw new IllegalArgumentException("Unknown type: " + type);
      }
   }
}
