package dev.snowdrop.command;

import dev.snowdrop.command.fetch.OCIBundleFetchCommand;
import io.fabric8.tekton.pipeline.v1.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import picocli.CommandLine;

import java.io.InputStream;
import java.net.URL;

import static dev.snowdrop.service.CodeGeneratorSvc.*;

@CommandLine.Command(name = "yamlToJava", description = "Generate a Java object from Tekton Task YAML")
public class YamlToJavaCommand implements Runnable {

   @CommandLine.Option(names = {"-t", "--type"}, description = "The type of the resource to generate: params, workspaces, task, git-task", required = true)
   String type;

   @CommandLine.Option(names = {"-p", "--path"}, description = "Path of the yaml resource file to process", required = true)
   String path;

   private static final Logger logger = LoggerFactory.getLogger(OCIBundleFetchCommand.class);

   @Override
   public void run() {
      switch (type) {
         case "params": generateParamsFromYAML(path); break;
         case "tasks": generateTaskFromYAML(path); break;
         case "workspaces": generateWorkspacesFromYAML(path); break;
         case "git-task": loadYAML(path); break;
         default: logger.warn("Unrecognized type {}", type);
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
