package dev.snowdrop.factory;

import dev.snowdrop.factory.konflux.KonfluxProvider;
import dev.snowdrop.factory.tekton.TektonProvider;
import dev.snowdrop.model.Configurator;
import io.fabric8.kubernetes.api.model.HasMetadata;

public class WorkflowBuilder {
    private Provider provider;
    private String resourceType;

    public WorkflowBuilder WorkflowBuilder() {
        return new WorkflowBuilder();
    }

    private static Provider createProvider(String provider) {
        switch (provider.toUpperCase()) {
            case "KONFLUX":
                return new KonfluxProvider();
            case "TEKTON":
                return new TektonProvider();
            default:
                throw new IllegalArgumentException("Unknown provider type: " + provider);
        }
    }


    public WorkflowBuilder withProvider(Type providerType) {
        this.provider = createProvider(providerType.toString());
        return this;
    }

    public WorkflowBuilder withResourceType(String resourceType) {
        this.resourceType = resourceType;
        return this;
    }

    public HasMetadata build(Configurator cfg) {
        return provider.buildResource(cfg, resourceType);
    }
}
