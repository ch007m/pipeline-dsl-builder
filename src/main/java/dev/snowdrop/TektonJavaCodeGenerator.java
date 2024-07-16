package dev.snowdrop;

import io.fabric8.tekton.pipeline.v1.Task;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import static dev.snowdrop.service.CodeGeneratorSvc.*;

public class TektonJavaCodeGenerator {
   public static void main(String[] args) {
      if (args.length < 2) {
         System.err.println("Please provide as arguments; the type and the YAML file path: ");
         System.err.println("TektonJavaCodeGenerator params files/konflux_template/params.yaml");
         System.exit(1);
      }

      String type = args[0];
      if (type.equals("params")) {
         generateParamsFromYAML(args[1]);
      }

      if (type.equals("task")) {
         generateTaskFromYAML(args[1]);
      }

      if (type.equals("workspaces")) {
         generateWorkspacesFromYAML(args[1]);
      }
      if (type.equals("git-task")) {
         loadYAML(args[1]);
      }
   }

   public static void loadYAML(String aUrl) {
      try {
         // Specify the URL of the YAML file
         URL url = new URL(aUrl);

         // Open an InputStream from the URL
         try (InputStream inputStream = url.openStream()) {

            // Create a new Yaml instance
            Yaml yaml = new Yaml(new Constructor(Task.class, new LoaderOptions()));

            // Parse the YAML file into a Java object
            Task task = yaml.load(inputStream);

            // Print the parsed data
            System.out.println(task);
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}
