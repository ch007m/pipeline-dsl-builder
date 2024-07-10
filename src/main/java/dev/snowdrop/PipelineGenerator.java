package dev.snowdrop;

import io.fabric8.tekton.pipeline.v1.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static dev.snowdrop.ParamsFactories.*;

public class PipelineGenerator {

    private static final Logger logger = LoggerFactory.getLogger(PipelineGenerator.class);

    public static Pipeline generatePipeline() {
        // @formatter:off
        Pipeline pipeline = new PipelineBuilder()
                .withNewMetadata()
                   .withName("pipeline-konflux")
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

                       // Task 2
                       .addNewTask()
                          .withName("task-2")
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

    public static void writeYamlToFile(String outputPath, String fileName, String yamlContent) {
        Path path = Paths.get(outputPath, fileName+".yaml");
        try {
            Files.write(path, yamlContent.getBytes());
            logger.info("YAML written to: " + outputPath);
        } catch (IOException e) {
            logger.error("Failed to write YAML to file: " + e.getMessage());
        }
    }
}