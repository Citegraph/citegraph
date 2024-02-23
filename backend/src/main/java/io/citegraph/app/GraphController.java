package io.citegraph.app;

import com.github.benmanes.caffeine.cache.Cache;
import io.citegraph.app.model.AuthorResponse;
import io.citegraph.app.model.CitationResponse;
import io.citegraph.app.model.CollaborationResponse;
import io.citegraph.app.model.EdgeDTO;
import io.citegraph.app.model.PaperResponse;
import io.citegraph.app.model.PathDTO;
import io.citegraph.app.model.VertexDTO;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.Path;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.attribute.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.apache.tinkerpop.gremlin.process.traversal.Order.asc;

@RestController
@RequestMapping("/apis/")
@CrossOrigin(origins = "http://localhost:3000")
public class GraphController {

    private static final Logger LOG = LoggerFactory.getLogger(GraphController.class);

    private static final int DEFAULT_LIMIT = 500;

    private static final List<Integer> DEFAULT_INT_LIST = Arrays.asList(0);
    private static final List<String> DEFAULT_STRING_LIST = Arrays.asList("N/A");

    @Autowired
    private GraphTraversalSource g;

    @Autowired
    private Cache<String, String> authorCache;

    @GetMapping("/paper/{id}")
    @SuppressWarnings("unchecked")
    public PaperResponse getPaper(@PathVariable String id, @RequestParam int limit, @RequestParam boolean getEdges) {
        if (!g.V().hasId(id).hasNext()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, String.format("Paper %s not found", id)
            );
        }
        Vertex paper = g.V().hasId(id).next();
        Map<Object, Object> paperProps = g.V(paper).elementMap().next();
        if (!paperProps.containsKey("title")) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, String.format("Paper %s does not have title", id)
            );
        }
        PaperResponse paperResponse = new PaperResponse(
            id,
            (String) paperProps.get("title"),
            (Integer) paperProps.getOrDefault("year", 0));

        // collect authors
        if (getEdges) {
            List<Object> authorIds = g.V(paper)
                .inE("writes")
                .order().by("authorOrder", asc)
                .outV()
                .id()
                .toList();
            List<AuthorResponse> authors = IntStream.range(0, authorIds.size())
                .mapToObj(index -> {
                    Object vid = authorIds.get(index);
                    Map<Object, Object> props = g.V(vid).elementMap().next();
                    return new AuthorResponse(
                        (String) props.getOrDefault("name", "unknown"),
                        (String) vid,
                        (Integer) props.getOrDefault("numOfPaperReferers", 0),
                        (Double) props.getOrDefault("pagerank", 0.0),
                        index + 1);
                })
                .collect(Collectors.toList());
            paperResponse.setAuthors(authors);
        }

        // collect pagerank
        double pagerank = (Double) paperProps.getOrDefault("pagerank", 0.0);
        // collect paper citations
        int numOfReferees = (Integer) paperProps.getOrDefault("numOfPaperReferees", 0);
        int numOfReferers = (Integer) paperProps.getOrDefault("numOfPaperReferers", 0);
        paperResponse.setNumOfReferees(numOfReferees);
        paperResponse.setNumOfReferers(numOfReferers);
        paperResponse.setPagerank(pagerank);
        paperResponse.setVenue((String) paperProps.get("venue"));
        paperResponse.setKeywords((String) paperProps.get("keywords"));
        paperResponse.setField((String) paperProps.get("field"));
        paperResponse.setDocType((String) paperProps.get("docType"));
        paperResponse.setVolume((String) paperProps.get("volume"));
        paperResponse.setIssue((String) paperProps.get("issue"));
        paperResponse.setIssn((String) paperProps.get("issn"));
        paperResponse.setIsbn((String) paperProps.get("isbn"));
        paperResponse.setDoi((String) paperProps.get("doi"));
        paperResponse.setPaperAbstract((String) paperProps.get("abstract"));

        if (getEdges) {
            List<PaperResponse> referees = g.V(paper).out("cites").limit(limit).toList()
                .stream()
                .map(v -> {
                    Map<Object, Object> props = g.V(v).elementMap().next();
                    return new PaperResponse(
                        (String) v.id(),
                        (String) props.getOrDefault("title", "N/A"),
                        (Integer) props.getOrDefault("year", "N/A"),
                        (Integer) props.getOrDefault("numOfPaperReferees", 0),
                        (Integer) props.getOrDefault("numOfPaperReferers", 0),
                        (Double) props.getOrDefault("pagerank", 0.0));
                })
                .collect(Collectors.toList());
            paperResponse.setReferees(referees);

            List<PaperResponse> referers = g.V(paper).in("cites").limit(limit).toList()
                .stream()
                .map(v -> {
                    Map<Object, Object> props = g.V(v).elementMap().next();
                    return new PaperResponse(
                        (String) v.id(),
                        (String) props.getOrDefault("title", "N/A"),
                        (Integer) props.getOrDefault("year", "N/A"),
                        (Integer) props.getOrDefault("numOfPaperReferees", 0),
                        (Integer) props.getOrDefault("numOfPaperReferers", 0),
                        (Double) props.getOrDefault("pagerank", 0.0));
                })
                .collect(Collectors.toList());
            paperResponse.setReferers(referers);
        }

        return paperResponse;
    }

    @GetMapping("/author/{id}")
    @SuppressWarnings("unchecked")
    public AuthorResponse getAuthor(@PathVariable String id, @RequestParam int limit, @RequestParam boolean getEdges) {
        if (!g.V().hasId(id).hasNext()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, String.format("Author %s not found", id)
            );
        }
        Vertex author = g.V().hasId(id).next();
        Map<Object, Object> authorProps = g.V(author).elementMap().next();
        if (!authorProps.containsKey("name")) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, String.format("Author %s's name not found", id)
            );
        }
        String name = (String) authorProps.get("name");

        authorCache.get(id, k -> name);

        // collect pagerank
        double pagerank = (Double) authorProps.getOrDefault("pagerank", 0.0);
        // collect papers
        int numOfPapers = (Integer) authorProps.getOrDefault("numOfPapers", 0);

        // collect author references
        int numOfReferees = (Integer) authorProps.getOrDefault("numOfAuthorReferees", 0);
        int numOfReferers = (Integer) authorProps.getOrDefault("numOfAuthorReferers", 0);
        // collect paper references
        int numOfPaperReferees = (Integer) authorProps.getOrDefault("numOfPaperReferees", 0);
        int numOfPaperReferers = (Integer) authorProps.getOrDefault("numOfPaperReferers", 0);

        int numOfCoauthors = (Integer) authorProps.getOrDefault("numOfCoworkers", 0);

        String org = (String) authorProps.getOrDefault("org", "");

        AuthorResponse res = new AuthorResponse(name, id, org, numOfPapers, numOfReferees, numOfReferers,
            numOfPaperReferees, numOfPaperReferers, numOfCoauthors, pagerank);

        if (getEdges) {
            List<PaperResponse> papers = g.V(author).out("writes").limit(limit).toList()
                .stream()
                .map(v -> {
                    Map<Object, Object> props = g.V(v).elementMap().next();
                    return new PaperResponse((String) v.id(),
                        (String) props.getOrDefault("title", "N/A"),
                        (Integer) props.getOrDefault("year", 0),
                        (Integer) props.getOrDefault("numOfPaperReferees", 0),
                        (Integer) props.getOrDefault("numOfPaperReferers", 0),
                        (Double) props.getOrDefault("pagerank", 0.0)
                    );
                })
                .collect(Collectors.toList());
            res.setPapers(papers);

            Map<String, Map<Object, Object>> idPropMap = new HashMap<>();
            buildPropMap(idPropMap, g.V(author).union(
                __.out("refers").limit(limit),
                __.in("refers").limit(limit),
                __.both("collaborates").limit(limit)
            ).toList());

            List<Edge> referees = g.V(author).outE("refers").limit(limit).toList();
            List<CitationResponse> refereeResponse = new ArrayList<>(referees.size());
            for (Edge referee : referees) {
                int refCount = (Integer) g.E(referee).values("refCount").next();
                String refereeId = (String) referee.inVertex().id();
                String refereeName = (String) idPropMap.get(refereeId).get("name");
                double refereePagerank = (Double) idPropMap.get(refereeId).get("pagerank");
                CitationResponse citationResponse = new CitationResponse(new AuthorResponse(refereeName, refereeId, refereePagerank), refCount);
                refereeResponse.add(citationResponse);
            }

            List<Edge> referers = g.V(author).inE("refers").limit(limit).toList();
            List<CitationResponse> refererResponse = new ArrayList<>(referers.size());
            for (Edge referer : referers) {
                int refCount = (Integer) g.E(referer).values("refCount").tryNext().orElse(1);
                String refererId = (String) referer.outVertex().id();
                String refererName = (String) idPropMap.get(refererId).get("name");
                double refererPagerank = (Double) idPropMap.get(refererId).get("pagerank");
                CitationResponse citationResponse = new CitationResponse(new AuthorResponse(refererName, refererId, refererPagerank), refCount);
                refererResponse.add(citationResponse);
            }

            List<Edge> coauthors = g.V(author).bothE("collaborates").limit(limit).toList();
            List<CollaborationResponse> coauthorResponse = new ArrayList<>(coauthors.size());
            for (Edge edge : coauthors) {
                int collaborationCount = (Integer) g.E(edge).values("collaborateCount").tryNext().orElse(1);
                Vertex otherVertex = edge.outVertex().equals(author) ? edge.inVertex() : edge.outVertex();
                String coauthorId = (String) otherVertex.id();
                String coauthorName = (String) idPropMap.get(coauthorId).get("name");
                double coauthorPagerank = (Double) idPropMap.get(coauthorId).get("pagerank");
                CollaborationResponse collaborationResponse = new CollaborationResponse(new AuthorResponse(coauthorName, coauthorId, coauthorPagerank), collaborationCount);
                coauthorResponse.add(collaborationResponse);
            }

            res.setReferees(refereeResponse);
            res.setReferers(refererResponse);
            res.setCoauthors(coauthorResponse);
        }

        return res;
    }

    @GetMapping("/search/author/{name}")
    public List<AuthorResponse> getAuthorByName(@PathVariable String name) {
        List<AuthorResponse> authors = g.V().has("name", Text.textContains(name)).limit(DEFAULT_LIMIT).toList()
            .stream()
            .map(v -> new AuthorResponse(
                (String) g.V(v).values("name").next(),
                (String) g.V(v).values("org").tryNext().orElseGet(() -> null),
                (String) v.id()))
            .collect(Collectors.toList());
        return authors;
    }

    @GetMapping("/search/paper/{title}")
    public List<PaperResponse> getPaperByTitle(@PathVariable String title) {
        List<PaperResponse> papers = g.V().has("title", Text.textContains(title)).limit(DEFAULT_LIMIT).toList()
            .stream()
            .map(v -> new PaperResponse((String) v.id(), (String) g.V(v).values("title").next()))
            .collect(Collectors.toList());
        return papers;
    }

    @GetMapping("/discover/authors")
    public List<AuthorResponse> getAuthors() {
        return authorCache.asMap().entrySet().stream()
            .map(entry -> new AuthorResponse(entry.getValue(), entry.getKey()))
            .collect(Collectors.toList());
    }

    @GetMapping("/graph/cluster/{vid}")
    public List<PathDTO> getCluster(@PathVariable String vid) {
        String clusterId = (String) g.V(vid).values("clusterId").next();
        return g.V(vid)
            .repeat(__.bothE("collaborates").otherV().simplePath())
            .until(__.not(__.has("clusterId", clusterId)))
            .limit(100)
            .path()
            .by(__.elementMap())
            .by(__.label())
            .toStream()
            .map(PathDTO::new)
            .collect(Collectors.toList());
    }

    @GetMapping("/graph/path")
    public List<PathDTO> getShortestPath(@RequestParam String fromId, @RequestParam String toId) {
        return g.V(fromId)
            .repeat(__.bothE("collaborates", "refers").otherV().simplePath())
            .until(__.hasId(toId))
            .path()
            .by(__.elementMap())
            .by(__.label())
            .limit(1)
            .toStream()
            .map(PathDTO::new)
            .collect(Collectors.toList());
    }

    /**
     * Feature request: https://github.com/Citegraph/citegraph/issues/2
     * Given a paper, find its n-hop references, with the following requirements:
     * 1) The "bedrock" paper, defined as the one has most incoming links in this subgraph.
     * 2) 2-hop graph would already be very crazily large, and sometimes user might want even 3-hop. Assuming the
     * "bedrock" paper would have been cited multiple times in this hypothetical subgraph, we could randomly select,
     * say, 10 outgoing links from every node except the original starting point
     *
     * Devnote: why do we not use a single gremlin query to achieve this? Three reasons:
     * 1) I believe it's technically possible to write a single query to achieve the above requirements, but I am not
     * an expert in writing complex gremlin queries
     * 2) Gremlin path representation contains duplicate data when we want a lot of paths that are more or less overlapping.
     * It's okay since our Gremlin server and web app server run on the same machine, but ideally we do want to reduce
     * unnecessary network cost
     * 3) We currently set up a global timeout on Gremlin server. For a task as complicated as this one, I do want to give
     * it more leniency to run longer. By splitting a single complicated giant query into a few smaller ones, we overcome
     * the timeout limitation.
     *
     * @param vid
     * @return
     */
    @GetMapping("/graph/citations/{vid}")
    public PathDTO getCitationNetwork(@PathVariable String vid) {
        PathDTO result = new PathDTO();
        Set<String> visited = new HashSet<>();
        visited.add(vid);

        // step 0: get title of current vertex
        String title = (String) g.V(vid).values("title").next();
        result.getVertices().add(new VertexDTO(vid, null, title, 0));

        // step 1: find immediate neighbors (1-hop references)
        List<Map<Object, Object>> oneHopVertices = g.V(vid).out("cites").elementMap("title").toList();
        for (Map<Object, Object> oneHopVertex : oneHopVertices) {
            String id = (String) oneHopVertex.get(T.id);
            visited.add(id);
            title = (String) oneHopVertex.get("title");
            result.getVertices().add(new VertexDTO(id, null, title, 0));
            result.getEdges().add(new EdgeDTO(vid, id, "cites"));
        }

        // step 2: find 2-hop references
        List<String> frontier = new ArrayList<>();
        for (Map<Object, Object> oneHopVertex : oneHopVertices) {
            String id = (String) oneHopVertex.get(T.id);
            List<Map<Object, Object>> twoHopVertices = g.V(id).out("cites").elementMap("title").toList();
            for (Map<Object, Object> twoHopVertex : twoHopVertices) {
                String toId = (String) twoHopVertex.get(T.id);
                result.getEdges().add(new EdgeDTO(id, toId, "cites"));
                if (visited.contains(toId)) {
                    frontier.add(toId);
                } else {
                    title = (String) twoHopVertex.get("title");
                    result.getVertices().add(new VertexDTO(toId, null, title, 0));
                    visited.add(toId);
                }
            }
        }

        // step 3: if we have less than 20 vertices, find 3-hop references, but only traverse 10 edges per vertex
        if (frontier.size() < 20) {
            for (String fromId: frontier) {
                List<Map<Object, Object>> threeHopVertices = g.V(fromId).out("cites").limit(10).elementMap("title").toList();
                for (Map<Object, Object> threeHopVertex : threeHopVertices) {
                    String toId = (String) threeHopVertex.get(T.id);
                    result.getEdges().add(new EdgeDTO(fromId, toId, "cites"));
                    if (!visited.add(toId)) {
                        title = (String) threeHopVertex.get("title");
                        result.getVertices().add(new VertexDTO(toId, null, title, 0));
                        visited.add(toId);
                    }
                }
            }
        }
        return result;
    }

    @GetMapping("/graph/vertex/{vid}")
    public Map<String, Object> getVertexById(@PathVariable String vid, @RequestParam int limit, @RequestParam boolean getEdges) {
        try {

            if (getEdges) {
                return g.V(vid)
                    .project("self", "neighbors")
                    .by(__.elementMap())
                    .by(
                        __.union(
                                __.bothE("collaborates").limit(limit).as("edge")
                                    .otherV().as("vertex")
                                    .select("edge", "vertex")
                                    .by(__.elementMap()),
                                __.outE("cites").limit(limit).as("edge")
                                    .otherV().as("vertex")
                                    .select("edge", "vertex")
                                    .by(__.elementMap()),
                                __.inE("cites").limit(limit).as("edge")
                                    .otherV().as("vertex")
                                    .select("edge", "vertex")
                                    .by(__.elementMap()),
                                __.outE("refers").limit(limit).as("edge")
                                    .otherV().as("vertex")
                                    .select("edge", "vertex")
                                    .by(__.elementMap()),
                                __.inE("refers").limit(limit).as("edge")
                                    .otherV().as("vertex")
                                    .select("edge", "vertex")
                                    .by(__.elementMap()),
                                __.bothE("writes").limit(limit).as("edge")
                                    .otherV().as("vertex")
                                    .select("edge", "vertex")
                                    .by(__.elementMap())
                            )
                            .fold()
                    ).next();
            } else {
                return g.V(vid)
                    .project("self")
                    .by(__.elementMap())
                    .next();
            }
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, String.format("Vertex (ID: %s) not found", vid));
        }
    }

    private void buildPropMap(Map<String, Map<Object, Object>> idPropMap, List<Vertex> authors) {
        for (Vertex v : authors) {
            Map<Object, Object> props = g.V(v).elementMap("name", "pagerank").next();
            idPropMap.putIfAbsent((String) v.id(), props);
        }
    }
}
