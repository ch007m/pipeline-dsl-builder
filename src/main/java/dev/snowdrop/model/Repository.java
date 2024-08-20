package dev.snowdrop.model;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Repository {
   private String url;

   @JsonSetter(nulls = Nulls.SKIP)
   private String context = DEFAULT_CONTEXT;

   @JsonSetter(nulls = Nulls.SKIP)
   private String revision = DEFAULT_REVISION;
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

   public void setDockerfilePath(String path) {
      if (path != null) {
         this.dockerfilePath = path;
         if (dockerfileUrl == null) {
            this.dockerfileUrl = path;
         }
      }
   }
}
