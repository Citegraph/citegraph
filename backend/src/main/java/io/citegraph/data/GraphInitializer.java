package io.citegraph.data;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.core.PropertyKey;
import org.janusgraph.core.schema.JanusGraphManagement;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

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
        List<String> stringPropertyKeys = Arrays.asList("org", "venue", "keywords", "field", "docType", "volume",
            "issue", "issn", "isbn", "doi", "abstract");

        for (String key : stringPropertyKeys) {
            if (mgmt.getPropertyKey(key) == null)
                mgmt.makePropertyKey(key).dataType(String.class).make();
        }

        // For internal use only; when we merge two authors (based on name and org), we
        // increment this counter, so that in the future, we have a chance to reconcile
        // wrong merge.
        if (mgmt.getPropertyKey("mergeCount") == null)
            mgmt.makePropertyKey("mergeCount").dataType(Integer.class).make();

        if (mgmt.getPropertyKey("numOfPaperReferees") == null)
            mgmt.makePropertyKey("numOfPaperReferees").dataType(Integer.class).make();
        if (mgmt.getPropertyKey("numOfPaperReferers") == null)
            mgmt.makePropertyKey("numOfPaperReferers").dataType(Integer.class).make();
        if (mgmt.getPropertyKey("numOfAuthorReferees") == null)
            mgmt.makePropertyKey("numOfAuthorReferees").dataType(Integer.class).make();
        if (mgmt.getPropertyKey("numOfAuthorReferers") == null)
            mgmt.makePropertyKey("numOfAuthorReferers").dataType(Integer.class).make();
        if (mgmt.getPropertyKey("numOfCoworkers") == null)
            mgmt.makePropertyKey("numOfCoworkers").dataType(Integer.class).make();
        if (mgmt.getPropertyKey("numOfPapers") == null)
            mgmt.makePropertyKey("numOfPapers").dataType(Integer.class).make();
        if (mgmt.getPropertyKey("pagerank") == null)
            mgmt.makePropertyKey("pagerank").dataType(Double.class).make();
        PropertyKey name = mgmt.getPropertyKey("name");
        if (name == null)
            name = mgmt.makePropertyKey("name").dataType(String.class).make();
        PropertyKey title = mgmt.getPropertyKey("title");
        if (title == null)
            title = mgmt.makePropertyKey("title").dataType(String.class).make();
        PropertyKey refCount = mgmt.getPropertyKey("refCount");
        if (refCount == null)
            mgmt.makePropertyKey("refCount").dataType(Integer.class).make();
        PropertyKey collaborateCount = mgmt.getPropertyKey("collaborateCount");
        if (collaborateCount == null)
            mgmt.makePropertyKey("collaborateCount").dataType(Integer.class).make();
        PropertyKey clusterId = mgmt.getPropertyKey("clusterId");
        if (clusterId == null)
            mgmt.makePropertyKey("clusterId").dataType(String.class).make();
        if (mgmt.getGraphIndex("nameIdx") == null)
            mgmt.buildIndex("nameIdx", Vertex.class).addKey(name).buildMixedIndex("search");
        if (mgmt.getGraphIndex("titleIdx") == null)
            mgmt.buildIndex("titleIdx", Vertex.class).addKey(title).buildMixedIndex("search");
        mgmt.getOrCreateEdgeLabel("writes");
        mgmt.getOrCreateEdgeLabel("cites");
        mgmt.getOrCreateEdgeLabel("refers");
        mgmt.getOrCreateEdgeLabel("collaborates");

        mgmt.commit();
        System.out.println("Schema created");
        graph.close();
        System.out.println("Graph closed, good bye");
    }
}
