package dev.snowdrop.model;

public class Configurator {
   private String flavor;
   private Builder builder;

   public String getFlavor() {
      return flavor;
   }

   public void setFlavor(String flavor) {
      this.flavor = flavor;
   }

   public Builder getBuilder() {
      return builder;
   }

   public void setBuilder(Builder builder) {
      this.builder = builder;
   }

}
