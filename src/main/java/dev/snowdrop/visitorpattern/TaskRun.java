package dev.snowdrop.visitorpattern;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

@Getter
@Setter
public class TaskRun implements Visitable {
    private String name;
    private List<Step> steps;

    public TaskRun(String name, List<Step> steps) {
        this.name = name;
        this.steps = steps;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    // Other methods related to TaskRun
}

