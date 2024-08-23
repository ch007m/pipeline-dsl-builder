package dev.snowdrop.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Workspace {
    private String name;
    private String workspace;
    private Volume volumeClaimTemplate;
    private List<Volume> volumeSources;
    private Secret secret;
    private ConfigMap configMap;

    public Workspace name(String name) {
        this.name = name;
        return this;
    }

    public Workspace workspace(String workspace) {
        this.workspace = workspace;
        return this;
    }
}
