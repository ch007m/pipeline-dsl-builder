package dev.snowdrop;

import dev.snowdrop.model.Action;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        List<Map<String, Object>> params = new ArrayList<>();

        Map<String, Object> param = new HashMap<>();
        param.put("url","$(params.git-url)");
        param.put("enable",true);
        params.add(param);

        Action action = new Action();
        action.setParams(params);
        action.setRef("bundles://pack-builder:quay.io/ch007m/tekton-bundle:latest");

        yaml.dump(action, writer);

        String expectedYaml = "!!dev.snowdrop.model.Action\n" +
            "params:\n"+
            "- {enable: true, url: $(params.git-url)}\n" +
            "ref: bundles://pack-builder:quay.io/ch007m/tekton-bundle:latest\n" +
            "script: null\n" +
            "scriptFileUrl: null\n";
        assertEquals(expectedYaml, writer.toString());
    }

    @Test
    public void fromActionParamsYamlToAction_Object() {
        String actionParams = """
            ref: "bundles://pack-builder:quay.io/ch007m/tekton-bundle:latest"
            params:
            - url: "$(params.git-url)"
            - enable: true
            - PACK_CMD_FLAGS:
              - "$(params.packCmdBuilderFlags)"
        """;

        Constructor constructor = new Constructor(Action.class, new LoaderOptions());
        Yaml yaml = new Yaml(constructor);
        Action a = yaml.load(actionParams);
        assertNotNull(a);
        assertEquals("bundles://pack-builder:quay.io/ch007m/tekton-bundle:latest",a.getRef());
        assertEquals("$(params.git-url)", a.getParams().get(0).get("url"));
        assertEquals(true,a.getParams().get(1).get("enable"));
        assertEquals(List.of("$(params.packCmdBuilderFlags)"),a.getParams().get(2).get("PACK_CMD_FLAGS"));
    }

}
