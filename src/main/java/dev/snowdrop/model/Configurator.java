package dev.snowdrop.model;

public class Configurator {
   private String type;
   private String name;
   private String namespace;
   private Job job;
   private Repository repository;

   public Repository getRepository() {
      return repository;
   }

   public void setRepository(Repository repository) {
      this.repository = repository;
   }

   public String getNamespace() {
      return namespace;
   }

   public void setNamespace(String namespace) {
      this.namespace = namespace;
   }

   public String getType() {
      return type;
   }

   public void setType(String type) {
      this.type = type;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public Job getJob() {
      return job;
   }

   public void setJob(Job job) {
      this.job = job;
   }

}
