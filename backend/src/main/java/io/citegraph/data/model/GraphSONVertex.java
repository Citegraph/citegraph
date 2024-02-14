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

        @JsonProperty("pagerank")
        private List<PageRank> pagerank;

        public List<Cluster> getCluster() {
            return cluster;
        }

        public void setCluster(List<Cluster> cluster) {
            this.cluster = cluster;
        }

        public List<PageRank> getPagerank() {
            return pagerank;
        }

        public void setPagerank(List<PageRank> pagerank) {
            this.pagerank = pagerank;
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PageRank {

        @JsonProperty("value")
        private InnerValue value;

        // Getters and setters
        public InnerValue getValue() {
            return value;
        }

        public void setValue(InnerValue value) {
            this.value = value;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InnerValue {

        @JsonProperty("@value")
        private Double value;

        // Getters and setters
        public Double getValue() {
            return value;
        }

        public void setValue(Double value) {
            this.value = value;
        }
    }
}
