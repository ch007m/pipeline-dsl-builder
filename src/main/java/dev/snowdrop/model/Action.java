package dev.snowdrop.model;

import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Setter
@Getter
public class Action {
    static int instanceCounter = 0;
    private Integer id = 0;
    private boolean isFinally = false;
    private String name;
    private String ref;
    private String script;
    private String scriptFileUrl;
    private String runAfter;
    private List<Map<String, Object>> params;
    private List<Workspace> workspaces;
    private List<String> args;
    private List<String> when;
    private List<Map<String, String>> results;

    public static final String STEP_SCRIPT_IMAGE = "centos";
    public static Map<String, String> IMAGES = new HashMap<>();

    static {
        IMAGES.put(STEP_SCRIPT_IMAGE, "registry.access.redhat.com/ubi9@sha256:1ee4d8c50d14d9c9e9229d9a039d793fcbc9aa803806d194c957a397cf1d2b17");
    }

    public Action() {
        instanceCounter++;
        id = Integer.valueOf(instanceCounter);
    }

    public boolean isFinally() {
        return isFinally;
    }

    // Custom setter with non-standard naming
    @JsonSetter("finally")
    public void isFinally(boolean isFinally) {
        this.isFinally = isFinally;
    }

}
