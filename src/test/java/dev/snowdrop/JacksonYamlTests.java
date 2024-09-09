package dev.snowdrop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import dev.snowdrop.model.Action;
import dev.snowdrop.model.Configurator;
import dev.snowdrop.model.Repository;
import dev.snowdrop.model.konflux.Application;
import dev.snowdrop.model.konflux.Component;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JacksonYamlTests {
    @Test
    public void fromYamlToObject() throws JsonProcessingException {
        String yamlStr = """
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

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Configurator cfg = mapper.readValue(yamlStr, Configurator.class);

        assert cfg != null;
        assertEquals("https://github.com/ch007m/new-quarkus-app-1", cfg.getRepository().getUrl());
    }

    @Test
    public void fromObjectToYaml() throws JsonProcessingException {
        String yamlStr = """
            type: "konflux"
            domain: "build"
            namespace: null
            job: null
            repository:
              url: "http://github.com/ch007m/new-quarkus-app-1"
              context: "."
              revision: "main"
              dockerfileUrl: null
              dockerfilePath: null
            outputPath: null
            application:
              enable: false
              name: null
            component:
              enable: false
              name: null
            bundles: null  
            """;

        Application app = new Application();
        app.setEnable(false);

        Component comp = new Component();
        comp.setEnable(false);

        Repository repo = new Repository();
        repo.setUrl("http://github.com/ch007m/new-quarkus-app-1");

        Configurator cfg = new Configurator();
        cfg.setType("konflux");
        cfg.setDomain("build");

        cfg.setApplication(app);
        cfg.setComponent(comp);
        cfg.setRepository(repo);

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        String result = mapper.writeValueAsString(cfg);

        assert result != null;
        assertEquals(yamlStr, result);
    }

    @Test
    public void checkYAMLBlockLiteral() throws JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper(
            new YAMLFactory()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)  // Disable "---"
                .enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)      // Use block style for multi-line strings
        );

        String bashScript = """
            #!/usr/bin/env bash
            set -e
            mkdir -p ~/.ssh
            if [ -e "/ssh/error" ]; then
              #no server could be provisioned
              cat /ssh/error
              exit 1
            fi
            """;

        String expectedYaml = """
            id: 1
            name: null
            ref: null
            script: |
              #!/usr/bin/env bash
              set -e
              mkdir -p ~/.ssh
              if [ -e "/ssh/error" ]; then
                #no server could be provisioned
                cat /ssh/error
                exit 1
              fi
            scriptFileUrl: null
            runAfter: null
            image: null
            params: null
            workspaces: null
            volumes: null
            args: null
            when: null
            results: null
            finally: false  
            """;

        Action myAction = new Action();
        myAction.setScript(bashScript);

        String yamlOutput = mapper.writeValueAsString(myAction);
        assertEquals(expectedYaml,yamlOutput);
    }
}