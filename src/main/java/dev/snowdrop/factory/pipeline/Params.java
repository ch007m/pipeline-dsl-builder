package dev.snowdrop.factory.pipeline;

import io.fabric8.tekton.pipeline.v1.*;

import java.util.ArrayList;
import java.util.List;

public class Params {

   public static List<ParamSpec> KONFLUX_PIPELINE_PARAMS() {
      List<ParamSpec> params = new ArrayList<>();
      params.add(new ParamSpecBuilder().withDescription("Source Repository URL").withName("git-url").withType("string").build());
      params.add(new ParamSpecBuilder().withDescription("Revision of the Source Repository").withName("revision").withType("string").withNewDefault("").build());
      return params;
   }
}
