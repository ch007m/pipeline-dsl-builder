package dev.snowdrop.command;

import dev.snowdrop.factory.TektonResource;
import dev.snowdrop.model.Configurator;
import dev.snowdrop.model.Domain;
import dev.snowdrop.service.ConfiguratorSvc;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Paths;

import static dev.snowdrop.factory.konflux.builder.ComponentBuilder.createComponent;
import static dev.snowdrop.factory.konflux.builder.ApplicationBuilder.createApplication;
import static dev.snowdrop.service.FileUtilSvc.readFileFromResources;

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

        // Load the default configuration
        String resourcesConfigurationPath = "dev/snowdrop/configuration/konflux-default-pipeline.yaml";
        String configYaml = readFileFromResources(resourcesConfigurationPath);
        Configurator defaultCfg = ConfiguratorSvc.LoadConfiguration(configYaml);
        logger.info("#### Default configuration loaded: {}", resourcesConfigurationPath);

        // Parse and validate the user's configuration file
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

        // Use the factory to generate the resources according to the provider and the type
        ConfiguratorSvc.writeYaml(TektonResource.create(cfg), resourcesPath);

        // TODO: Is it the best place to create such YAML resources. To be reviewed
        if (cfg.getApplication() != null && cfg.getApplication().isEnable()) {
            ConfiguratorSvc.writeYaml(createApplication(cfg), resourcesPath);
        }

        if (cfg.getComponent() != null && cfg.getComponent().isEnable()) {
            ConfiguratorSvc.writeYaml(createComponent(cfg), resourcesPath);
        }
    } // end of run method
}
