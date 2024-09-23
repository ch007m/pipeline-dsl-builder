package dev.snowdrop.visitorpattern.example1;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Step {
        private String name;

    public Step(String name) {
        this.name = name;
    }
}