package dev.snowdrop.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Setter
@Getter
public class Job {
   @JsonProperty(required = true)
   private String name;
   private String description;
   private List<Map<String, String>> annotations;
   private List<Map<String, String>> labels;

   @JsonSetter(nulls = Nulls.SKIP)
   private String resourceType = DEFAULT_RESOURCE_TYPE;

   private List<Map<String, Object>> params;
   private List<Workspace> workspaces;
   private List<Action> actions;
   private List<Map<String, String>> results;

   @JsonSetter(nulls = Nulls.SKIP)
   private String timeout = DEFAULT_TIMEOUT;

   private static String DEFAULT_RESOURCE_TYPE = "PipelineRun";
   private static String DEFAULT_TIMEOUT = "0h5m0s";
}
