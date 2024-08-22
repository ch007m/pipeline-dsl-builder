package dev.snowdrop.factory;

import dev.snowdrop.model.Action;
import dev.snowdrop.model.Configurator;
import io.fabric8.kubernetes.api.model.Duration;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.tekton.pipeline.v1.TimeoutFields;
import io.fabric8.tekton.pipeline.v1.TimeoutFieldsBuilder;

import java.text.ParseException;
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

    public static TimeoutFields populateTimeOut(String timeOut) {
        Duration duration = null;
        try {
            duration = Duration.parse(timeOut);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return new TimeoutFieldsBuilder()
            .withPipeline(duration)
            .build();
    }
}
