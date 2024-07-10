package dev.snowdrop;

import dev.snowdrop.model.Configurator;
import dev.snowdrop.service.ConfiguratorSvc;
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
      logger.info("#### Output path: " + outputPath);

      // Parse and validate the configuration
      Configurator cfg = ConfiguratorSvc.LoadConfiguration(configuration);

      // Generate a Pipeline according to the configurator
      Pipeline pipeline = PipelineGenerator.generatePipeline(cfg);

      ConfiguratorSvc.writeYaml(pipeline, outputPath);


   }
}
