package dev.snowdrop.visitorpattern;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PipelineRun extends AbstractRun {
    public String name;
    public List<Action> actions;
    private Configurator config;

    public PipelineRun(Configurator cfg) {
        this.config = cfg;
    }

    @Override
    public AbstractRun accept(Visitor visitor) {
        return visitor.visit(this);
    }

    // Other methods related to PipelineRun
}
