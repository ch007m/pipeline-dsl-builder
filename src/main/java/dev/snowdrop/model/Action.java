package dev.snowdrop.model;

import java.util.List;
import java.util.Map;

public class Action {
    private String name;
    private String ref;
    private String script;
    private String scriptFileUrl;
    private List<Map<String, Object>> params;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Map<String, Object>> getParams() {
        return params;
    }

    public void setParams(List<Map<String, Object>> params) {
        this.params = params;
    }

    public String getScriptFileUrl() {
        return scriptFileUrl;
    }

    public void setScriptFileUrl(String scriptFileUrl) {
        this.scriptFileUrl = scriptFileUrl;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }
}
