package dev.snowdrop.service;

import dev.snowdrop.konflux.v1alpha1.Application;
import dev.snowdrop.konflux.v1alpha1.ApplicationBuilder;
import dev.snowdrop.model.Configurator;

import java.util.Map;

public class ApplicationComponentBuilder {
    public static Application createApplication(Configurator cfg) {
        Application application = new ApplicationBuilder()
            // @formatter:off
            .withNewMetadata()
               .withName(cfg.getName())
               .withAnnotations(Map.of("application.thumbnail","1"))
            .endMetadata()
            .withNewSpec()
               .withDisplayName(cfg.getName())
            .endSpec()
            // @formatter:on
            .build();
        return application;
    }
}
