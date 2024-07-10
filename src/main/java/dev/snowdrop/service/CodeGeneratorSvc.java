package dev.snowdrop.service;

import dev.snowdrop.factory.pipeline.Params;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

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
}
