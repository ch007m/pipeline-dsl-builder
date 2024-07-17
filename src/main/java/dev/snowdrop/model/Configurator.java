package dev.snowdrop.model;

public class Configurator {
   private String flavor;
   private String name;
   private Pipeline pipeline;

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
