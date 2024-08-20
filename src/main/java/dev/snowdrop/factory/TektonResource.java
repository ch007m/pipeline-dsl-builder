package dev.snowdrop.factory;

import dev.snowdrop.model.Action;
import dev.snowdrop.model.Configurator;

import java.util.List;

public class TektonResource {
    public static <T> T create(Configurator cfg) {
        Class<T> type;
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
