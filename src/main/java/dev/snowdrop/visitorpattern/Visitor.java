package dev.snowdrop.visitorpattern;

public interface Visitor {
    AbstractRun visit(PipelineRun pipelineRun);
    AbstractRun visit(TaskRun taskRun);
}

