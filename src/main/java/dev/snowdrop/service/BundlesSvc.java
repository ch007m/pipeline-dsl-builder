package dev.snowdrop.service;

import dev.snowdrop.model.Bundle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BundlesSvc {
    private static BundlesSvc bundlesSvc;
    private static Map<String, Bundle> bundles = new HashMap<>();

    private BundlesSvc() {
        initBundlesFromDefaultCfg();
    }

    public static BundlesSvc getInstance() {
        if (bundlesSvc == null) {
            bundlesSvc = new BundlesSvc();
        }
        return bundlesSvc;
    }

    public void initBundlesFromDefaultCfg() {
        ConfiguratorSvc configuratorSvc = new ConfiguratorSvc();
        List<Bundle> defaultBundles = configuratorSvc.defaultConfigurator.getBundles();
        defaultBundles.forEach(b -> {
            addBundle(new Bundle(b.getRegistry(), b.getName(), b.getVersion(), b.getSha256()));
        });
    }

    public static void addBundle(Bundle b) {
        String key = b.getRegistry() +":" + b.getName() + ":" + b.getVersion();
        bundles.put(key, b);
    }

    public static String getBundleURL(String registry, String name, String version) {
        String key = registry + ":" +name + ":" + version;
        Bundle b = bundles.get(key);
        if (b != null) {
            return String.format("%s/%s:%s@sha256:%s", b.getRegistry(), b.getName(), b.getVersion(), b.getSha256());
        } else {
            throw new RuntimeException("Could not find bundle : " + String.format("%s/%s:%s@sha256:%s", b.getRegistry(), b.getName(), b.getVersion(), b.getSha256()));
        }
    }
}
