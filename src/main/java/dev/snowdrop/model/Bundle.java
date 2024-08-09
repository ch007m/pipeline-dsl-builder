package dev.snowdrop.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
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
}
