package dev.snowdrop.visitorpattern;

import java.util.Arrays;
import java.util.List;

public class MainApp {
    public static void main(String[] args) {
        // Create a sample Configurator
        Job job = new Job();

        List<Action> actions = Arrays.asList(
            new Action("Action1", "Description1"),
            new Action("Action2", "Description2")
        );
        job.setActions(actions);

        Configurator configurator = new Configurator();
        configurator.setName("Example Config");
        configurator.setResourceType("PipelineRun"); // Or "TaskRun"
        configurator.setJob(job);

        // Create resource (PipelineRun or TaskRun)
        Visitable resource = JobFactory.createResource(configurator);

        // Process resource using visitor
        ResourceVisitor visitor = new ResourceVisitor();
        resource.accept(visitor);
    }
}
