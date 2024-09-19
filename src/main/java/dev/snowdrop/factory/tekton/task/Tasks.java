package dev.snowdrop.factory.tekton.task;

import dev.snowdrop.factory.ActionProvider;
import dev.snowdrop.model.Configurator;
import io.fabric8.kubernetes.api.model.HasMetadata;

public class Tasks implements ActionProvider {
    @Override
    public HasMetadata buildResource(Configurator cfg) {
        return null;
    }
}
