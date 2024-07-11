package dev.snowdrop.service;

import dev.snowdrop.factory.AnnotationsProviderFactory;
import dev.snowdrop.factory.Flavor;
import dev.snowdrop.factory.LabelsProviderFactory;
import dev.snowdrop.model.Configurator;
import io.fabric8.tekton.pipeline.v1.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.snowdrop.factory.konflux.pipeline.Tasks.CLONE_REPOSITORY;
import static dev.snowdrop.factory.konflux.pipeline.Tasks.INIT;
import static dev.snowdrop.factory.konflux.task.Params.*;
import static dev.snowdrop.factory.konflux.pipeline.Finally.*;
import static dev.snowdrop.factory.konflux.pipeline.Params.*;
import static dev.snowdrop.factory.konflux.pipeline.Results.*;
import static dev.snowdrop.factory.konflux.pipeline.Workspaces.*;

public class PipelineGeneratorSvc {

    private static final Logger logger = LoggerFactory.getLogger(PipelineGeneratorSvc.class);

    public static Pipeline createBuilder(Configurator cfg) {
        final Flavor FLAVOR = Flavor.valueOf(cfg.getFlavor().toUpperCase());
        // @formatter:off
        Pipeline pipeline = new PipelineBuilder()
                .withNewMetadata()
                   .withName(cfg.getPipeline().getName())
                   .withLabels(LabelsProviderFactory.getProvider(FLAVOR).getPipelineLabels())
                   .withAnnotations(AnnotationsProviderFactory.getProvider(FLAVOR).getPipelineAnnotations())
                .endMetadata()
                .withNewSpec()
                   .withWorkspaces(KONFLUX_PIPELINE_WORKSPACES())
                   .withParams(KONFLUX_PIPELINE_PARAMS())
                   .withResults(KONFLUX_PIPELINE_RESULTS())
                   .withFinally(KONFLUX_PIPELINE_FINALLY())
                   .withTasks()
                       // Task 0
                       .addNewTask()
                          .withName("task-bundle-resolver")
                          .withNewTaskRef()
                             .withResolver("bundles")
                               .withParams()
                                 .addNewParam().withName("bundle").withValue(new ParamValue("quay.io/redhat-appstudio-tekton-catalog/task-git-clone:0.1@sha256:1f84973a21aabea38434b1f663abc4cb2d86565a9c7aae1f90decb43a8fa48eb")).endParam()
                                 .addNewParam().withName("name").withValue(new ParamValue("git-clone")).endParam()
                          .endTaskRef()
                       .endTask()

                       .withTasks(
                          INIT(),
                          CLONE_REPOSITORY()
                       )

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
                .endSpec()
                .build();
        // @formatter:on
        return pipeline;
    }
}