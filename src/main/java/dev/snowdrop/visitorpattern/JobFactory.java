package dev.snowdrop.visitorpattern;

import java.util.List;
import java.util.stream.Collectors;

public class JobFactory {
    public static Visitable createResource(Configurator config) {
        if (config.getResourceType().equalsIgnoreCase("PipelineRun")) {
            return new PipelineRun(config);
        } else if (config.getResourceType().equalsIgnoreCase("TaskRun")) {
            return new TaskRun(config);
        }
        throw new IllegalArgumentException("Unknown resource type: " + config.getResourceType());
    }
}
