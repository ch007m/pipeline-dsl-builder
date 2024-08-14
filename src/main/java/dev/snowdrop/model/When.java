package dev.snowdrop.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class When {
    private String input;
    private String operator;
    private List<String> values;

    public When input(String input) {
        this.input = input;
        return this;
    }

    public When operator(String operator) {
        this.operator = operator;
        return this;
    }

    public When values(List<String> values) {
        this.values = values;
        return this;
    }
}
