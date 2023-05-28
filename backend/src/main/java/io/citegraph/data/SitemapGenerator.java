package io.citegraph.data;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

public class SitemapGenerator {
    public static void main(String[] args) throws IOException {
        System.out.println("Opening graph...");
        URL resource = GraphInitializer.class.getClassLoader().getResource("janusgraph-cql-lucene.properties");
        JanusGraph graph = null;
        try {
            graph = JanusGraphFactory.open(resource.toURI().getPath());
        } catch (Exception ex) {
            System.out.println(ex);
            System.exit(0);
        }
        GraphTraversal<Vertex, Vertex> g = graph.traversal().V();
        int count = 0;
        int fileCount = 0;
        FileWriter fileWriter = null;
        while (g.hasNext()) {
            try {
                Vertex v = g.next();
                StringBuilder builder = new StringBuilder("http://www.citegraph.io/");

                if (v.value("type").equals("author")) {
                    builder.append("author/");
                } else if (v.value("type").equals("paper")) {
                    builder.append("paper/");
                }
                builder.append(v.id());
                if (count == 0) {
                    if (fileWriter != null) {
                        fileWriter.close();
                    }
                    // create new sitemap file
                    fileCount++;
                    fileWriter = new FileWriter("sitemap" + fileCount + ".txt");
                }
                fileWriter.write(builder + "\n");
                count = (count + 1) % 50000; // individual sitemap file limit
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        graph.close();
    }
}
