package dev.snowdrop.factory.tekton;

import dev.snowdrop.factory.Type;
import dev.snowdrop.model.Action;
import dev.snowdrop.model.Configurator;
import io.fabric8.tekton.pipeline.v1.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static dev.snowdrop.factory.WorkfowResourceBuilder.*;
import static dev.snowdrop.model.Action.STEP_SCRIPT_IMAGE;

public class TaskRunBuilder {
    public static io.fabric8.tekton.pipeline.v1.TaskRun generateTaskRun(Type TYPE, Configurator cfg, List<Param> params, List<WorkspaceBinding> pipelineWorkspaces, List<PipelineResult> pipelineResults) {
        List<Action> actions = cfg.getJob().getActions();
        // @formatter:off
        TaskRun taskRun = new io.fabric8.tekton.pipeline.v1.TaskRunBuilder()
            .withNewMetadata()
              .withName(cfg.getJob().getName())
              .withLabels(Map.of("tekton.dev/taskRun",cfg.getJob().getName()))
              .withNamespace(cfg.getNamespace())
            .endMetadata()
            .withNewSpec()
              .withParams(params) // Parameters will be propagated to the steps
              .withWorkspaces(pipelineWorkspaces)
              //.withServiceAccountName("") //TODO
              //.withRetries(0) // TODO
              //.withDebug(null) // TODO
              .withNewTaskSpec()
                .withResults(cfg.getJob().getResults() != null ? populateTaskResults(cfg.getJob().getResults()) : null)
                .withWorkspaces() // TODO
                // .withParams() // TODO: Check if that make sense to have them here too vs Spec level
                .withSteps(
                    actions.stream()
                        .map(action -> new StepBuilder()
                            .withName(action.getName())
                            .withArgs(action.getArgs() != null ? action.getArgs() : null)
                            .withCommand(action.getCommand() != null ? action.getCommand() : null)
                            .withImage(action.getImage() != null ? action.getImage() : Action.IMAGES.get(STEP_SCRIPT_IMAGE))
                            .withEnv(populateStepEnvVars(action))
                            .withScript(action.getScript())
                            .withVolumeMounts(populateTaskVolumeMounts(action))
                            .build())
                        .collect(Collectors.toList()))
              .endTaskSpec()
            .endSpec()
        .build();

        // @formatter:on
        return taskRun;
    }
}
