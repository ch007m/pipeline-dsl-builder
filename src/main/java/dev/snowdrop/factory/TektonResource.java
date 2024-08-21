package dev.snowdrop.factory;

import dev.snowdrop.model.Action;
import dev.snowdrop.model.Configurator;
import io.fabric8.kubernetes.api.model.HasMetadata;

import java.util.List;

public class TektonResource {
    public static HasMetadata create(Configurator cfg) {
        List<Action> actions = cfg.getJob().getActions();
        String domain = cfg.getDomain().toUpperCase();
        Type TYPE = Type.valueOf(cfg.getType().toUpperCase());

        if (TYPE == null) {
            throw new RuntimeException("Missing type/provider");
        }

        if (domain == null) {
            throw new RuntimeException("Missing domain");
        }

        return JobFactory
            .withType(TYPE)
            .generatePipeline(cfg);
    }
}
