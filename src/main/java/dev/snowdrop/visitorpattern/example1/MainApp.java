package dev.snowdrop.visitorpattern.example1;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

@Slf4j
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
        configurator.setName("PipelineRun example");
        configurator.setResourceType("PipelineRun"); // Or "TaskRun"
        configurator.setJob(job);

        // Create resource (PipelineRun or TaskRun)
        Visitable resource = JobFactory.createResource(configurator);

        // Process resource using visitor
        ResourceVisitor visitor = new ResourceVisitor();
        AbstractRun run = resource.accept(visitor);
        if (run instanceof PipelineRun) {
            System.out.println("A PipelineRun : " + ((PipelineRun)run).getName());
        } else {
            log.info("A TaskRun : " + ((TaskRun) run).getName());
        }

        configurator.setName("TaskRun example");
        configurator.setResourceType("TaskRun"); // Or "TaskRun"
        configurator.setJob(job);

        // Create resource (PipelineRun or TaskRun)
        resource = JobFactory.createResource(configurator);

        // Process resource using visitor
        visitor = new ResourceVisitor();
        run = resource.accept(visitor);
        if (run instanceof PipelineRun) {
            System.out.println("A PipelineRun : " + ((PipelineRun)run).getName());
        } else {
            System.out.println("A TaskRun : " + ((TaskRun)run).getName());
        }

    }
}
