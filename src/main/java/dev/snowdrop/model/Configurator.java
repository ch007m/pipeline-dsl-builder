package dev.snowdrop.model;

import dev.snowdrop.model.konflux.Application;
import dev.snowdrop.model.konflux.Component;
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
   private Application application;
   private Component component;
}
