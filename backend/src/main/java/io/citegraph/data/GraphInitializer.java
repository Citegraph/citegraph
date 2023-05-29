package io.citegraph.data;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.core.PropertyKey;
import org.janusgraph.core.schema.JanusGraphManagement;

import java.net.URL;

public class GraphInitializer {
    public static void main(String[] args) {
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

        mgmt.makePropertyKey("numOfPaperReferees").dataType(Integer.class).make();
        mgmt.makePropertyKey("numOfPaperReferers").dataType(Integer.class).make();
        mgmt.makePropertyKey("numOfAuthorReferees").dataType(Integer.class).make();
        mgmt.makePropertyKey("numOfAuthorReferers").dataType(Integer.class).make();
        mgmt.makePropertyKey("numOfCoworkers").dataType(Integer.class).make();
        mgmt.makePropertyKey("numOfPapers").dataType(Integer.class).make();
        PropertyKey name = mgmt.makePropertyKey("name").dataType(String.class).make();
        PropertyKey title = mgmt.makePropertyKey("title").dataType(String.class).make();
        PropertyKey refCount = mgmt.makePropertyKey("refCount").dataType(Integer.class).make();
        mgmt.buildIndex("nameIdx", Vertex.class).addKey(name).buildMixedIndex("search");
        mgmt.buildIndex("titleIdx", Vertex.class).addKey(title).buildMixedIndex("search");
        mgmt.makeEdgeLabel("writes").make();
        mgmt.makeEdgeLabel("cites").make();
        mgmt.makeEdgeLabel("refers").make();

        mgmt.commit();
        System.out.println("Schema created");
        graph.close();
        System.out.println("Graph closed, good bye");
    }
}
