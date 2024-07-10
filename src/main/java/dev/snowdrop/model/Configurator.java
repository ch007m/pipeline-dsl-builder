package dev.snowdrop.model;

public class Configurator {
   private Builder builder;

   public Builder getBuilder() {
      return builder;
   }

   public void setBuilder(Builder builder) {
      this.builder = builder;
   }

   public static class Builder {
      private Repository repository;

      public Repository getRepository() {
         return repository;
      }

      public void setRepository(Repository repository) {
         this.repository = repository;
      }
   }

   public static class Repository {
      private String name;
      private String branch;

      public String getName() {
         return name;
      }

      public void setName(String name) {
         this.name = name;
      }

      public String getBranch() {
         return branch;
      }

      public void setBranch(String branch) {
         this.branch = branch;
      }
   }
}
