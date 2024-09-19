package dev.snowdrop.factory;

import dev.snowdrop.model.Configurator;
import io.fabric8.kubernetes.api.model.HasMetadata;

public class JobBuilder {
    private final JobProvider jobProvider;
    private String resourceType;

    public JobBuilder(JobProvider jobProvider) {
        this.jobProvider = jobProvider;
    }

    public JobBuilder withResourceType(String resourceType) {
        this.resourceType = resourceType;
        return this;
    }

    public HasMetadata buildResource(Configurator cfg) {
        return jobProvider.buildResource(cfg, resourceType);
    }
}