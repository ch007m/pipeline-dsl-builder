package dev.snowdrop.factory;

import dev.snowdrop.model.Bundle;

import java.util.HashMap;
import java.util.Map;

public class Bundles {

    private static Map<String, Bundle> bundles = new HashMap<>();

    static {
        initBundles();
    }

    // TODO: Find a way to generate such a Map using the content published within this oci
    // conftest pull --policy './temp' oci::quay.io/konflux-ci/tekton-catalog/data-acceptable-bundles:latest
    private static void initBundles() {
        addBundle(new Bundle("quay.io/konflux-ci/tekton-catalog","task-buildah", "0.1","f1aba019735496f9f7a7366b6cef8daa29ac5b36ecfc8a449669d736fb97295a" ));
        addBundle(new Bundle("quay.io/konflux-ci/tekton-catalog","task-init","0.2","092c113b614f6551113f17605ae9cb7e822aa704d07f0e37ed209da23ce392cc"));
        addBundle(new Bundle("quay.io/konflux-ci/tekton-catalog","task-git-clone","0.1","0bb1be8363557e8e07ec34a3c5daaaaa23c9d533f0bb12f00dc604d00de50814"));
        addBundle(new Bundle("quay.io/konflux-ci/tekton-catalog","task-prefetch-dependencies","0.1","d56ee97a3801f13c363d98bd5fe775b19a29a227e39d5214e9092598c6f881a1"));
        addBundle(new Bundle("quay.io/konflux-ci/tekton-catalog","task-source-build","0.1","21cb5ebaff7a9216903cf78933dc4ec4dd6283a52636b16590a5f52ceb278269"));
        addBundle(new Bundle("quay.io/konflux-ci/tekton-catalog","task-build-image-index","0.1","a0ac2ef2107c8d117febbe076c884ef12e0276b0942a47c3906f5f25d128073e"));
        addBundle(new Bundle("quay.io/konflux-ci/tekton-catalog","task-deprecated-image-check","0.4","d98fa9daf5ee12dfbf00880b83d092d01ce9994d79836548d2f82748bb0c64a2"));
        addBundle(new Bundle("quay.io/konflux-ci/tekton-catalog","task-clair-scan","0.1","baea4be429cf8d91f7c758378cea42819fe324f25a7f957bf9805409cab6d123"));
        addBundle(new Bundle("quay.io/konflux-ci/tekton-catalog","task-sast-snyk-check","0.1","82c42d27c9c59db6cf6c235e89f7b37f5cdfc75d0d361ca0ee91ae703ba72301"));
        addBundle(new Bundle("quay.io/konflux-ci/tekton-catalog","task-clamav-scan","0.1","7bb17b937c9342f305468e8a6d0a22493e3ecde58977bd2ffc8b50e2fa234d58"));
        addBundle(new Bundle("quay.io/konflux-ci/tekton-catalog","task-show-sbom","0.1","9bfc6b99ef038800fe131d7b45ff3cd4da3a415dd536f7c657b3527b01c4a13b"));
        addBundle(new Bundle("quay.io/konflux-ci/tekton-catalog","task-sbom-json-check","0.1","2c5de51ec858fc8d47e41c65b20c83fdac249425d67ed6d1058f9f3e0b574500"));
        addBundle(new Bundle("quay.io/konflux-ci/tekton-catalog","task-summary","0.2","d97c04ab42f277b1103eb6f3a053b247849f4f5b3237ea302a8ecada3b24e15b"));
        addBundle(new Bundle("quay.io/konflux-ci/tekton-catalog","task-ecosystem-cert-preflight-checks","0.1","5131cce0f93d0b728c7bcc0d6cee4c61d4c9f67c6d619c627e41e3c9775b497d"));

        // TODO: Create for each task a quay.io repo instead of packaging them in one repository
        addBundle(new Bundle("quay.io/ch007m","tekton-bundle","latest","bc130944a4ee377846abd2ffe9add0c8ad1dff571089d4e0b590e0c446660ac4"));
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
