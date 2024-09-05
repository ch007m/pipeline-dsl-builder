package dev.snowdrop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import dev.snowdrop.command.BuilderCommand;
import dev.snowdrop.model.Configurator;
import io.fabric8.kubernetes.api.model.HasMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import static dev.snowdrop.service.FileUtilSvc.readFileFromResources;

@ApplicationScoped
public class ConfiguratorSvc {

    private static final Logger logger = LoggerFactory.getLogger(BuilderCommand.class);
    private static ConfiguratorSvc instance;
    public Configurator defaultConfigurator;

    public ConfiguratorSvc() {}

    // Public method to provide the singleton instance
    public static ConfiguratorSvc getInstance() {
        if (instance == null) {
            instance = new ConfiguratorSvc();
        }
        return instance;
    }

    public void loadDefaultConfiguration(String cfgFileName) {
        if (defaultConfigurator == null) {
            String configYaml = readFileFromResources("dev/snowdrop/configuration/" + cfgFileName);
            logger.info("#### Default configuration loaded: {}", "dev/snowdrop/configuration/" + cfgFileName);
            defaultConfigurator = loadConfiguration(configYaml);
        }
    }

    public Configurator loadConfiguration(String input) {
        Configurator configurator = new Configurator();
        try {
            configurator = loadYaml(input);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return configurator;
    }

    public static Configurator loadYaml(Object input) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            if (input instanceof String) {
                String str = (String) input;

                // Check if the string represents a file path or YAML content
                File file = new File(str);
                if (file.exists()) {
                    // The string is a file path
                    return mapper.readValue(file, Configurator.class);
                } else {
                    // The string is YAML content
                    return mapper.readValue(str, Configurator.class);
                }
            } else {
                throw new IllegalArgumentException("Input must be a valid file path or YAML content as String.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void writeYaml(HasMetadata resource, String outputPath) {
        Path yamlFilePath = null;
        try {
            String fileName = resource.getKind().toLowerCase() + "-" + resource.getMetadata().getName();
            yamlFilePath = Paths.get(outputPath, fileName + ".yaml");

            Files.createDirectories(Paths.get(outputPath));
            Path pathToYaml = Files.createFile(yamlFilePath);

            // Convert the resource to YAML
            ObjectMapper mapper = new ObjectMapper(
                new YAMLFactory()
                    .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                    .enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)
            );
            mapper.writeValue(pathToYaml.toFile(), resource);

            logger.info("Path of the resource generated: {}", pathToYaml);
            logger.debug("Generated YAML: \n{}", yamlFilePath);

        } catch (FileAlreadyExistsException e) {
            // TODO
            logger.warn("File already exists: " + yamlFilePath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
