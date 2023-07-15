package io.citegraph.app;

import com.github.benmanes.caffeine.cache.Cache;
import io.citegraph.app.model.AuthorResponse;
import io.citegraph.app.model.CitationResponse;
import io.citegraph.app.model.CollaborationResponse;
import io.citegraph.app.model.PaperResponse;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Edge;
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

import javax.ws.rs.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            List<AuthorResponse> authors = g.V(paper).in("writes").toList()
                .stream()
                .map(v -> new AuthorResponse(
                    (String) g.V(v).values("name").tryNext().orElse("unknown"),
                    (String) v.id()))
                .collect(Collectors.toList());
            paperResponse.setAuthors(authors);
        }

        // collect paper citations
        int numOfReferees = (Integer) paperProps.getOrDefault("numOfPaperReferees", 0);
        int numOfReferers = (Integer) paperProps.getOrDefault("numOfPaperReferers", 0);
        paperResponse.setNumOfReferees(numOfReferees);
        paperResponse.setNumOfReferers(numOfReferers);

        if (getEdges) {
            List<PaperResponse> referees = g.V(paper).out("cites").limit(limit).toList()
                .stream()
                .map(v -> new PaperResponse(
                    (String) v.id(),
                    (String) g.V(v).values("title").next(),
                    (Integer) g.V(v).values("year").next()))
                .collect(Collectors.toList());
            paperResponse.setReferees(referees);

            List<PaperResponse> referers = g.V(paper).in("cites").limit(limit).toList()
                .stream()
                .map(v -> new PaperResponse(
                    (String) v.id(),
                    (String) g.V(v).values("title").next(),
                    (Integer) g.V(v).values("year").next()))
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

        // collect papers
        int numOfPapers = (Integer) authorProps.getOrDefault("numOfPapers", 0);

        // collect author references
        int numOfReferees = (Integer) authorProps.getOrDefault("numOfAuthorReferees", 0);
        int numOfReferers = (Integer) authorProps.getOrDefault("numOfAuthorReferers", 0);
        // collect paper references
        int numOfPaperReferees = (Integer) authorProps.getOrDefault("numOfPaperReferees", 0);
        int numOfPaperReferers = (Integer) authorProps.getOrDefault("numOfPaperReferers", 0);

        int numOfCoauthors = (Integer) authorProps.getOrDefault("numOfCoworkers", 0);

        AuthorResponse res = new AuthorResponse(name, id, numOfPapers, numOfReferees, numOfReferers,
            numOfPaperReferees, numOfPaperReferers, numOfCoauthors);

        if (getEdges) {
            List<PaperResponse> papers = g.V(author).out("writes").limit(limit).toList()
                .stream()
                .map(v -> {
                    Map<Object, Object> props = g.V(v).elementMap().next();
                    return new PaperResponse((String) v.id(),
                        (String) props.getOrDefault("title", "N/A"),
                        (Integer) props.getOrDefault("year", 0),
                        (Integer) props.getOrDefault("numOfPaperReferees", 0),
                        (Integer) props.getOrDefault("numOfPaperReferers", 0)
                    );
                })
                .collect(Collectors.toList());
            res.setPapers(papers);

            Map<String, String> idNameMap = new HashMap<>();
            buildNameMap(idNameMap, g.V(author).union(
                __.out("refers").limit(limit),
                __.in("refers").limit(limit),
                __.both("collaborates").limit(limit)
            ).toList());

            List<Edge> referees = g.V(author).outE("refers").limit(limit).toList();
            List<CitationResponse> refereeResponse = new ArrayList<>(referees.size());
            for (Edge referee : referees) {
                int refCount = (Integer) g.E(referee).values("refCount").next();
                String refereeId = (String) referee.inVertex().id();
                String refereeName = idNameMap.get(refereeId);
                CitationResponse citationResponse = new CitationResponse(new AuthorResponse(refereeName, refereeId), refCount);
                refereeResponse.add(citationResponse);
            }

            List<Edge> referers = g.V(author).inE("refers").limit(limit).toList();
            List<CitationResponse> refererResponse = new ArrayList<>(referers.size());
            for (Edge referer : referers) {
                int refCount = (Integer) g.E(referer).values("refCount").tryNext().orElse(1);
                String refererId = (String) referer.outVertex().id();
                String refererName = idNameMap.get(refererId);
                CitationResponse citationResponse = new CitationResponse(new AuthorResponse(refererName, refererId), refCount);
                refererResponse.add(citationResponse);
            }

            List<Edge> coauthors = g.V(author).bothE("collaborates").limit(limit).toList();
            List<CollaborationResponse> coauthorResponse = new ArrayList<>(coauthors.size());
            for (Edge edge : coauthors) {
                int collaborationCount = (Integer) g.E(edge).values("collaborateCount").tryNext().orElse(1);
                Vertex otherVertex = edge.outVertex().equals(author) ? edge.inVertex() : edge.outVertex();
                String coauthorId = (String) otherVertex.id();
                String coauthorName = idNameMap.get(coauthorId);
                CollaborationResponse collaborationResponse = new CollaborationResponse(new AuthorResponse(coauthorName, coauthorId), collaborationCount);
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
            .map(v -> new AuthorResponse((String) g.V(v).values("name").next(), (String) v.id()))
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

    @GetMapping("/graph/vertex/{vid}")
    public Map<String, Object> getVertexById(@PathVariable String vid, @RequestParam int limit, @RequestParam boolean getEdges) {
        if (getEdges) {
            return g.V(vid)
                .project("self", "neighbors")
                .by(__.elementMap())
                .by(
                    __.bothE().limit(limit).as("edge")
                        .otherV().as("vertex")
                        .select("edge", "vertex")
                        .by(__.elementMap())
                        .fold()
                ).next();
        } else {
            return g.V(vid)
                .project("self")
                .by(__.elementMap())
                .next();
        }
    }

    private void buildNameMap(Map<String, String> idNameMap, List<Vertex> authors) {
        for (Vertex v : authors) {
            idNameMap.putIfAbsent((String) v.id(), (String) g.V(v).values("name").tryNext().orElse("unknown"));
        }
    }
}
