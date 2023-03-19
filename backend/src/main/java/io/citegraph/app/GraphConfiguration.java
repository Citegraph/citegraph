package io.citegraph.app;

import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URL;

@Configuration
public class GraphConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(GraphConfiguration.class);

    public static final String GRAPH_CONFIG_NAME = "janusgraph-cql-lucene.properties";

    @Bean
    public JanusGraph getGraph() {
        LOG.info("Opening graph...");
        URL resource = this.getClass().getClassLoader().getResource(GRAPH_CONFIG_NAME);
        try {
            return JanusGraphFactory.open(resource.toURI().getPath());
        } catch (Exception ex) {
            LOG.error("Fail to open graph", ex);
            throw new RuntimeException(ex);
        }
    }
}
