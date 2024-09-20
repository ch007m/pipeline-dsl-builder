package dev.snowdrop.visitorpattern;

import java.util.stream.Collectors;

public class ResourceVisitor implements Visitor {

    @Override
    public AbstractRun visit(PipelineRun pipelineRun) {
        System.out.println("Processing PipelineRun ...");
        Configurator cfg = pipelineRun.getConfig();
        pipelineRun.setActions(cfg.getJob().getActions());
        pipelineRun.setName(cfg.getName());
        return pipelineRun;
    }

    @Override
    public TaskRun visit(TaskRun taskRun) {
        System.out.println("Processing TaskRun ...");
        Configurator cfg = taskRun.getConfig();
        taskRun.setSteps(cfg.getJob().getActions().stream()
            .map(action -> new Step(action.getName()))
            .collect(Collectors.toList()));
        taskRun.setName(cfg.getName());
        return taskRun;
    }
}
