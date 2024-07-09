package dev.snowdrop;

import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.tekton.pipeline.v1.*;

public class PipelineGenerator {
    public static void main(String[] args) {
        // @formatter:off
        Pipeline pipeline = new PipelineBuilder()
                .withNewMetadata()
                    .withName("pipeline-konflux")
                .endMetadata()
                .withNewSpec()
                    .withParams()
                        .addNewParam().withName("foo").withType("string").endParam()
                    .withTasks()
                        // Task 0
                        .addNewTask()
                            .withName("task-0")
                        .endTask()

                        // Task 1
                        .addNewTask()
                            .withName("task-1")
                            .withRunAfter("task-0")
                            .withParams()
                                .addNewParam().withName("param1").withValue(new ParamValue("val1")).endParam()
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

        // Generate Pipeline
        String yamlPipeline = Serialization.asYaml(pipeline);
        System.out.println("Created: " + yamlPipeline);
    };
}
