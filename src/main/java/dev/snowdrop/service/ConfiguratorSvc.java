package dev.snowdrop.service;

import dev.snowdrop.command.BuilderCommand;
import dev.snowdrop.model.Configurator;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfiguratorSvc {

   private static final Logger logger = LoggerFactory.getLogger(BuilderCommand.class);

   public static Configurator LoadConfiguration(String configFile) {
      Configurator configurator = new Configurator();

      // Load the YAML file
      try {
         configurator = loadYaml(configFile);
      } catch(Exception e) {
         e.printStackTrace();
      }
      return configurator;
   }

   public static Configurator loadYaml(String filePath) {
      // Create a YAML parser
      Yaml yaml = new Yaml(new Constructor(Configurator.class, new LoaderOptions()));

      // Load the YAML using the configFile path
      try {
         InputStream inputStream = new FileInputStream(filePath);
         return yaml.load(inputStream);
      } catch (IOException e) {
         e.printStackTrace();
         return null;
      }
   }

   public static void writeYaml(HasMetadata resource, String outputPath) {
      try {
         // Convert the resource(run) to YAML
         String yamlResource = Serialization.asYaml(resource);
         // TODO : find a better way to escape double quotes
         // yamlResource = yamlResource.replaceAll("\\\\\"", "\"");
         logger.debug("Created YAML: \n{}", yamlResource);

         // Use the kubernetes kind as filename
         String fileName = resource.getKind().toLowerCase() + "-" + resource.getMetadata().getName();
         writeYamlToFile(outputPath, fileName, yamlResource);
      } catch (Exception e) {
         logger.error(e.getMessage());
      }
   }

   public static void writeYamlToFile(String outputPath, String fileName, String yamlContent) {
      Path filePath = Paths.get(outputPath, fileName+".yaml");
      try {
         File dir = new File(outputPath);
         dir.mkdirs();

         File f = new File(String.valueOf(filePath));
         Files.write(f.toPath(), yamlContent.getBytes());
         logger.info("### YAML file generated: " + filePath);

      } catch (Exception e) {
         logger.error("Failed to write YAML to file: " + e.getMessage());
      }
   }
}
