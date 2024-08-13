package dev.snowdrop.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Configurator {
   private String type;
   private String domain;
   private String namespace;
   private Job job;
   private Repository repository;
   private String outputPath;
}
