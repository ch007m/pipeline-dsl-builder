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

    public static String STORAGE = "storage";
}
