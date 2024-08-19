package dev.snowdrop;

import dev.snowdrop.model.Configurator;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.wildfly.common.Assert.assertNotNull;

public class ConfiguratorTests {

    @Test
    public void fromYamlToObject() {
        String yamlStr = """
            !dev.snowdrop.Configurator
            type: konflux
            application:
              enable: false
            component:
              enable: false
            domain: build

            repository:
              url: https://github.com/ch007m/new-quarkus-app-1
              dockerfilePath: src/main/docker/Dockerfile.jvm
            """;

        Yaml yaml = new Yaml(new CustomConfigurator(new LoaderOptions()));
        Configurator cfg = yaml.load(yamlStr);

        assert cfg != null;
        assertNull(cfg.getRepository());
    }

}