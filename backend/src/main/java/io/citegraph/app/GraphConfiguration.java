package io.citegraph.app;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.commons.lang3.tuple.Pair;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import java.net.URL;
import java.util.concurrent.TimeUnit;

@Configuration
public class GraphConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(GraphConfiguration.class);

    public static final String GRAPH_CONFIG_NAME = "janusgraph-cql-lucene.properties";

    private JanusGraph graph;

    @Bean
    public JanusGraph getGraph() {
        LOG.info("Opening graph...");
        URL resource = this.getClass().getClassLoader().getResource(GRAPH_CONFIG_NAME);
        try {
            graph = JanusGraphFactory.open(resource.toURI().getPath());
            return graph;
        } catch (Exception ex) {
            LOG.error("Fail to open graph", ex);
            throw new RuntimeException(ex);
        }
    }

    @Bean
    public Cache<String, String> getAuthorCache() {
        LOG.info("Init author cache...");
        Cache<String, String> cache = Caffeine.newBuilder()
            .maximumSize(10)
            .build();
        return cache;
    }

    @PreDestroy
    public void close() {
        LOG.info("Closing graph...");
        graph.close();
    }
}
