package dev.snowdrop.command;

import dev.snowdrop.factory.Type;
import dev.snowdrop.model.Configurator;
import dev.snowdrop.model.Domain;
import dev.snowdrop.service.ConfiguratorSvc;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import static dev.snowdrop.factory.konflux.pipeline.Pipelines.createBuild;
import static dev.snowdrop.factory.konflux.pipeline.Pipelines.createBuilder;
import static dev.snowdrop.factory.tekton.pipeline.Pipelines.createResource;

@TopCommand
@Command(name = "builder", mixinStandardHelpOptions = true, description = "Application generating Tekton Pipeline(run)s")
public class BuilderCommand implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(BuilderCommand.class);

    @Option(names = {"-c", "--configuration-path"}, description = "The path of the configuration file", required = true)
    String configuration;

    @Option(names = {"-o", "--output-path"}, description = "The output path", required = true)
    String outputPath;

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

        String resourcesPath = outputPath + "/" + cfg.getType() + "/" + cfg.getDomain();
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