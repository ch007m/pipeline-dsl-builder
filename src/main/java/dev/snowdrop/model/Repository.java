package dev.snowdrop.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Repository {
   private String url;
   private String context;
   private String revision;
   private String dockerfileUrl;
   private String dockerfilePath;

   private static String DEFAULT_REVISION = "main";
   private static String DEFAULT_CONTEXT = ".";

   public Repository setDefaultValues() {
      if (this.getContext() == null) {
         this.context = DEFAULT_CONTEXT;
      }
      if (this.getRevision() == null) {
         this.revision = DEFAULT_REVISION;
      }
      if (dockerfileUrl == null) {
         this.dockerfileUrl = this.getDockerfilePath();
      }
      return this;
   }

   /*
   public void setDockerfilePath(String path) {
      if (path != null) {
         this.dockerfilePath = path;
         if (dockerfileUrl == null) {
            this.dockerfileUrl = path;
         }
      }
   }

   public void setRevision(String name) {
      if (name == null || name.isEmpty()) {
         this.revision = DEFAULT_REVISION;
      } else {
         this.revision = name;
      }
   }

   public void setContext(String ctx) {
      if (ctx == null || ctx.isEmpty()) {
         this.context = DEFAULT_CONTEXT;
      } else {
         this.context = ctx;
      }
   }
   */
}
