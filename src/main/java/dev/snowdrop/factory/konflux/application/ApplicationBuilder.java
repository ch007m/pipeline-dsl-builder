package dev.snowdrop.factory.konflux.application;

import dev.snowdrop.factory.konflux.component.Annotations;
import dev.snowdrop.konflux.v1alpha1.Application;
import dev.snowdrop.model.Configurator;

import java.util.Map;
/*
New YAML example: https://github.com/konflux-ci/konflux-ci/blob/main/test/resources/demo-users/user/ns2/application-and-component.yaml

and

hereafter => YAML generated by konflux-ci
---
kind: Application
metadata:
  annotations:
    application.thumbnail: "10"
  name: my-quarkus
  namespace: user-ns1
spec:
  displayName: my-quarkus
* */
public class ApplicationBuilder {

    public static Application createApplication(Configurator cfg) {
        Application application = new dev.snowdrop.konflux.v1alpha1.ApplicationBuilder()
            // @formatter:off
            .withNewMetadata()
               .withName(cfg.getJob().getName())
               .withAnnotations(Annotations.get(cfg))
               .withNamespace(cfg.getNamespace())
            .endMetadata()
            .withNewSpec()
               .withDisplayName(cfg.getJob().getName())
            .endSpec()
            // @formatter:on
            .build();
        return application;
    }

}
