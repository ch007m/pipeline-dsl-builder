package dev.snowdrop.factory;

import dev.snowdrop.model.Configurator;
import io.fabric8.kubernetes.api.model.HasMetadata;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WorkflowResource {

    public static HasMetadata create(Configurator cfg) {
        String DOMAIN = cfg.getDomain().toUpperCase();
        Type PROVIDER = Type.valueOf(cfg.getProvider().toUpperCase());
        String RESOURCE_TYPE = cfg.getResourceType().toLowerCase();

        if (PROVIDER == null) {
            throw new RuntimeException("Missing type/provider: tekton or konflux");
        }

        if (DOMAIN == null) {
            throw new RuntimeException("Missing domain");
        }

        if (RESOURCE_TYPE == null) {
            throw new RuntimeException("Missing workflow resource type: pipelinerun taskrun, etc");
        }

        return new WorkflowBuilder()
            .withProvider(PROVIDER)
            .withResourceType(RESOURCE_TYPE)
            .build(cfg);
    }
}
