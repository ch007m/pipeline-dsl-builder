package dev.snowdrop.visitorpattern.example1;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Job {
    private List<Action> actions;

    public Job() {}
}