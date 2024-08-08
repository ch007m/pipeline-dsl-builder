package dev.snowdrop.model;

public class Bundle {
   private String name;
   private String version;
   private String sha256;
   private String registry;
   private String uri;

   public Bundle(String registry, String name, String version, String sha256) {
      this.name = name;
      this.version = version;
      this.sha256 = sha256;
      this.registry = registry;
   }

   public Bundle(String uri, String name) {
      this.uri = uri;
      this.name = name;
   }

   public Bundle(String uri) {
      this.uri = uri;
   }

   public String getUri() {
      return uri;
   }

   public void setUri(String uri) {
      this.uri = uri;
   }

   public String getRegistry() {
      return registry;
   }

   public void setRegistry(String registry) {
      this.registry = registry;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getVersion() {
      return version;
   }

   public void setVersion(String version) {
      this.version = version;
   }

   public String getSha256() {
      return sha256;
   }

   public void setSha256(String sha256) {
      this.sha256 = sha256;
   }
}
