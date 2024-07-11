package dev.snowdrop.factory.pipeline;

import dev.snowdrop.factory.Flavor;

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
