package dev.snowdrop;

import dev.snowdrop.dsl.PipelineDSL;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.List;

public class Builder {
    public static void main(String[] args) {
        PipelineDSL pipeline = PipelineDSL.create()
                .apiVersion("tekton.dev/v1beta1")
                .kind("Pipeline")
                .metadata()
                  .name("pipeline-rhtap")
                  .endMetadata()
                .spec()
                  .workspaces(List.of(
                          new PipelineDSL.Workspace(null).name("workspace").optional(false),
                          new PipelineDSL.Workspace(null).name("git-auth").optional(true)
                  ))
                  .params(List.of(
                          new PipelineDSL.Param(null).description("A description").name("param1").type("string").defaultValue("default")
                  ))
                  .results(List.of(
                          new PipelineDSL.Result(null).description("A result description").name("result1").value("value1")
                  ))
                  .tasks(List.of(
                          new PipelineDSL.Task(null)
                                  .name("buildpacks-extension-check")
                                  .runAfter("clone-repository")
                                  .params(List.of(
                                          new PipelineDSL.Param(null).name("builderImage").type("string").defaultValue("$(params.cnbBuilderImage)")
                                  ))
                                  .taskRef(new PipelineDSL.TaskRef()
                                    .resolver("git")
                                    .params(List.of(
                                      new PipelineDSL.Param(null).name("url").defaultValue("https://github.com/redhat-buildpacks/catalog.git"),
                                      new PipelineDSL.Param(null).name("revision").defaultValue("main"),
                                      new PipelineDSL.Param(null).name("pathInRepo").defaultValue("tekton/task/buildpacks-extension-check/01/buildpacks-extension-check.yaml")
                                    ))
                                  )
                  ))
                 .endSpec();

        // Use SnakeYAML to convert the pipeline object to YAML
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        String yamlString = yaml.dump(pipeline);

        System.out.println(yamlString);
    }
}
