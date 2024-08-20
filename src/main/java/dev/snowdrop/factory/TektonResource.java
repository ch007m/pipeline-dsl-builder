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

        if (actions.isEmpty()) {
            throw new RuntimeException("Missing actions");
        }

        // TODO: We should create a factory here able to process the job according to
        // the provider: tekton vs konflux vs ...
        // return Pipelines.createJob(cfg, actions);
        return JobFactory
            .withProvider(TYPE)
            .generate(cfg);
    }
}
