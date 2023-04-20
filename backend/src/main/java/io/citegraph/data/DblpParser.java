package io.citegraph.data;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.citegraph.data.model.Author;
import io.citegraph.data.model.Paper;
import org.apache.commons.lang.StringUtils;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.core.JanusGraphTransaction;
import org.janusgraph.core.JanusGraphVertex;
import org.janusgraph.core.attribute.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.citegraph.app.GraphConfiguration.GRAPH_CONFIG_NAME;

/**
 * It parses dblp dataset and dumps into graph database
 */
public class DblpParser {
    private static final Logger LOG = LoggerFactory.getLogger(DblpParser.class);

    /**
     * A naive single-threaded author references loader. It adds an edge from one
     * author to another author if their papers have citation relationships. The
     * edge contains a counter representing the number of times the author cites
     * the other author's work.
     * This method is idempotent.
     *
     * @param paper
     * @param graph
     */
    private static void loadAuthorRefs(final Paper paper, final JanusGraph graph) {
        if (StringUtils.isBlank(paper.getId())) {
            LOG.error("Paper {} does not have id, skip", paper.getTitle());
            return;
        }
        Vertex pVertex = graph.traversal().V(paper.getId()).next();
        List<String> references = paper.getReferences();
        if (references == null) {
            return;
        }
        List<Vertex> authors = graph.traversal().V(pVertex).in("writes").toList();
        List<Vertex> citedPapers = graph.traversal().V(pVertex).out("cites").toList();
        for (Vertex citedP : citedPapers) {
            List<Vertex> refAuthors = graph.traversal().V(citedP).in("writes").toList();
            for (Vertex author : authors) {
                final String name = author.value("name");
                for (Vertex refAuthor : refAuthors) {
                    GraphTraversal<Vertex, Edge> t = graph.traversal().V(author).outE("refers").where(__.inV().is(refAuthor));
                    if (t.hasNext()) {
                        Edge e = t.next();
                        int count = Integer.parseInt(graph.traversal().E(e).properties("refCount").next().value().toString());
                        graph.traversal().E(e).property("refCount", count + 1).next();
                    } else {
                        graph.traversal().V(author).addE("refers").to(refAuthor)
                            .property("refCount", 1)
                            .property("name", name).next();
                    }
                }
            }
        }
    }

    /**
     * A naive single-threaded citations loader. It only touches upon on paper-paper
     * relationships, which does not involve any index lookup, so it is relatively fast.
     *
     * This method is NOT idempotent - if you run the method again, it will create duplicate
     * edges between papers.
     *
     * @param paper
     * @param graph
     */
    private static void loadCitations(final Paper paper, final JanusGraph graph) {
        if (StringUtils.isBlank(paper.getId())) {
            LOG.error("Paper {} does not have id, skip", paper.getTitle());
            return;
        }
        List<String> references = paper.getReferences();
        if (references == null) {
            return;
        }
        JanusGraphTransaction tx = graph.newTransaction();
        Vertex pVertex = tx.traversal().V(paper.getId()).next();
        for (String ref : new HashSet<>(references)) {
            Vertex citedVertex = tx.traversal().V(ref).next();
            tx.traversal().V(pVertex).addE("cites").to(citedVertex).next();
        }
        tx.commit();
    }

    /**
     * A naive single-threaded vertices loader. To finish loading ~5 million papers,
     * it can take a few hours to finish.
     *
     * This method is NOT idempotent - if you run the method again, it will throw an
     * error because of duplicate papers.
     * @param paper
     * @param graph
     */
    private static void loadVertices(final Paper paper, final JanusGraph graph) {
        // create paper vertex first
        if (StringUtils.isBlank(paper.getId())) {
            LOG.error("Paper {} does not have id, skip", paper.getTitle());
            return;
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
                GraphTraversal<Vertex, Vertex> traversal = graph.traversal().V()
                    .has("name", Text.textContains(author.getName())).limit(1);
                if (traversal.hasNext()) {
                    // we match the author with closest name, usually it's good enough
                    aVertex = traversal.next();
                } else {
                    aVertex = graph.addVertex(T.id, UUID.randomUUID().toString(),
                        "name", author.getName(), "type", "author");
                }
            }
            // create edge between author and paper
            graph.traversal().V(aVertex).addE("writes").to(pVertex).next();
            LOG.debug("Created edge between paper " + paper.getId() + " and author " + author.getId());
        }
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.format("Usage: java %s <DBLP-Citation-network V14 path> <mode>\n", DblpParser.class.getName());
            System.err.println("<vertices>: Load all papers and authors, including edges between papers and authors");
            System.err.println("<citations>: Load all citations between papers");
            System.exit(0);
        }

        LOG.info("Opening graph...");
        URL resource = GraphInitializer.class.getClassLoader().getResource(GRAPH_CONFIG_NAME);
        JanusGraph graph = null;
        try {
            graph = JanusGraphFactory.open(resource.toURI().getPath());
        } catch (Exception ex) {
            System.out.println(ex);
            System.exit(0);
        }

        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        final String path = args[0];
        final String mode = args[1];

        boolean multiThreading = mode.equalsIgnoreCase("citations");
        // create a thread pool for data loading
        ExecutorService executor = multiThreading
            ? Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
            : null;

        FileInputStream inputStream = null;
        Scanner sc = null;
        long i = 0;
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
                    if (mode.equalsIgnoreCase("vertices")) {
                        loadVertices(paper, graph);
                    } else if (mode.equalsIgnoreCase("citations")) {
                        JanusGraph finalGraph = graph;
                        executor.submit(() -> {
                            loadCitations(paper, finalGraph);
                        });
                    } else if (mode.equalsIgnoreCase("references")) {
                        LOG.error("Mode references deprecated, please use AuthorRefEdgeLoader instead");
                        graph.close();
                        System.exit(0);
                    } else {
                        LOG.error("Unknown mode {}, must be either vertices or citations", mode);
                        graph.close();
                        System.exit(0);
                    }
                    i++;
                    if (i % 100 == 0) {
                        if (!multiThreading) {
                            graph.tx().commit();
                        }
                        LOG.info("Batch " + (i / 100) + " committed, failed count = " + failedCount);
                    }
                } catch (Exception ex) {
                    LOG.error("Fail to write to graph", ex);
                    failedCount++;
                }
            }
            if (!multiThreading) {
                graph.tx().commit();
                LOG.info("Batch " + (i / 100) + " committed, failed count = " + failedCount);
            }
            // note that Scanner suppresses exceptions
            if (sc.ioException() != null) {
                throw sc.ioException();
            }
        } finally {
            if (executor != null) {
                executor.shutdown();
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            }
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


