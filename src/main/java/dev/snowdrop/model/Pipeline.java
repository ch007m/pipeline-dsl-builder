package dev.snowdrop.model;

public class Pipeline {
   private String name;

   public String getDescription() {
      return description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   private String description;
   private String domain;
   private Builder builder;

   public Builder getBuilder() {
      return builder;
   }

   public void setBuilder(Builder builder) {
      this.builder = builder;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getDomain() {
      return domain;
   }

   public void setDomain(String domain) {
      this.domain = domain;
   }

}
