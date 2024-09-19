package dev.snowdrop.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class Step {
    private String name;
    private List<String> args;
    private String command;
    private String image;
    private String script;
    // TODO: Should we have a new class maybe VolumeMount ?
    private List<Volume> volumes;
    private List<Map<String, String>> env;
    private String workingDir;
}
