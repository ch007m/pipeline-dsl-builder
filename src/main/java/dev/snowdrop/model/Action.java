package dev.snowdrop.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Setter
@Getter
public class Action {
    static int instanceCounter = 0;
    private Integer id = 1;
    private String name;
    private String ref;
    private String script;
    private String scriptFileUrl;
    private List<Map<String, Object>> params;
    private List<Workspace> workspaces;

    public static final String STEP_SCRIPT_IMAGE = "ubuntu";

    public Action()
    {
        instanceCounter++;
        id = instanceCounter;
    }

}
