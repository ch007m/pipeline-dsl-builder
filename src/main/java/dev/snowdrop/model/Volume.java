package dev.snowdrop.model;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Volume {
    private String name;

    @JsonSetter(nulls = Nulls.SKIP)
    private String storage = DEFAULT_STORAGE_QUANTITY;

    @JsonSetter(nulls = Nulls.SKIP)
    private String accessMode = DEFAULT_ACCESS_MODE;

    private String secret;
    private String configMap;
    private String emptyDir;
    private String mountPath;
    private Boolean readOnly = true;

    public static String STORAGE = "storage";
    private static String DEFAULT_STORAGE_QUANTITY = "1Gi";
    private static String DEFAULT_ACCESS_MODE = "ReadWriteOnce";
}
