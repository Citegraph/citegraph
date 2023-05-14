package io.citegraph.app;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.MessageSerializer;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.driver.ser.GraphBinaryMessageSerializerV1;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.janusgraph.graphdb.tinkerpop.JanusGraphIoRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.apache.tinkerpop.gremlin.driver.ser.AbstractMessageSerializer.TOKEN_IO_REGISTRIES;

@Configuration
public class GraphConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(GraphConfiguration.class);

    public static final String GRAPH_CONFIG_NAME = "janusgraph-cql-lucene.properties";

    private GraphTraversalSource g;

    private MessageSerializer createGraphBinaryMessageSerializerV1() {
        final GraphBinaryMessageSerializerV1 serializer = new GraphBinaryMessageSerializerV1();
        final Map<String, Object> config = new HashMap<>();
        config.put(TOKEN_IO_REGISTRIES, Collections.singletonList(JanusGraphIoRegistry.class.getName()));
        serializer.configure(config, Collections.emptyMap());
        return serializer;
    }

    @Bean
    public GraphTraversalSource getGraph() {
        LOG.info("Opening graph...");
        try {
            Cluster cluster = Cluster.build("localhost")
                .port(8182)
                .serializer(createGraphBinaryMessageSerializerV1())
                .create();

            g = AnonymousTraversalSource.traversal()
                .withRemote(DriverRemoteConnection.using(cluster, "g"));
            return g;
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
    public void close() throws Exception {
        LOG.info("Closing graph...");
        g.close();
    }
}
