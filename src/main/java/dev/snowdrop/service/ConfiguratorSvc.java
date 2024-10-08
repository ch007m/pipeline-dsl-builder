package dev.snowdrop.service;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dev.snowdrop.factory.WorkflowResource;
import dev.snowdrop.model.Configurator;
import io.fabric8.kubernetes.api.model.HasMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.LITERAL_BLOCK_STYLE;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.WRITE_DOC_START_MARKER;
import static dev.snowdrop.service.FileUtilSvc.readFileFromResources;

@Slf4j
@ApplicationScoped
@Setter
@Getter
public class ConfiguratorSvc {

    private static ConfiguratorSvc instance;
    public Configurator defaultConfigurator;
    public HasMetadata defaultPipeline;

    public ConfiguratorSvc() {}

    // Public method to provide the singleton instance
    public static ConfiguratorSvc getInstance() {
        if (instance == null) {
            instance = new ConfiguratorSvc();
        }
        return instance;
    }

    // TODO: To be reviewed as code will fail when there is no default config file, which is certainly an option
    public Boolean loadDefaultConfiguration(Configurator cfg) {
        if (defaultConfigurator == null) {
            String cfgFileName = String.format("%s-default-pipeline.yaml",cfg.getProvider());
            try {
                String configYaml = readFileFromResources("dev/snowdrop/configuration/" + cfgFileName);
                log.info("#### Default configuration loaded: {}", "dev/snowdrop/configuration/" + cfgFileName);
                defaultConfigurator = loadConfiguration(configYaml);
            } catch (Exception e) {
                log.warn("Default configuration file {} don't exist for or cannot be loaded :",cfgFileName);
                return false;
            }
            defaultConfigurator.setOutputPath(cfg.getOutputPath());
        }
        return true;
    }

    public void populateDefaultPipeline() {
        if (defaultPipeline == null) {
            // Let's create the default Pipeline
            defaultPipeline = WorkflowResource.create(defaultConfigurator);
        }
    }

    public Configurator loadConfiguration(String input) throws Exception {
        Configurator configurator = new Configurator();
        try {
            configurator = loadYaml(input);
        } catch (Exception e) {
            throw e;
        }
        return configurator;
    }

    public static Configurator loadYaml(Object input) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            if (input instanceof String str) {

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
        } catch (JsonMappingException ex) {
            throw ex;
        } catch (IOException e) {
            throw e;
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
                    .disable(WRITE_DOC_START_MARKER)
                    .enable(LITERAL_BLOCK_STYLE)
            );
            mapper.writeValue(pathToYaml.toFile(), resource);

            log.info("Path of the resource generated: {}", pathToYaml);
            log.debug("Generated YAML: \n{}", yamlFilePath);

        } catch (FileAlreadyExistsException e) {
            // TODO
            log.warn("File already exists: " + yamlFilePath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
