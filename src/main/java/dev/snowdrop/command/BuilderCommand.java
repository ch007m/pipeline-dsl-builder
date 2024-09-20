package dev.snowdrop.command;

import dev.snowdrop.factory.WorkflowResource;
import dev.snowdrop.model.Configurator;
import dev.snowdrop.model.Domain;
import dev.snowdrop.service.ConfiguratorSvc;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Paths;

import static dev.snowdrop.factory.Type.KONFLUX;
import static dev.snowdrop.factory.konflux.builder.ApplicationBuilder.createApplication;
import static dev.snowdrop.factory.konflux.builder.ComponentBuilder.createComponent;

@Slf4j
@TopCommand
@Command(name = "builder", mixinStandardHelpOptions = true, description = "Application generating Tekton Pipeline(run)s")
public class BuilderCommand implements Runnable {

    @Option(names = {"-c", "--configuration-path"}, description = "The path of the configuration file", required = true)
    String configuration;

    @Option(names = {"-o", "--output-path"}, description = "The output path", required = true)
    String outputPath;

    ConfiguratorSvc configuratorSvc = ConfiguratorSvc.getInstance();

    @Override
    public void run() {
        log.info("#### Configuration path: {}", configuration);
        log.debug("#### Output path: {}", outputPath);

        // Parse and validate the user's configuration file
        Configurator cfg = null;
        try {
            cfg = configuratorSvc.loadConfiguration(configuration);
            // Set the outputPath (used to extract tasks from oci bundles, etc.) to the configurator object
            cfg.setOutputPath(outputPath);
        } catch (Exception e) {
            log.error("Error loading configuration", e);
            System.exit(1);
        }

        // Load the default Pipeline configuration using the yaml file loaded from the resources folder
        // TODO: To be documented, reviewed & tested
        if (configuratorSvc.loadDefaultConfiguration(cfg)) {
            // Populate the default Pipeline object that we will use
            // as template tlo add the user's tasks, etc
            configuratorSvc.populateDefaultPipeline();
        } else {
            if (cfg.getProvider().toUpperCase().equals(KONFLUX.name())) {
                log.error("The default configuration file do not exist or cannot not be loaded. This file is mandatory for Konflux");
                System.exit(1);
            } else {
                log.warn("No configuration file found for Tekton");
            }
        }

        if (cfg == null) {
            log.error("Configuration file cannot be empty !");
            System.exit(1);
        }

        if (cfg.getProvider() == null) {
            log.error("Type is missing from the configuration yaml file !");
            System.exit(1);
        } else {
            log.info("#### Type selected: {}", cfg.getProvider().toUpperCase());
        }

        if (cfg.getDomain() == null) {
            cfg.setDomain(Domain.EXAMPLE.name());
        }
        log.info("#### Pipeline domain selected: {}", cfg.getDomain());

        String resourcesPath = Paths.get(cfg.getOutputPath(), cfg.getProvider(), cfg.getDomain()).toString();

        // Use the factory to generate the resources according to the provider and the type
        ConfiguratorSvc.writeYaml(WorkflowResource.create(cfg), resourcesPath);

        // TODO: Is it the best place to create such YAML resources. To be reviewed
        if (cfg.getApplication() != null && cfg.getApplication().isEnable()) {
            ConfiguratorSvc.writeYaml(createApplication(cfg), resourcesPath);
        }

        if (cfg.getComponent() != null && cfg.getComponent().isEnable()) {
            ConfiguratorSvc.writeYaml(createComponent(cfg), resourcesPath);
        }
    } // end of run method
}
