package dev.snowdrop;

import dev.snowdrop.factory.Type;
import dev.snowdrop.model.Configurator;
import dev.snowdrop.model.Domain;
import dev.snowdrop.service.ConfiguratorSvc;
import io.fabric8.tekton.pipeline.v1.Pipeline;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import static dev.snowdrop.factory.konflux.pipeline.Pipelines.*;
import static dev.snowdrop.factory.tekton.pipeline.Pipelines.*;
import static dev.snowdrop.service.ApplicationComponentBuilder.createApplication;
import static dev.snowdrop.service.ApplicationComponentBuilder.createComponent;

@TopCommand
@Command(name = "pipelinebuilder", mixinStandardHelpOptions = true, description = "Application generating Tekton Pipeline for Konflux")
public class PipeBuilderApp implements Runnable {

   private static final Logger logger = LoggerFactory.getLogger(PipeBuilderApp.class);

   @Option(names = {"-c", "--configuration-path"}, description = "The path of the configuration file", required = true)
   String configuration;

   @Option(names = {"-o", "--output-path"}, description = "The output path", required = true)
   String outputPath;

   public static void main(String[] args) {
      int exitCode = new CommandLine(new PipeBuilderApp()).execute(args);
      System.exit(exitCode);
   }

   @Override
   public void run() {
      logger.info("#### Configuration path: {}", configuration);
      logger.debug("#### Output path: {}", outputPath);

      // Parse and validate the configuration
      Configurator cfg = ConfiguratorSvc.LoadConfiguration(configuration);

      if (cfg == null) {
         logger.error("Configuration file cannot be empty !");
         System.exit(1);
      }

      if (cfg.getType() == null) {
         logger.error("Type is missing from the configuration yaml file !");
         System.exit(1);
      } else {
         logger.info("#### Type selected: {}", cfg.getType().toUpperCase());
      }

      if (cfg.getJob().getDomain() == null) {
         cfg.getJob().setDomain(Domain.EXAMPLE.name());
      }
      logger.info("#### Pipeline domain selected: {}", cfg.getJob().getDomain());

      Pipeline pipeline = null;
      String resourcesPath = outputPath + "/" + cfg.getJob().getDomain();

      // Type: Tekton and Domain: example
      if (cfg.getType().toUpperCase().equals(Type.TEKTON.name()) &&
          cfg.getJob().getDomain().toUpperCase().equals(Domain.EXAMPLE.name())) {
         // TODO: Enhance the factory to be able to generate the resource according to the resourceType: Pipeline, PipelineRun, Task
         ConfiguratorSvc.writeYaml(createExample(cfg), resourcesPath);
      }

      // Type: Tekton
      // Domain: pack
      // Resource generated: PipelineRun
      if (cfg.getType().toUpperCase().equals(Type.TEKTON.name()) &&
         cfg.getJob().getDomain().toUpperCase().equals(Domain.PACK.name())) {
         // TODO: Enhance the factory to be able to generate the resource according to the resourceType: Pipeline, PipelineRun, Task
         ConfiguratorSvc.writeYaml(createPackBuilder(cfg), resourcesPath);
      }

      // Type: Konflux
      // Domain: build
      // Resource generated: PipelineRun
      // TODO: To be reviewed as not complete
      if (cfg.getType().toUpperCase().equals(Type.KONFLUX.name()) &&
          cfg.getJob().getDomain().toUpperCase().equals(Domain.BUILD.name())) {
         // TODO: Enhance the factory to be able to generate the resource according to the resourceType: Pipeline, PipelineRun, Task
         ConfiguratorSvc.writeYaml(createBuild(cfg), resourcesPath);
      }

      // Type: Konflux
      // Domain: buildpack
      // Resource generated: Pipeline
      // TODO: To be reviewed as not complete
      if (cfg.getType().toUpperCase().equals(Type.KONFLUX.name()) &&
          cfg.getJob().getDomain().toUpperCase().equals(Domain.BUILDPACK.name())) {
         // TODO: Enhance the factory to be able to generate the resource according to the resourceType: Pipeline, PipelineRun, Task
         ConfiguratorSvc.writeYaml(createBuilder(cfg), resourcesPath);
      }

      // TODO: Add a boolean to enable to generate the following resources
      if (cfg.getType().toUpperCase().equals(Type.KONFLUX.name())) {
         // Generate the Application, Component CR
         ConfiguratorSvc.writeYaml(createApplication(cfg), resourcesPath);
         ConfiguratorSvc.writeYaml(createComponent(cfg), resourcesPath);
      }
   }
}
