package dev.snowdrop.visitorpattern;

import java.util.List;
import java.util.stream.Collectors;

public class JobFactory {
    public static Visitable createResource(Configurator config) {
        if (config.getResourceType().equalsIgnoreCase("PipelineRun")) {
            return new PipelineRun(config);
        } else if (config.getResourceType().equalsIgnoreCase("TaskRun")) {
            // Convert Actions to Steps
            List<Step> steps = config.getJob().getActions().stream()
                .map(action -> new Step(action.getName()))
                .collect(Collectors.toList());
            return new TaskRun(config.getName(), steps);
        }
        throw new IllegalArgumentException("Unknown resource type: " + config.getResourceType());
    }
}
