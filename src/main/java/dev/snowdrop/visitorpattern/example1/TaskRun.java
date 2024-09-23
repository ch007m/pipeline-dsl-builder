package dev.snowdrop.visitorpattern.example1;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TaskRun extends AbstractRun {
    private String name;
    private List<Step> steps;
    private Configurator config;

    public TaskRun(Configurator cfg) {
        this.config = cfg;
    }

    @Override
    public AbstractRun accept(Visitor visitor) {
        return visitor.visit(this);
    }

    // Other methods related to TaskRun
}

