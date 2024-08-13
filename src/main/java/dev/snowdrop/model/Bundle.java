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
   private String protocol;
   private String uri;

   public Bundle() {}

   public Bundle(String registry, String name, String version, String sha256, String uri, String protocol) {
      this.name = name;
      this.version = version;
      this.sha256 = sha256;
      this.registry = registry;
      this.uri = uri;
      this.protocol = protocol;
   }

   public Bundle(String registry, String name, String version, String sha256) {
      this.name = name;
      this.version = version;
      this.sha256 = sha256;
      this.registry = registry;
   }

   public Bundle protocol(String protocol) {
      this.protocol = protocol;
      return this;
   }

   public Bundle uri(String uri) {
      this.uri = uri;
      return this;
   }
}
