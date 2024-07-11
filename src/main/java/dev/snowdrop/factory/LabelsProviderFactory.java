package dev.snowdrop.factory;

import dev.snowdrop.factory.pipeline.konflux.Labels;

public class LabelsProviderFactory {
   public static LabelsProvider getProvider(Flavor type) {
      switch (type) {
         case KONFLUX:
            return new Labels();
         case STANDARD:
            return new Labels();
         default:
            throw new IllegalArgumentException("Unknown type: " + type);
      }
   }
}
