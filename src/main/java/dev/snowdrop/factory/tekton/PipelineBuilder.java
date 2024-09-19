package dev.snowdrop.factory.tekton;

import dev.snowdrop.factory.AnnotationsProviderFactory;
import dev.snowdrop.factory.LabelsProviderFactory;
import dev.snowdrop.factory.Type;
import dev.snowdrop.model.Configurator;
import io.fabric8.tekton.pipeline.v1.Pipeline;
import io.fabric8.tekton.pipeline.v1.PipelineTask;
import io.fabric8.tekton.pipeline.v1.WorkspaceBinding;

import java.util.List;

public class PipelineBuilder {
    public static Pipeline generatePipeline(Type TYPE, Configurator cfg, List<PipelineTask> tasks, List<WorkspaceBinding> pipelineWorkspaces) {
        // @formatter:off
        Pipeline pipeline = new io.fabric8.tekton.pipeline.v1.PipelineBuilder()
            .withNewMetadata()
              .withName(cfg.getJob().getName())
              .withLabels(LabelsProviderFactory.getProvider(TYPE).getPipelineLabels(cfg))
              .withAnnotations(AnnotationsProviderFactory.getProvider(TYPE).getPipelineAnnotations(cfg))
              .withNamespace(cfg.getNamespace())
            .endMetadata()
            .withNewSpec()
              .withTasks(tasks)
            .endSpec()
            .build();
        // @formatter:on
        return pipeline;
    }

}
