package dev.snowdrop.snakeyaml;

import dev.snowdrop.model.Action;
import dev.snowdrop.model.Job;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SnakeYamlTests {

    Yaml yaml;
    StringWriter writer;

    @Test
    public void paramsWithDifferentObjects() {
        List<Map<String, Object>> params = new ArrayList<>();

        Map<String, Object> param = new HashMap<>();
        param.put("url","$(params.git-url)");
        param.put("enable",true);
        params.add(param);

        Action action = new Action();
        action.setParams(params);
        action.setName("git-clone");
        action.setRef("bundle://quay.io/ch007m/tekton-bundle:latest");

        StringWriter writer = new StringWriter();
        Yaml yaml = new Yaml();
        yaml.dump(action, writer);

        String expectedYaml = "!!dev.snowdrop.model.Action\n" +
            "id: 2\n" +
            "name: git-clone\n" +
            "params:\n"+
            "- {enable: true, url: $(params.git-url)}\n" +
            "ref: bundle://quay.io/ch007m/tekton-bundle:latest\n" +
            "runAfter: null\n" +
            "script: null\n" +
            "scriptFileUrl: null\n" +
            "when: null\n" +
            "workspaces: null\n";
        assertEquals(expectedYaml, writer.toString());
    }

    @Test
    public void fromYamlToObject() {
        String yamlStr = """
          name: action-with-workspaces
          workspaces:
            - name: source-dir
              volumeClaimTemplate:
                storage: 1Gi
                accessMode: ReadWriteOnce
            - name: data-store
              volumeSources:
                - secret: pack-config-toml
                - secret: quay-creds
          """;

        Constructor constructor = new Constructor(Job.class, new LoaderOptions());
        Yaml yaml = new Yaml(constructor);
        Job j = yaml.load(yamlStr);

        assertNotNull(j);

        assertEquals("source-dir",j.getWorkspaces().get(0).getName());
        assertEquals("ReadWriteOnce",j.getWorkspaces().get(0).getVolumeClaimTemplate().getAccessMode());
        assertEquals("1Gi",j.getWorkspaces().get(0).getVolumeClaimTemplate().getStorage());

        assertEquals("pack-config-toml",j.getWorkspaces().get(1).getVolumeSources().get(0).getSecret());
        assertEquals("quay-creds",j.getWorkspaces().get(1).getVolumeSources().get(1).getSecret());
    }

    @Test
    public void fromActionParamsYamlToAction_Object() {
        String yamlStr = """
            name: git-clone
            ref: "bundle://pack-builder:quay.io/ch007m/tekton-bundle:latest"
            params:
            - url: "$(params.git-url)"
            - enable: true
            - PACK_CMD_FLAGS:
              - "$(params.packCmdBuilderFlags)"
        """;

        Constructor constructor = new Constructor(Action.class, new LoaderOptions());
        Yaml yaml = new Yaml(constructor);
        Action a = yaml.load(yamlStr);

        assertNotNull(a);
        assertEquals("bundle://pack-builder:quay.io/ch007m/tekton-bundle:latest",a.getRef());
        assertEquals("git-clone",a.getName());
        assertEquals("$(params.git-url)", a.getParams().get(0).get("url"));
        assertEquals(true,a.getParams().get(1).get("enable"));
        assertEquals(List.of("$(params.packCmdBuilderFlags)"),a.getParams().get(2).get("PACK_CMD_FLAGS"));
    }

    @Test
    public void fromJobYamlToJobObject() {
        String yamlStr = """
          resourceType: PipelineRun
          name: pack-builder-push
          description: "This Pipeline builds a builder image using the pack CLI."
          params:
          - debug: true
          - git-url: "https://github.com/redhat-buildpacks/ubi-image-builder.git"
          - cmdArgs:
            - -v
            - --publish
          actions:
            - name: git-clone
              ref: bundle://quay.io/konflux-ci/tekton-catalog/task-git-clone:0.1@sha256:de0ca8872c791944c479231e21d68379b54877aaf42e5f766ef4a8728970f8b3
              params:
                - url: "$(params.git-url)"
                - subdirectory: "."
            - name: tool
              ref: bundle://quay.io/ch007m/tekton-bundle:latest@sha256:af13b94347457df001742f8449de9edb381e90b0d174da598ddd15cf493e340f
              params:
                - CMD_ARGS:
                  - "$(params.cmdArgs)"
        """;

        Constructor constructor = new Constructor(Job.class, new LoaderOptions());
        Yaml yaml = new Yaml(constructor);
        Job j = yaml.load(yamlStr);

        assertNotNull(j);

        assertEquals(List.of("-v", "--publish"),j.getParams().get(2).get("cmdArgs"));
        assertEquals(2,j.getActions().size());
        assertEquals(List.of("$(params.cmdArgs)"),j.getActions().get(1).getParams().get(0).get("CMD_ARGS"));
    }

}
