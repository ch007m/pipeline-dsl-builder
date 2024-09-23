package dev.snowdrop.visitorpattern.example1;

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
