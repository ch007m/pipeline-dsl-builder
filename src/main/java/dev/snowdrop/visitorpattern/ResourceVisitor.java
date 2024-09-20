package dev.snowdrop.visitorpattern;

public class ResourceVisitor implements Visitor {

    @Override
    public void visit(PipelineRun pipelineRun) {
        System.out.println("Processing PipelineRun: " + pipelineRun.getName());
        // Logic to process PipelineRun
    }

    @Override
    public void visit(TaskRun taskRun) {
        System.out.println("Processing TaskRun: " + taskRun.getName());
        // Logic to process TaskRun
    }
}
