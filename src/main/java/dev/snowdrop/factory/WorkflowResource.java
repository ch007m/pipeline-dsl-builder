package dev.snowdrop.factory;

import dev.snowdrop.model.*;
import io.fabric8.kubernetes.api.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowResource {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowResource.class);

    public static HasMetadata create(Configurator cfg) {
        String DOMAIN = cfg.getDomain().toUpperCase();
        Type PROVIDER = Type.valueOf(cfg.getType().toUpperCase());
        String RESOURCE_TYPE = cfg.getJob().getResourceType().toLowerCase();

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
