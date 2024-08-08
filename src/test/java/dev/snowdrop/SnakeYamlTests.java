package dev.snowdrop;

import dev.snowdrop.model.Action;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SnakeYamlTests {

    Yaml yaml;
    StringWriter writer;

    @BeforeEach
    public void init() {
        yaml = new Yaml();
        writer = new StringWriter();
    }

    @Test
    public void paramsWithDifferentObjects() {
        HashMap<String, Object> params = new HashMap<>();
        params.put("url","$(params.git-url)");
        params.put("enable",true);

        Action action = new Action();
        action.setParams(params);

        yaml.dump(action, writer);

        String expectedYaml = "!!dev.snowdrop.model.Action\n" +
            "params: {enable: true, url: $(params.git-url)}\n" +
            "ref: null\n" +
            "script: null\n" +
            "scriptFileUrl: null\n";
        assertEquals(expectedYaml, writer.toString());
    }

}
