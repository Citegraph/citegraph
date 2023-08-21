package io.citegraph.data.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Used to laod GraphSON vertex
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphSONVertex {
    @JsonProperty("id")
    private String id;

    @JsonProperty("properties")
    private Properties properties;

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Properties {

        @JsonProperty("cluster")
        private List<Cluster> cluster;

        public List<Cluster> getCluster() {
            return cluster;
        }

        public void setCluster(List<Cluster> cluster) {
            this.cluster = cluster;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Cluster {

        @JsonProperty("value")
        private String value;

        // Getters and setters
        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
