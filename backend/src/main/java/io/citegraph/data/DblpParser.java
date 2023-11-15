package io.citegraph.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.citegraph.data.model.Author;
import io.citegraph.data.model.FieldOfStudy;
import io.citegraph.data.model.Paper;
import org.apache.commons.lang.StringUtils;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.core.JanusGraphTransaction;
import org.janusgraph.core.JanusGraphVertex;
import org.janusgraph.core.attribute.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.citegraph.app.GraphConfiguration.GRAPH_CONFIG_NAME;

/**
 * It parses dblp dataset and dumps into graph database
 */
public class DblpParser {
    private static final Logger LOG = LoggerFactory.getLogger(DblpParser.class);
    private static final String GPT_URL = "https://api.openai.com/v1/chat/completions";
    private static final String API_KEY = System.getenv("OPENAI_KEY");
    private static final String GPT_MODEL = "gpt-3.5-turbo";
    private static final Map<String, Boolean> GPT_QA_CACHE = new HashMap<>();
    private static BufferedWriter bufferedWriter;

    private static String getString(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return value;
    }

    private static boolean sameOrg(String org1, String org2) {
        if (Objects.equals(org1.toLowerCase().replaceAll("\\s+",""), org2.toLowerCase().replaceAll("\\s+",""))) {
            return true;
        }
        if (org1.compareTo(org2) < 0) {
            return sameOrg(org2, org1);
        }
        String key = org1 + "\t" + org2;
        if (GPT_QA_CACHE.containsKey(key)) {
            return GPT_QA_CACHE.get(key);
        }
        try {
            // Set timeouts in milliseconds
            int connectionTimeout = 10000; // e.g., 10 seconds
            int readTimeout = 15000; // e.g., 15 seconds
            int maxRetries = 3; // Maximum number of retries
            int retryDelay = 5000; // Delay between retries (5 seconds)

            URL obj = new URL(GPT_URL);
            StringBuffer response = new StringBuffer();
            boolean success = false;

            for (int attempt = 0; attempt < maxRetries && !success; attempt++) {
                try {
                    HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Authorization", "Bearer " + API_KEY);
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setConnectTimeout(connectionTimeout);
                    connection.setReadTimeout(readTimeout);

                    String prompt = "Do " + org1 + " and " + org2 + " belong to the same or the same larger institute? Answer yes or no without explanation.";

                    // The request body
                    String body = "{\"model\": \"" + GPT_MODEL + "\", \"messages\": [{\"role\": \"user\", \"content\": \"" + prompt + "\"}]}";
                    connection.setDoOutput(true);
                    OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                    writer.write(body);
                    writer.flush();
                    writer.close();

                    // Reading response
                    BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    br.close();

                    success = true; // If the request was successful
                } catch (IOException e) {
                    LOG.error("Attempt {} failed: {}", attempt + 1, e.getMessage());
                    if (attempt < maxRetries - 1) {
                        // Wait for retryDelay milliseconds before retrying
                        try {
                            Thread.sleep(retryDelay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Thread interrupted", ie);
                        }
                    }
                }
            }

            if (!success) {
                throw new RuntimeException("Failed to connect to the API after " + maxRetries + " attempts.");
            }
            LOG.info("Response from OpenAI is {}, org1 = {}, org2 = {}", response, org1, org2);
            boolean ans = response.toString().toLowerCase().contains("yes");
            GPT_QA_CACHE.put(key, ans);
            bufferedWriter.write(org1 + "\t" + org2 + "\t" + ans);
            bufferedWriter.newLine();
            bufferedWriter.flush();
            // sleep 2 seconds to avoid calling GPT AI too frequently
            Thread.sleep(2000);
            return ans;
        } catch (Exception e) {
            LOG.error("Fail to call OpenAI API", e);
            return false;
        }
    }

    private static boolean sameAuthor(Vertex a, Author b) {
        if (!a.property("name").isPresent()) {
            LOG.error("Vertex {} does not have name property, data corrupted", a.id());
            return false;
        }
        if (!a.property("name").value().equals(b.getName())) {
            return false;
        }
        // existing vertex has no org info
        if (!a.property("org").isPresent()) {
            return StringUtils.isBlank(b.getOrg());
        }
        // existing vertex has org info, but incoming author doesn't
        if (StringUtils.isBlank(b.getOrg())) {
            return false;
        }
        // both have org info
        String existingOrg = (String) a.property("org").value();
        String org = b.getOrg();
        return sameOrg(existingOrg, org);
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
    private static void loadVertices(final Paper paper, final JanusGraph graph) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        // create paper vertex first
        if (StringUtils.isBlank(paper.getId())) {
            LOG.info("Paper {} does not have id", paper);
            System.exit(1);
        }
        JanusGraphVertex pVertex = graph.addVertex(
            T.id, paper.getId(),
            "title", paper.getTitle(),
            "year", paper.getYear(),
            "type", "paper",
            "venue", paper.getVenue() != null ? getString(paper.getVenue().getRaw()) : null,
            "keywords", paper.getKeywords() != null ? String.join(",", paper.getKeywords()): null,
            "field", paper.getField() != null ? paper.getField().stream().map(FieldOfStudy::getName).collect(Collectors.joining(",")) : null,
            "docType", getString(paper.getDocType()),
            "volume", getString(paper.getVolume()),
            "issue", getString(paper.getIssue()),
            "issn", getString(paper.getIssn()),
            "isbn", getString(paper.getIsbn()),
            "doi", getString(paper.getDoi()),
            "abstract", getString(paper.getPaperAbstract()));
        // create author vertex if not exists
        int order = 0;
        for (Author author : paper.getAuthors()) {
            order++;
            if (StringUtils.isBlank(author.getName())) continue;
            Vertex aVertex = null;
            if (StringUtils.isNotBlank(author.getId())) {
                GraphTraversal<Vertex, Vertex> traversal = graph.traversal().V(author.getId());
                if (traversal.hasNext()) {
                    aVertex = traversal.next();
                } else {
                    aVertex = graph.addVertex(
                        T.id, author.getId(),
                        "name", author.getName(),
                        "type", "author",
                        "org", getString(author.getOrg()));
                }
            } else {
                 GraphTraversal<Vertex, Vertex> traversal = graph.traversal().V()
                     .has("name", Text.textContains(author.getName()));
                if (!traversal.hasNext()) {
                    aVertex = graph.addVertex(T.id, UUID.randomUUID().toString(),
                        "name", author.getName(), "type", "author", "org", getString(author.getOrg()));
                } else {
                    // find the best match
                    boolean found = false;
                    while (traversal.hasNext()) {
                        Vertex candidate = traversal.next();
                        if (sameAuthor(candidate, author)) {
                            found = true;
                            aVertex = candidate;
                            int mergeCount = aVertex.property("mergeCount").isPresent()
                                ? (int) aVertex.property("mergeCount").value()
                                : 0;
                            mergeCount++;
                            aVertex.property("mergeCount", mergeCount);
                            // LOG.info("Merged author {}, merge count = {}", mapper.writeValueAsString(author), mergeCount);
                            break;
                        }
                    }
                    if (!found) {
                        aVertex = graph.addVertex(T.id, UUID.randomUUID().toString(),
                            "name", author.getName(), "type", "author", "org", getString(author.getOrg()));
                    }
                }
            }
            // create edge between author and paper
            graph.traversal().V(aVertex).addE("writes").property("authorOrder", order).to(pVertex).next();
            LOG.debug("Created edge between paper " + paper.getId() + " and author " + author.getId());
        }
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.format("Usage: java %s <DBLP-Citation-network V14 path> <Org-Match-Result tsv path> <mode>\n", DblpParser.class.getName());
            System.err.println("<mode: vertices>: Load all papers and authors, including edges between papers and authors");
            System.err.println("<mode: citations>: Load all citations between papers");
            System.exit(0);
        }

        final String path = args[0];
        final String orgTsv = args[1];
        final String mode = args[2];

        // load existing org comparison records to cache
        try (BufferedReader br = new BufferedReader(new FileReader(orgTsv))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Use a tab as delimiter
                String[] values = line.split("\t");
                if (values.length != 3) {
                    LOG.error("Reading invalid line with {} entries, expect 3: {}", values.length, line);
                    continue;
                }

                String key = values[0] + "\t" + values[1];
                boolean areSame = Boolean.parseBoolean(values[2]);
                GPT_QA_CACHE.put(key, areSame);
                LOG.info("Parsed input {}: org1 = {}, org2 = {}", areSame, values[0], values[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        LOG.info("Loaded {} entries to cache", GPT_QA_CACHE.size());
        LOG.info("================================================");
        LOG.info("                     START                      ");
        LOG.info("================================================");

        bufferedWriter = new BufferedWriter(new FileWriter(orgTsv, true));

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
                if (i == 0) {
                    i++;
                    continue;
                }
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
        bufferedWriter.close();
    }
}


