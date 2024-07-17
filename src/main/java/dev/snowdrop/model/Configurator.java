package dev.snowdrop.model;

public class Configurator {
   private String flavor;
   private String name;
   private String namespace;
   private Pipeline pipeline;

   public String getNamespace() {
      return namespace;
   }

   public void setNamespace(String namespace) {
      this.namespace = namespace;
   }

   public String getFlavor() {
      return flavor;
   }

   public void setFlavor(String flavor) {
      this.flavor = flavor;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public Pipeline getPipeline() {
      return pipeline;
   }

   public void setPipeline(Pipeline pipeline) {
      this.pipeline = pipeline;
   }



}
