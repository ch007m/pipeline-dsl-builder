package dev.snowdrop.factory.pipeline.konflux;

import io.fabric8.tekton.pipeline.v1.*;

import java.util.ArrayList;
import java.util.List;

public class Results {

   public static List<PipelineResult> KONFLUX_PIPELINE_RESULTS() {
      List<PipelineResult> rs = new ArrayList<>();
      rs.add(new PipelineResultBuilder().withName("IMAGE_URL").withValue(new ParamValue("$(tasks.build-container.results.IMAGE_URL)")).build());
      rs.add(new PipelineResultBuilder().withName("IMAGE_DIGEST").withValue(new ParamValue("$(tasks.build-container.results.IMAGE_DIGEST)")).build());
      rs.add(new PipelineResultBuilder().withName("CHAINS-GIT_URL").withValue(new ParamValue("$(tasks.clone-repository.results.url)")).build());
      rs.add(new PipelineResultBuilder().withName("CHAINS-GIT_COMMIT").withValue(new ParamValue("$(tasks.clone-repository.results.commit)")).build());
      return rs;
   }
}
