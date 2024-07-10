package dev.snowdrop;

import dev.snowdrop.model.Configurator;
import dev.snowdrop.service.FileUtilSvc;
import io.fabric8.tekton.pipeline.v1.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.snowdrop.ParamsFactories.*;

public class PipelineGenerator {

    private static final Logger logger = LoggerFactory.getLogger(PipelineGenerator.class);

    public static Pipeline createBuilder(Configurator cfg) {
        // @formatter:off
        Pipeline pipeline = new PipelineBuilder()
                .withNewMetadata()
                   .withName(cfg.getBuilder().getName())
                .endMetadata()
                .withNewSpec()
                   .withWorkspaces()
                      .addNewWorkspace("workspace","workspace",false)
                      .addNewWorkspace("git-auth","git-auth",true)
                   .withParams(KONFLUX_PARAMS_SPEC())
                   .withTasks()
                       // Task 0
                       .addNewTask()
                          .withName("task-0")
                       .endTask()

                       // Task 1
                       .addNewTask()
                          .withName("task-1")
                          .withRunAfter("task-0")
                          .withParams(KONFLUX_PARAMS())
                          .withParams(CNB_PARAMS())
                          .withNewTaskRef()
                             .withResolver("git")
                             .withParams()
                             .addNewParam().withName("url").withValue(new ParamValue("https://github.com/redhat-buildpacks/catalog.git")).endParam()
                          .endTaskRef()
                       .endTask()

                       // Embedded Task with script
                       .addNewTask()
                          .withName("task-embedded-script")
                          .withTaskSpec(
                             new EmbeddedTaskBuilder()
                                .addNewStep()
                                    .withName("run-script")
                                    .withImage("ubuntu")
                                    .withScript(FileUtilSvc.loadFileAsString("echo.sh"))
                                .endStep()
                                .build()
                          )
                       .endTask()

                       // Simple Task with TaskRef and When
                       .addNewTask()
                          .withName("task-ref")
                          .withRunAfter("task-1")
                          .withWhen()
                             .addNewWhen()
                                .withInput("$(tasks.init.results.build)")
                                .withOperator("in")
                                .withValues("true")
                             .endWhen()
                       .endTask()
                .endSpec()
                .build();
        // @formatter:on
        return pipeline;
    }
}