package dev.snowdrop.service;

import dev.snowdrop.model.Configurator;
import io.fabric8.tekton.pipeline.v1.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.snowdrop.factory.task.Params.*;
import static dev.snowdrop.factory.pipeline.Labels.*;
import static dev.snowdrop.factory.pipeline.Params.*;
import static dev.snowdrop.factory.pipeline.Results.*;
import static dev.snowdrop.factory.pipeline.Workspaces.*;

public class PipelineGeneratorSvc {

    private static final Logger logger = LoggerFactory.getLogger(PipelineGeneratorSvc.class);

    public static Pipeline createBuilder(Configurator cfg) {
        // @formatter:off
        Pipeline pipeline = new PipelineBuilder()
                .withNewMetadata()
                   .withName(cfg.getBuilder().getName())
                   .withLabels(KONFLUX_PIPELINE_LABELS())
                .endMetadata()
                .withNewSpec()
                   .withWorkspaces(KONFLUX_PIPELINE_WORKSPACES())
                   .withParams(KONFLUX_PIPELINE_PARAMS())
                   .withResults(KONFLUX_PIPELINE_RESULTS())
                   .withFinally()
                        .addNewFinally()
                          .withName("show-sbom")
                          .withNewTaskRef()
                            // There is a problem here as konflux pipeline uses as fields: name + version
                            .withName("show-sbom")
                            .withApiVersion("0.1")
                            .withKind("")
                          .endTaskRef()
                          .withParams()
                            .addNewParam().withName("IMAGE_URL").withValue(new ParamValue("$(tasks.build-container.results.IMAGE_URL)")).endParam()
                        .endFinally()
                        .addNewFinally()
                          .withName("show-summary")
                          .withNewTaskRef()
                             .withName("summary")
                             .withApiVersion("0.2")
                             .withKind("")
                          .endTaskRef()
                          .withParams()
                              .addNewParam().withName("pipelinerun-name").withValue(new ParamValue("$(context.pipelineRun.name)")).endParam()
                              .addNewParam().withName("git-url").withValue(new ParamValue("$(tasks.clone-repository.results.url)?rev=$(tasks.clone-repository.results.commit)")).endParam()
                              .addNewParam().withName("image-url").withValue(new ParamValue("$(params.output-image)")).endParam()
                              .addNewParam().withName("build-task-status").withValue(new ParamValue("$(tasks.build-container.status)")).endParam()
                        .endFinally()
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