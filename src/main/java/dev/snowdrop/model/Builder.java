package dev.snowdrop.model;

public class Builder {
   private Repository repository;
   private String name;

   public Repository getRepository() {
      return repository;
   }

   public void setRepository(Repository repository) {
      this.repository = repository;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }
}
