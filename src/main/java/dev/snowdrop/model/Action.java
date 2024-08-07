package dev.snowdrop.model;

public class Action {
    private String ref;
    private String script;
    private String scriptFileUrl;

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
