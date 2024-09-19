package dev.snowdrop.factory.tekton;

import dev.snowdrop.factory.AnnotationsProviderFactory;
import dev.snowdrop.factory.LabelsProviderFactory;
import dev.snowdrop.factory.Type;
import dev.snowdrop.model.Configurator;
import io.fabric8.tekton.pipeline.v1.*;

import java.util.List;

import static dev.snowdrop.factory.WorkfowResourceBuilder.populateTimeOut;

public class PipelineRunBuilder {
    public static PipelineRun generatePipelineRun(Type TYPE, Configurator cfg, List<PipelineTask> tasks, List<Param> params, List<WorkspaceBinding> pipelineWorkspaces, List<PipelineResult> pipelineResults) {
        // @formatter:off
        PipelineRun pipelineRun = new io.fabric8.tekton.pipeline.v1.PipelineRunBuilder()
            .withNewMetadata()
              .withName(cfg.getJob().getName())
              .withLabels(LabelsProviderFactory.getProvider(TYPE).getPipelineLabels(cfg))
              .withAnnotations(AnnotationsProviderFactory.getProvider(TYPE).getPipelineAnnotations(cfg))
              .withNamespace(cfg.getNamespace())
            .endMetadata()
            .withNewSpec()
              .withParams(params)
              .withWorkspaces(pipelineWorkspaces)
              .withTimeouts(populateTimeOut(cfg.getJob().getTimeout()))
              .withNewPipelineSpec()
                .withResults(pipelineResults)
                .withTasks(tasks)
              .endPipelineSpec()
            .endSpec()
            .build();
        // @formatter:on
        return pipelineRun;
    }

}
