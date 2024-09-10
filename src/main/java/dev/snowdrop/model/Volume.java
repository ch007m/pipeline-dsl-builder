package dev.snowdrop.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Volume {
    private String name;
    private String storage;
    private String accessMode;
    private String secret;
    private String mountPath;
    private Boolean readOnly = true;

    public static String STORAGE = "storage";
}
