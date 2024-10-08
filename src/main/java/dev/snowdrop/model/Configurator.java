package dev.snowdrop.model;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import dev.snowdrop.model.konflux.Application;
import dev.snowdrop.model.konflux.Component;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

import static dev.snowdrop.factory.Type.TEKTON;
import static dev.snowdrop.model.Domain.DEMO;

@Setter
@Getter
public class Configurator {
   @JsonSetter(nulls = Nulls.SKIP)
   private String provider = DEFAULT_PROVIDER;

   @JsonSetter(nulls = Nulls.SKIP)
   private String resourceType = DEFAULT_RESOURCE_TYPE;

   @JsonSetter(nulls = Nulls.SKIP)
   private String domain = DEFAULT_DOMAIN;

   private String namespace;
   private Job job;
   private Repository repository;
   private String outputPath;
   private Application application;
   private Component component;
   private List<Bundle> bundles;

   private static String DEFAULT_PROVIDER = String.valueOf(TEKTON).toLowerCase();
   private static String DEFAULT_RESOURCE_TYPE = "PipelineRun";
   private static String DEFAULT_DOMAIN = String.valueOf(DEMO).toLowerCase();
}
