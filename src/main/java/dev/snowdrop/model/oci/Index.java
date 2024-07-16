package dev.snowdrop.model.oci;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Index {

    @JsonProperty("schemaVersion")
    private int schemaVersion;

    @JsonProperty("manifests")
    private List<ManifestEntry> manifests;

    // Getters and Setters

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public List<ManifestEntry> getManifests() {
        return manifests;
    }

    public void setManifests(List<ManifestEntry> manifests) {
        this.manifests = manifests;
    }

}
