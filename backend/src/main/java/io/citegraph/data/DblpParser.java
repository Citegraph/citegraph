package io.citegraph.data;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.citegraph.data.model.Author;
import io.citegraph.data.model.Paper;
import org.apache.commons.lang.StringUtils;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.core.JanusGraphVertex;
import org.janusgraph.core.attribute.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.net.URL;
import java.util.Scanner;
import java.util.UUID;

/**
 * It parses dblp dataset and dumps into graph database
 */
public class DblpParser {

    private static final Logger LOG = LoggerFactory.getLogger(DblpParser.class);
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.format("Usage: java %s <DBLP-Citation-network V14 path>\n", DblpParser.class.getName());
            System.exit(0);
        }

        LOG.info("Opening graph...");
        URL resource = GraphInitializer.class.getClassLoader().getResource("janusgraph-cql-lucene.properties");
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
        long authorWithoutId = 0;
        long failedCount = 0;
        try {
            inputStream = new FileInputStream(path);
            sc = new Scanner(inputStream, "UTF-8");
            while (sc.hasNextLine()) {
                String line = sc.nextLine().trim();
                if (line.endsWith(",")) {
                    line = line.substring(0, line.length() - 1);
                }
                Paper paper;
                try {
                    paper = mapper.readValue(line, Paper.class);
                } catch (Exception ex) {
                    LOG.info("Read line " + i + " fails, skip, line = " + line, ex);
                    failedCount++;
                    continue;
                }
                try {
                    // create paper vertex first
                    if (StringUtils.isBlank(paper.getId())) {
                        LOG.error("Paper {} does not have id, skip", paper.getTitle());
                        continue;
                    }
                    JanusGraphVertex pVertex = graph.addVertex(T.id, paper.getId(),
                        "title", paper.getTitle(), "year", paper.getYear(), "type", "paper");
                    // create author vertex if not exists
                    for (Author author : paper.getAuthors()) {
                        if (StringUtils.isBlank(author.getName())) continue;
                        Vertex aVertex = null;
                        if (StringUtils.isNotBlank(author.getId())) {
                            GraphTraversal<Vertex, Vertex> traversal = graph.traversal().V(author.getId());
                            if (traversal.hasNext()) {
                                aVertex = traversal.next();
                            } else {
                                aVertex = graph.addVertex(T.id, author.getId(),
                                    "name", author.getName(), "type", "author");
                            }
                        } else {
                            LOG.debug("Author {} does not have id", author.getName());
                            authorWithoutId++;
                            GraphTraversal<Vertex, Vertex> traversal = graph.traversal().V()
                                .has("name", Text.textContains(author.getName()));
                            if (traversal.hasNext()) {
                                // we match the author with closest name, usually it's good enough
                                aVertex = traversal.next();
                            } else {
                                aVertex = graph.addVertex(T.id, UUID.randomUUID().toString().replace('-', '_'),
                                    "name", author.getName(), "type", "author");
                            }
                        }
                        // create edge between author and paper
                        graph.traversal().V(aVertex).addE("writes").to(pVertex).next();
                        LOG.debug("Created edge between paper " + paper.getId() + " and author " + author.getId());
                    }
                    i++;
                    if (i % 100 == 0) {
                        graph.tx().commit();
                        LOG.info("Batch " + (i / 100) + " committed, failed count = " + failedCount + " author without Id = " + authorWithoutId);
                    }
                } catch (Exception ex) {
                    LOG.error("Fail to write to graph", ex);
                    failedCount++;
                }
            }
            graph.tx().commit();
            LOG.info("Batch " + (i / 100) + " committed, failed count = " + failedCount + " author without Id = " + authorWithoutId);
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


