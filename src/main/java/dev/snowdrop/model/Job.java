package dev.snowdrop.model;

import java.util.List;
import java.util.Map;

/**
 * The type Job.
 */
public class Job {
   private String name;
   private String description;
   private String resourceType;
   private List<Map<String, Object>> params;
   private List<Workspace> workspaces;
   private List<Action> actions;

   public List<Workspace> getWorkspaces() {
      return workspaces;
   }

   public void setWorkspaces(List<Workspace> workspaces) {
      this.workspaces = workspaces;
   }

   public List<Map<String, Object>> getParams() {
      return params;
   }

   public void setParams(List<Map<String, Object>> params) {
      this.params = params;
   }

   public List<Action> getActions() {
      return actions;
   }

   public void setActions(List<Action> actions) {
      this.actions = actions;
   }

   /**
    * Gets resource type.
    *
    * @return the resource type
    */
   public String getResourceType() {
      return resourceType;
   }

   /**
    * Sets resource type.
    *
    * @param resourceType the resource type
    */
   public void setResourceType(String resourceType) {
      this.resourceType = resourceType;
   }

   /**
    * Gets description.
    *
    * @return the description
    */
   public String getDescription() {
      return description;
   }

   /**
    * Sets description.
    *
    * @param description the description
    */
   public void setDescription(String description) {
      this.description = description;
   }

   /**
    * Gets name.
    *
    * @return the name
    */
   public String getName() {
      return name;
   }

   /**
    * Sets name.
    *
    * @param name the name
    */
   public void setName(String name) {
      this.name = name;
   }

}
