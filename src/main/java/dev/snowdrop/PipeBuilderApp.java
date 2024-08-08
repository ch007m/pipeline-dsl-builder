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

import static dev.snowdrop.factory.Type.KONFLUX;
import static dev.snowdrop.factory.Type.TEKTON;
import static dev.snowdrop.factory.konflux.pipeline.Pipelines.*;
import static dev.snowdrop.factory.tekton.pipeline.Pipelines.*;
import static dev.snowdrop.model.Domain.BUILD;
import static dev.snowdrop.model.Domain.BUILDPACK;
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

        if (cfg.getDomain() == null) {
            cfg.setDomain(Domain.EXAMPLE.name());
        }
        logger.info("#### Pipeline domain selected: {}", cfg.getDomain());

        String resourcesPath = outputPath + "/" + cfg.getDomain();
        Type providerType = Type.valueOf(cfg.getType().toUpperCase());
        Domain domain = Domain.valueOf(cfg.getDomain().toUpperCase());

        switch (providerType) {
            case TEKTON:
                ConfiguratorSvc.writeYaml(createResource(cfg), resourcesPath);
                break;
            case KONFLUX:
                // TODO: To be reviewed as not generated resources still include hard coded values, etc
                switch (domain) {
                    case BUILD:
                        // Resource generated: PipelineRun
                        ConfiguratorSvc.writeYaml(createBuild(cfg), resourcesPath);
                        break;
                    case BUILDPACK:
                        // Resource generated: Pipeline
                        ConfiguratorSvc.writeYaml(createBuilder(cfg), resourcesPath);
                        break;
                } // end of switch domain
        } // end of switch providerType
    } // end of run method
}
