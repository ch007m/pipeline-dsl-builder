package dev.snowdrop.factory.task;

import io.fabric8.tekton.pipeline.v1.*;

import java.util.ArrayList;
import java.util.List;

public class Params {

   public static List<Param> KONFLUX_PARAMS() {
      List<Param> params = new ArrayList<>();
      params.add(new ParamBuilder().withName("IMAGE").withValue(new ParamValue("$(params.output-image")).build());
      params.add(new ParamBuilder().withName("DOCKERFILE").withValue(new ParamValue("$(params.dockerfile")).build());
      params.add(new ParamBuilder().withName("CONTEXT").withValue(new ParamValue("$(params.path-context")).build());
      params.add(new ParamBuilder().withName("HERMETIC").withValue(new ParamValue("$(params.hermetic")).build());
      params.add(new ParamBuilder().withName("PREFETCH_INPUT").withValue(new ParamValue("$(params.prefetch-input")).build());
      params.add(new ParamBuilder().withName("IMAGE_EXPIRES_AFTER").withValue(new ParamValue("$(params.image-expires-after")).build());
      params.add(new ParamBuilder().withName("COMMIT_SHA").withValue(new ParamValue("$(tasks.clone-repository.results.commit")).build());
      params.add(new ParamBuilder().withName("APP_IMAGE").withValue(new ParamValue("$(params.output-image")).build());
      params.add(new ParamBuilder().withName("SOURCE_SUBPATH").withValue(new ParamValue("$(params.sourceSubPath")).build());
      return params;
   }

   public static List<Param> CNB_PARAMS() {
      List<Param> params = new ArrayList<>();
      params.add(new ParamBuilder().withName("IMAGE").withValue(new ParamValue("$(params.output-image")).build());
      params.add(new ParamBuilder().withName("DOCKERFILE").withValue(new ParamValue("$(params.dockerfile")).build());
      params.add(new ParamBuilder().withName("CONTEXT").withValue(new ParamValue("$(params.path-context")).build());
      params.add(new ParamBuilder().withName("HERMETIC").withValue(new ParamValue("$(params.hermetic")).build());
      params.add(new ParamBuilder().withName("PREFETCH_INPUT").withValue(new ParamValue("$(params.prefetch-input")).build());
      params.add(new ParamBuilder().withName("IMAGE_EXPIRES_AFTER").withValue(new ParamValue("$(params.image-expires-after")).build());
      params.add(new ParamBuilder().withName("COMMIT_SHA").withValue(new ParamValue("$(tasks.clone-repository.results.commit")).build());
      params.add(new ParamBuilder().withName("APP_IMAGE").withValue(new ParamValue("$(params.output-image")).build());
      params.add(new ParamBuilder().withName("SOURCE_SUBPATH").withValue(new ParamValue("$(params.sourceSubPath")).build());
      params.add(new ParamBuilder().withName("CNB_BUILDER_IMAGE").withValue(new ParamValue("$(params.cnbBuilderImage")).build());
      params.add(new ParamBuilder().withName("CNB_LIFECYCLE_IMAGE").withValue(new ParamValue("$(params.cnbLifecycleImage")).build());
      params.add(new ParamBuilder().withName("CNB_EXPERIMENTAL_MODE").withValue(new ParamValue("$(params.cnbExperimentalMode")).build());
      params.add(new ParamBuilder().withName("CNB_LOG_LEVEL").withValue(new ParamValue("$(params.cnbLogLevel")).build());
      params.add(new ParamBuilder().withName("CNB_RUN_IMAGE").withValue(new ParamValue("$(params.cnbRunImage")).build());
      params.add(new ParamBuilder().withName("CNB_BUILD_IMAGE").withValue(new ParamValue("$(params.cnbBuildImage")).build());
      params.add(new ParamBuilder().withName("CNB_USER_ID").withValue(new ParamValue("$(tasks.buildpacks-extension-check.results.uid")).build());
      params.add(new ParamBuilder().withName("CNB_GROUP_ID").withValue(new ParamValue("$(tasks.buildpacks-extension-check.results.gid")).build());
      params.add(new ParamBuilder().withName("CNB_ENV_VARS").withValue(new ParamValue(List.of("$(params.cnbBuildEnvVars)"))).build());
      return params;
   }
}
