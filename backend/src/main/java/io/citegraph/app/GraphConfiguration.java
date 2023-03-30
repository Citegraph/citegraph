package io.citegraph.app;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.citegraph.data.GraphInitializer;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

@Configuration
public class GraphConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(GraphConfiguration.class);

    public static final String GRAPH_CONFIG_NAME = "janusgraph-cql-lucene.properties";

    private JanusGraph graph;

    @Bean
    public JanusGraph getGraph() {
        LOG.info("Opening graph...");
        URL res = this.getClass().getClassLoader().getResource(GRAPH_CONFIG_NAME);
        File file;
        if (res.getProtocol().equals("jar")) {
            try {
                InputStream input = GraphInitializer.class.getClassLoader().getResourceAsStream(GRAPH_CONFIG_NAME);
                file = File.createTempFile("tempfile", ".tmp");
                OutputStream out = new FileOutputStream(file);
                int read;
                byte[] bytes = new byte[1024];

                while ((read = input.read(bytes)) != -1) {
                    out.write(bytes, 0, read);
                }
                out.close();
                file.deleteOnExit();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            //this will probably work in your IDE, but not from a JAR
            file = new File(res.getFile());
        }

        if (file != null && !file.exists()) {
            throw new RuntimeException("Error: File " + file + " not found!");
        }
        try {
            graph = JanusGraphFactory.open(file.getAbsolutePath());
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
