package io.citegraph.data;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.core.PropertyKey;
import org.janusgraph.core.schema.JanusGraphManagement;

import java.net.URL;

public class GraphInitializer {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.format("Usage: java %s <dataset-name>\n", GraphInitializer.class.getName());
            System.exit(0);
        }

        System.out.println("Opening graph...");
        URL resource = GraphInitializer.class.getClassLoader().getResource("janusgraph-cql-lucene.properties");
        JanusGraph graph = null;
        try {
            graph = JanusGraphFactory.open(resource.toURI().getPath());
        } catch (Exception ex) {
            System.out.println(ex);
            System.exit(0);
        }
        JanusGraphManagement mgmt = graph.openManagement();
        System.out.println("Current schema is...");
        mgmt.printSchema();

        final String dataset = args[0];
        System.out.println("Creating schema for dataset " + dataset + "...");

        if ("dblp".equalsIgnoreCase(dataset)) {
            PropertyKey name = mgmt.makePropertyKey("name").dataType(String.class).make();
            mgmt.buildIndex("nameIdx", Vertex.class).addKey(name).buildMixedIndex("search");
        }

        mgmt.commit();
        System.out.println("Schema created");
        graph.close();
        System.out.println("Graph closed, good bye");
    }
}
