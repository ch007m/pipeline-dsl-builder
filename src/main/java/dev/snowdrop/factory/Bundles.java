package dev.snowdrop.factory;

import dev.snowdrop.model.Bundle;

import java.util.HashMap;
import java.util.Map;

public class Bundles {

    private static final String BUNDLE_REGISTRY = "quay.io/konflux-ci/tekton-catalog";
    private static Map<String, Bundle> bundles = new HashMap<>();

    static {
        initBundles();
    }

    // TODO: Find a way to generate such a Map using the content published within this oci
    // conftest pull --policy './temp' oci::quay.io/konflux-ci/tekton-catalog/data-acceptable-bundles:latest
    private static void initBundles() {
        addBundle(new Bundle("task-init","0.2","ceed8b7d5a3583cd21e7eea32498992824272a5436f17ce24c56c75919839e42"));
        addBundle(new Bundle("task-git-clone","0.1","de0ca8872c791944c479231e21d68379b54877aaf42e5f766ef4a8728970f8b3"));
        addBundle(new Bundle("task-prefetch-dependencies","0.1","03e8293e6cc7d70a5f899751c6a4c2a25c3e3a6cfa7c437f9beca69638ce6988"));
        addBundle(new Bundle("task-source-build","0.1","d1fe83481466a3b8ca91ba952f842689c9b9a63183b20fad6927cca10372f08a"));
        addBundle(new Bundle("task-deprecated-image-check","0.4","48f8a4da120a4dec29da6e4faacee81d024324861474e10e0a7fcfcf56677249"));
        addBundle(new Bundle("task-clair-scan","0.1","07f56dc7b7d77d394c6163f2682b3a72f8bd53e0f43854d848ee0173feb2b25d"));
        addBundle(new Bundle("task-sast-snyk-check","0.1","d501cb1ff0f999a478a7fb8811fb501300be3f158aaedee663d230624d74d2b4"));
        addBundle(new Bundle("task-clamav-scan","0.1","45deb2d3cc6a23166831c7471882a0c8cc8a754365e0598e3e2022cbb1866375"));
        addBundle(new Bundle("task-sbom-json-check","0.1","03322cc79854aeba2a4f6ba48b35a97701297f153398a03917d166cfeebd2c08"));
        addBundle(new Bundle("task-ecosystem-cert-preflight-checks","0.1","8838d3e1628dbe61f4851b3640d2e3a9a3079d3ff3da955f4a3e4c2c95a013df"));
    }

    public static void addBundle(Bundle bundle) {
        String key = bundle.getName() + ":" + bundle.getVersion();
        bundles.put(key, bundle);
    }

    public static String getBundleURL(String name, String version) {
        String key = name + ":" + version;
        Bundle b = bundles.get(key);
        if (b != null) {
            return String.format("%s/%s:%s@sha256:%s", BUNDLE_REGISTRY, b.getName(), b.getVersion(), b.getSha256());
        }
        return "Bundle not found";
    }
}
