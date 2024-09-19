package dev.snowdrop.factory;

import dev.snowdrop.model.Configurator;
import io.fabric8.kubernetes.api.model.HasMetadata;

public class Builder {
    private final Provider provider;
    private String resourceType;

    public Builder(Provider provider) {
        this.provider = provider;
    }

    public Builder withResourceType(String resourceType) {
        this.resourceType = resourceType;
        return this;
    }

    public HasMetadata build(Configurator cfg) {
        return provider.buildResource(cfg, resourceType);
    }
}