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

   @JsonSetter(nulls = Nulls.SKIP)
   private String resourceType = DEFAULT_RESOURCE_TYPE;

   private List<Map<String, Object>> params;
   private List<Workspace> workspaces;
   private List<Action> actions;

   private static String DEFAULT_RESOURCE_TYPE = "PipelineRun";
}
