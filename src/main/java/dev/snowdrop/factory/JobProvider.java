package dev.snowdrop.factory;

import dev.snowdrop.model.Configurator;

public interface JobProvider {
    <T> T generatePipeline(Configurator cfg);
}
