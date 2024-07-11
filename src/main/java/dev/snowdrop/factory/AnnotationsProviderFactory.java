package dev.snowdrop.factory;

public class AnnotationsProviderFactory {
   public static AnnotationsProvider getProvider(Flavor type) {
      switch (type) {
         case KONFLUX:
            return new dev.snowdrop.factory.konflux.pipeline.Annotations();
         case TEKTON:
            return new dev.snowdrop.factory.tekton.pipeline.Annotations();
         default:
            throw new IllegalArgumentException("Unknown type: " + type);
      }
   }
}
