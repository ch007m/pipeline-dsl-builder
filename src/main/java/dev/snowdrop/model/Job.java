package dev.snowdrop.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Setter
@Getter
public class Job {
   private String name;
   private String description;
   private String resourceType;
   private List<Map<String, Object>> params;
   private List<Workspace> workspaces;
   private List<Action> actions;
}
