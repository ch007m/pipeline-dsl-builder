package dev.snowdrop.visitorpattern;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Action {
    private String name;
    private String description;
    private List<Step> steps;

    public Action(String name, String description) {
        this.name = name;
        this.description = description;
    }
}