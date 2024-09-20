package dev.snowdrop.visitorpattern;

public interface Visitor {
    void visit(PipelineRun pipelineRun);
    void visit(TaskRun taskRun);
}

