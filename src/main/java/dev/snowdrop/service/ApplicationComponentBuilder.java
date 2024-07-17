package dev.snowdrop.service;

import dev.snowdrop.konflux.v1alpha1.Application;
import dev.snowdrop.konflux.v1alpha1.ApplicationBuilder;
import dev.snowdrop.konflux.v1alpha1.Component;
import dev.snowdrop.konflux.v1alpha1.ComponentBuilder;
import dev.snowdrop.model.Configurator;

import java.util.Map;
/*
New YAML example: https://github.com/konflux-ci/konflux-ci/blob/main/test/resources/demo-users/user/ns2/application-and-component.yaml

and

hereafter => old YAML used with RHTAP previously
---
apiVersion: appstudio.redhat.com/v1alpha1
kind: Application
metadata:
  name: $GITHUB_REPO_DEMO_NAME
spec:
  appModelRepository:
    url: ""
  displayName: $GITHUB_REPO_DEMO_NAME
  gitOpsRepository:
    url: ""
---
apiVersion: appstudio.redhat.com/v1alpha1
kind: Component
metadata:
  annotations:
    appstudio.openshift.io/pac-provision: request
    image.redhat.com/generate: '{"visibility":"public"}'
  name: $COMPONENT_NAME
spec:
  application: $GITHUB_REPO_DEMO_NAME
  componentName: $COMPONENT_NAME
  replicas: 1
  resources:
    requests:
      cpu: 10m
      memory: 100Mi
  source:
    git:
      context: ./
      devfileUrl: $DEVFILE_URL
      revision: main
      url: https://github.com/halkyonio/$GITHUB_REPO_DEMO_NAME.git
  targetPort: 8080
EOF
* */
public class ApplicationComponentBuilder {
    private final static String COMPONENT_ANNOTATION = "{\"visibility\":\"public\"}";

    public static Application createApplication(Configurator cfg) {
        Application application = new ApplicationBuilder()
            // @formatter:off
            .withNewMetadata()
               .withName(cfg.getName())
               .withAnnotations(Map.of("application.thumbnail","1"))
               .withNamespace(cfg.getNamespace())
            .endMetadata()
            .withNewSpec()
               .withDisplayName(cfg.getName())
            .endSpec()
            // @formatter:on
            .build();
        return application;
    }

    public static Component createComponent(Configurator cfg) {
        Component component = new ComponentBuilder()
            // @formatter:off
            .withNewMetadata()
                .withName(cfg.getName())
                .withNamespace(cfg.getNamespace())
                .withAnnotations(Map.of(
                    "build.appstudio.openshift.io/request","configure-pac",
                    "image.redhat.com/generate",COMPONENT_ANNOTATION
                ))
            .endMetadata()
            .withNewSpec()
               .withApplication(cfg.getName())
               .withComponentName(cfg.getName())
               .withNewSource()
                  .withNewGit()
                     .withUrl("TODO")
                     .withRevision("TODO")
                     .withContext("TODO")
                     .withDockerfileUrl("TODO")
                  .endGit()
               .endSource()
            .endSpec()
            // @formatter:on
            .build();
        return component;
    }
}
