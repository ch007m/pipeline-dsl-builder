package dev.snowdrop.model;

import java.util.HashMap;
import java.util.List;

public class Action {
    private String ref;
    private String script;
    private String scriptFileUrl;
    private HashMap<String,Object> params;

    public HashMap<String, ?> getParams() {
        return params;
    }

    public void setParams(HashMap<String, Object> params) {
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
