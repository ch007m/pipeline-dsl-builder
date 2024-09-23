package dev.snowdrop.visitorpattern.example1;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Configurator {
    private String name;
    private String provider;
    private String resourceType;
    private String description;
    private Job job;
}