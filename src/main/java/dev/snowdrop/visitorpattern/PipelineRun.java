package dev.snowdrop.visitorpattern;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PipelineRun implements Visitable {
    private String name;
    private List<Action> actions;

    public PipelineRun(Configurator cfg) {
        this.name = cfg.getName();
        this.actions = cfg.getJob().getActions();
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    // Other methods related to PipelineRun
}
