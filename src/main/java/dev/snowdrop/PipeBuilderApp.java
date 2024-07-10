package dev.snowdrop;

import io.fabric8.kubernetes.client.utils.Serialization;
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

   @Option(names = {"-c", "--configuration"}, description = "The configuration file", required = true)
   String configuration;

   @Option(names = {"-o", "--output"}, description = "The output file", required = true)
   String output;

   public static void main(String[] args) {
      int exitCode = new CommandLine(new PipeBuilderApp()).execute(args);
      System.exit(exitCode);
   }

   @Override
   public void run() {
      logger.info("#### Configuration: " + configuration);
      logger.info("#### Output: " + output);

      Pipeline pipeline = PipelineGenerator.generatePipeline();
      String yamlPipeline = Serialization.asYaml(pipeline);

      logger.debug("Created YAML: \n{}", yamlPipeline);

      // Write the YAML to the output file
      PipelineGenerator.writeYamlToFile(output, pipeline.getMetadata().getName(), yamlPipeline);
   }
}
