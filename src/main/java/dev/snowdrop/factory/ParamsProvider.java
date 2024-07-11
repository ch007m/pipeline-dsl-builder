package dev.snowdrop.factory;

import io.fabric8.tekton.pipeline.v1.Param;

import java.util.List;

public interface ParamsProvider {
   List<Param> getPipelineParams();
   List<Param> getTaskParams();
}
