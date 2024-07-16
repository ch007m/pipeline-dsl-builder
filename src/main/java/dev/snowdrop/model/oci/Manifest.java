package dev.snowdrop.model.oci;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Manifest {

    @JsonProperty("schemaVersion")
    private int schemaVersion;

    @JsonProperty("mediaType")
    private String mediaType;

    @JsonProperty("config")
    private Config config;

    @JsonProperty("layers")
    private List<Layer> layers;

    // Getters and Setters

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public List<Layer> getLayers() {
        return layers;
    }

    public void setLayers(List<Layer> layers) {
        this.layers = layers;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Layer {

        @JsonProperty("mediaType")
        private String mediaType;

        @JsonProperty("size")
        private int size;

        @JsonProperty("digest")
        private String digest;

        @JsonProperty("annotations")
        private Map<String, String> annotations;

        // Getters and Setters

        public String getMediaType() {
            return mediaType;
        }

        public void setMediaType(String mediaType) {
            this.mediaType = mediaType;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public String getDigest() {
            return digest;
        }

        public void setDigest(String digest) {
            this.digest = digest;
        }

        public Map<String, String> getAnnotations() {
            return annotations;
        }

        public void setAnnotations(Map<String, String> annotations) {
            this.annotations = annotations;
        }
    }
}
