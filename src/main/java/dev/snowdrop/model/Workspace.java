package dev.snowdrop.model;

import java.util.ArrayList;
import java.util.List;

public class Workspace {
    private String name;
    private Volume volumeClaimTemplate;
    private List<Volume> volumeSources;

    public Volume getVolumeClaimTemplate() {
        return volumeClaimTemplate;
    }

    public void setVolumeClaimTemplate(Volume volumeClaimTemplate) {
        this.volumeClaimTemplate = volumeClaimTemplate;
    }

    public List<Volume> getVolumeSources() {
        return volumeSources;
    }

    public void setVolumeSources(List<Volume> volumeSources) {
        /*
        List<Volume> newVolumeSources = new ArrayList<>();
        Volume newVolume = new Volume();
        for (Volume v : volumeSources) {
            if (v.getSecret() != "") {
                newVolume.setSecret(v.getSecret());
                newVolumeSources.add(newVolume);
            }
        }
        if (newVolumeSources.isEmpty()) {
            this.volumeSources = volumeSources;
        } else {
            this.volumeSources = newVolumeSources;
        }
        */
        this.volumeSources = volumeSources;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
