package dev.snowdrop.factory;

import dev.snowdrop.model.Configurator;
import io.fabric8.kubernetes.api.model.HasMetadata;

public interface ActionProvider {
    HasMetadata buildResource(Configurator cfg);
}
