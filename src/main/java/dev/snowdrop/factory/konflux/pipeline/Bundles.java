package dev.snowdrop.factory.konflux.pipeline;

import dev.snowdrop.model.Bundle;

import java.util.HashMap;
import java.util.Map;

public class Bundles {

   private static final String BUNDLE_REGISTRY = "quay.io/konflux-ci/tekton-catalog";
   private static Map<String, Bundle> bundles = new HashMap<>();

   public static void addBundle(Bundle bundle) {
      String key = bundle.getName() + ":" + bundle.getVersion();
      bundles.put(key, bundle);
   }

   public static String getBundleURL(String name, String version) {
      String key = name + ":" + version;
      Bundle b = bundles.get(key);
      if (b != null) {
         return String.format("%s/%s:%s@%s", BUNDLE_REGISTRY, b.getName(), b.getVersion(), b.getSha256());
      }
      return "Bundle not found";
   }

}
