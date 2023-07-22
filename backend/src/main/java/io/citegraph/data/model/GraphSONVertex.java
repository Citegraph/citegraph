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

        @JsonProperty("gremlin.pageRankVertexProgram.pageRank")
        private List<PageRank> pageRank;

        // Getters and setters
        public List<PageRank> getPageRank() {
            return pageRank;
        }

        public void setPageRank(List<PageRank> pageRank) {
            this.pageRank = pageRank;
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
        private double value;

        // Getters and setters
        public double getValue() {
            return value;
        }

        public void setValue(double value) {
            this.value = value;
        }
    }
}
