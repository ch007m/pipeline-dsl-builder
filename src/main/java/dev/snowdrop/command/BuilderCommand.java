package dev.snowdrop.command;

import dev.snowdrop.factory.TektonResource;
import dev.snowdrop.factory.Type;
import dev.snowdrop.model.Configurator;
import dev.snowdrop.model.Domain;
import dev.snowdrop.service.ConfiguratorSvc;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Paths;

import static dev.snowdrop.factory.konflux.component.ComponentBuilder.createComponent;
import static dev.snowdrop.factory.konflux.pipeline.Pipelines.createBuild;
import static dev.snowdrop.factory.konflux.application.ApplicationBuilder.createApplication;

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

        // Set the outputPath to the configurator object
        cfg.setOutputPath(outputPath);

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

        String resourcesPath = Paths.get(cfg.getOutputPath(), cfg.getType(), cfg.getDomain()).toString();
        Type providerType = Type.valueOf(cfg.getType().toUpperCase());
        Domain domain = Domain.valueOf(cfg.getDomain().toUpperCase());

        switch (providerType) {
            case TEKTON:
                ConfiguratorSvc.writeYaml(TektonResource.create(cfg), resourcesPath);
                break;
            case KONFLUX:
                // Set default values for the Repository when not defined part of the configuration yaml
                // TODO: To be reviewed to see if we can do that during yaml parsing
                cfg.getRepository().setDefaultValues();

                // TODO: When the konflux switch code will be reviewed and removed, then this code should be moved after.
                if (cfg.getApplication() != null && cfg.getApplication().isEnable()) {
                    ConfiguratorSvc.writeYaml(createApplication(cfg), resourcesPath);
                }

                if (cfg.getComponent() != null &&  cfg.getComponent().isEnable()) {
                    ConfiguratorSvc.writeYaml(createComponent(cfg), resourcesPath);
                }

                // TODO: To be reviewed as generated resources still include hard coded values, etc
                switch (domain) {
                    case BUILD:
                        // Resource generated: PipelineRun
                        ConfiguratorSvc.writeYaml(createBuild(cfg), resourcesPath);
                        break;
                    case BUILDPACK:
                        // Resource generated: PipelineRun
                        ConfiguratorSvc.writeYaml(createBuild(cfg), resourcesPath);
                        break;
                } // end of switch domain
        } // end of switch providerType
    } // end of run method
}
