package dev.snowdrop;

import dev.snowdrop.model.Configurator;
import dev.snowdrop.service.ConfiguratorSvc;
import dev.snowdrop.service.PipelineGeneratorSvc;
import io.fabric8.tekton.pipeline.v1.Pipeline;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

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
      logger.info("#### Configuration path: " + configuration);
      logger.debug("#### Output path: " + outputPath);

      // Parse and validate the configuration
      Configurator cfg = ConfiguratorSvc.LoadConfiguration(configuration);

      if (cfg == null) {
         logger.error("Configuration file cannot be empty !");
         System.exit(1);
      }

      if (cfg.getFlavor() == null) {
         logger.error("Flavor is missing from the configuration yaml file !");
         System.exit(1);
      } else {
         logger.info("#### Flavor selected: " + cfg.getFlavor().toUpperCase());
      }

      Pipeline pipeline = null;

      if (cfg.getBuilder() != null) {
         pipeline = PipelineGeneratorSvc.createBuilder(cfg);
      }

      if (pipeline != null) {
         ConfiguratorSvc.writeYaml(pipeline, outputPath);
      } else {
         logger.error("The pipeline has not been generated properly and is null !");
      }
   }
}
