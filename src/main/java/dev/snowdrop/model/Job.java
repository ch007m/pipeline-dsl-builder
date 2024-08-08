package dev.snowdrop.model;

import java.util.List;

/**
 * The type Job.
 */
public class Job {
   private String resourceType;
   private String name;
   private String description;
   private List<Action> actions;
   private Builder builder;
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
    * Gets builder.
    *
    * @return the builder
    */
   public Builder getBuilder() {
      return builder;
   }

   /**
    * Sets builder.
    *
    * @param builder the builder
    */
   public void setBuilder(Builder builder) {
      this.builder = builder;
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
