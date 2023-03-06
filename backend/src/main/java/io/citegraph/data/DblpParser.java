package io.citegraph.data;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.citegraph.data.model.Author;
import io.citegraph.data.model.Paper;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.core.JanusGraphVertex;

import java.io.FileInputStream;
import java.net.URL;
import java.util.Scanner;

/**
 * It parses dblp dataset and dumps into graph database
 */
public class DblpParser {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.format("Usage: java %s <DBLP-Citation-network V14 path>\n", DblpParser.class.getName());
            System.exit(0);
        }

        System.out.println("Opening graph...");
        URL resource = GraphInitializer.class.getClassLoader().getResource("janusgraph-berkeleyje-lucene.properties");
        JanusGraph graph = null;
        try {
            graph = JanusGraphFactory.open(resource.toURI().getPath());
        } catch (Exception ex) {
            System.out.println(ex);
            System.exit(0);
        }

        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        final String path = args[0];
        FileInputStream inputStream = null;
        Scanner sc = null;
        long i = 0;
        try {
            inputStream = new FileInputStream(path);
            sc = new Scanner(inputStream, "UTF-8");
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                try {
                    Paper paper = mapper.readValue(line, Paper.class);
                    // create paper vertex first
                    JanusGraphVertex pVertex = graph.addVertex("vid", paper.getId(),
                        "title", paper.getTitle(), "year", paper.getYear(), "type", "paper");
                    System.out.println("Created paper vertex " + paper.getId());
                    // create author vertex if not exists
                    for (Author author : paper.getAuthors()) {
                        Vertex aVertex;
                        GraphTraversal<Vertex, Vertex> traversal = graph.traversal().V().has("vid", author.getId());
                        if (traversal.hasNext()) {
                            aVertex = traversal.next();
                        } else {
                            aVertex = graph.addVertex("vid", author.getId(),
                                "name", author.getName(), "type", "author");
                        }
                        // create edge between author and paper
                        graph.traversal().V(aVertex).addE("writes").to(pVertex).next();
                        System.out.println("Created edge between paper " + paper.getId() + " and author " + author.getId());
                    }
                } catch (Exception ex) {
                    System.out.println("Read line " + i + " fails, skip, line = " + line);
                }
                i++;
                if (i % 100 == 0) {
                    graph.tx().commit();
                    System.out.println("Batch " + (i / 100) + " committed");
                }
            }
            graph.tx().commit();
            System.out.println("Batch " + (i / 100) + " committed");
            // note that Scanner suppresses exceptions
            if (sc.ioException() != null) {
                throw sc.ioException();
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (sc != null) {
                sc.close();
            }
        }
        graph.close();
    }
}


