package dev.snowdrop.service;

import dev.snowdrop.factory.konflux.pipeline.Params;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.tekton.client.DefaultTektonClient;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1.Task;
import io.fabric8.tekton.pipeline.v1.TaskBuilder;
import io.fabric8.tekton.pipeline.v1.TaskSpec;
import io.fabric8.tekton.pipeline.v1.TaskSpecBuilder;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CodeGeneratorSvc {

   public static void generateParamsFromYAML(String yamlPath) {
      Yaml yaml = new Yaml(new Constructor(List.class, new LoaderOptions()));
      InputStream inputStream = Params.class
         .getClassLoader()
         .getResourceAsStream(yamlPath);

      List<Map<String, Object>> params = yaml.load(inputStream);

      StringBuilder javaCode = new StringBuilder();

      for (Map<String, Object> param : params) {
         javaCode.append("params.add(new ParamSpecBuilder()")
            .append(".withDescription(\"").append(param.get("description")).append("\")")
            .append(".withName(\"").append(param.get("name")).append("\")")
            .append(".withType(\"").append(param.get("type")).append("\")");

         if (param.containsKey("default")) {
            javaCode.append(".withNewDefault(\"").append(param.get("default")).append("\")");
         }

         javaCode.append(".build());\n");
      }

      System.out.println(javaCode.toString());
   }

   public static void generateTaskFromYAML(String resourcePath) {
       try {
          TektonClient tkn = new DefaultTektonClient();
          TaskSpec task = tkn.v1().tasks().load(new File(resourcePath)).item().getSpec();

          TaskSpec tsBuilder = new TaskSpecBuilder(task).build();
          System.out.println("Task : " + task );
       } catch (Exception e) {
           throw new RuntimeException(e);
       }
   }
}
