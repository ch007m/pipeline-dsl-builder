package dev.snowdrop.factory.tekton;

import dev.snowdrop.factory.AnnotationsProviderFactory;
import dev.snowdrop.factory.LabelsProviderFactory;
import dev.snowdrop.factory.Type;
import dev.snowdrop.model.Action;
import dev.snowdrop.model.Configurator;
import io.fabric8.tekton.pipeline.v1.StepBuilder;
import io.fabric8.tekton.pipeline.v1.TaskRun;
import io.fabric8.tekton.pipeline.v1.TaskRunDebugBuilder;
import io.fabric8.tekton.pipeline.v1.WorkspaceBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static dev.snowdrop.model.Action.STEP_SCRIPT_IMAGE;

public class TaskRunBuilder {
    public static io.fabric8.tekton.pipeline.v1.TaskRun generateTaskRun(Type TYPE, Configurator cfg, List<WorkspaceBinding> pipelineWorkspaces) {
        List<Action> actions = cfg.getJob().getActions();
        // @formatter:off
        TaskRun taskRun = new io.fabric8.tekton.pipeline.v1.TaskRunBuilder()
            .withNewMetadata()
              .withName(cfg.getJob().getName())
              .withLabels(LabelsProviderFactory.getProvider(TYPE).getPipelineLabels(cfg))
              .withAnnotations(AnnotationsProviderFactory.getProvider(TYPE).getPipelineAnnotations(cfg))
              .withNamespace(cfg.getNamespace())
            .endMetadata()
            .withNewSpec()
              .withParams() // TODO
              .withWorkspaces(pipelineWorkspaces) // TODO
              //.withServiceAccountName("") //TODO
              //.withRetries(0) // TODO
              //.withDebug(null) // TODO
              .withNewTaskSpec()
                .withResults()
                .withWorkspaces() // TODO
                .withSteps(
                    actions.stream()
                        .map(action -> new StepBuilder()
                            .withName(action.getName())
                            .withArgs("args") // TODO
                            .withImage(action.getImage() != null ? action.getImage() : Action.IMAGES.get(STEP_SCRIPT_IMAGE)) // TODO
                            .withScript(action.getScript())
                            .withVolumeMounts() // TODO
                            .build())
                        .collect(Collectors.toList()))
              .endTaskSpec()
            .endSpec()
        .build();

        // @formatter:on
        return taskRun;
    }
}
