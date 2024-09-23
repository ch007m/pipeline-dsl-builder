package dev.snowdrop.visitorpattern.example1;

public interface Visitor {
    AbstractRun visit(PipelineRun pipelineRun);
    AbstractRun visit(TaskRun taskRun);
}

