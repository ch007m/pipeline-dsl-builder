package dev.snowdrop.factory.tekton;

import dev.snowdrop.factory.Type;
import dev.snowdrop.model.Configurator;
import io.fabric8.tekton.pipeline.v1.TaskRun;
import io.fabric8.tekton.pipeline.v1.WorkspaceBinding;

import java.util.List;

public class TaskRunBuilder {
    public static TaskRun generateTaskRun(Type TYPE, Configurator cfg, List<WorkspaceBinding> pipelineWorkspaces) {
        return new io.fabric8.tekton.pipeline.v1.TaskRunBuilder().build();
    }
}
