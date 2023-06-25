package io.citegraph.app;

import com.github.benmanes.caffeine.cache.Cache;
import io.citegraph.app.model.AuthorResponse;
import io.citegraph.app.model.CitationResponse;
import io.citegraph.app.model.PaperResponse;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/apis/")
@CrossOrigin(origins = "http://localhost:3000")
public class GraphController {

    private static final Logger LOG = LoggerFactory.getLogger(GraphController.class);

    private static final int DEFAULT_LIMIT = 500;

    private static final List<Integer> DEFAULT_LIST = Arrays.asList(0);

    @Autowired
    private GraphTraversalSource g;

    @Autowired
    private Cache<String, String> authorCache;

    @GetMapping("/paper/{id}")
    public PaperResponse getPaper(@PathVariable String id, @RequestParam int limit) {
        if (!g.V().hasId(id).hasNext()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, String.format("Paper %s not found", id)
            );
        }
        Vertex paper = g.V().hasId(id).next();
        Map<Object, Object> paperProps = g.V(paper).valueMap().next();
        if (!paperProps.containsKey("title")) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, String.format("Paper %s does not have title", id)
            );
        }
        PaperResponse paperResponse = new PaperResponse(
            id,
            ((List<String>) paperProps.get("title")).get(0),
            ((List<Integer>) paperProps.getOrDefault("year", DEFAULT_LIST)).get(0));

        // collect authors
        List<AuthorResponse> authors = g.V(paper).in("writes").toList()
            .stream()
            .map(v -> new AuthorResponse(
                (String) g.V(v).values("name").tryNext().orElse("unknown"),
                (String) v.id()))
            .collect(Collectors.toList());
        paperResponse.setAuthors(authors);

        // collect paper citations
        int numOfReferees = ((List<Integer>) paperProps.getOrDefault("numOfPaperReferees", DEFAULT_LIST)).get(0);
        int numOfReferers = ((List<Integer>) paperProps.getOrDefault("numOfPaperReferers", DEFAULT_LIST)).get(0);
        paperResponse.setNumOfReferees(numOfReferees);
        paperResponse.setNumOfReferers(numOfReferers);

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

        return paperResponse;
    }

    @GetMapping("/author/{id}")
    public AuthorResponse getAuthor(@PathVariable String id, @RequestParam int limit) {
        if (!g.V().hasId(id).hasNext()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, String.format("Author %s not found", id)
            );
        }
        Vertex author = g.V().hasId(id).next();
        Map<Object, Object> authorProps = g.V(author).valueMap().next();
        if (!authorProps.containsKey("name")) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, String.format("Author %s's name not found", id)
            );
        }
        String name = ((List<String>) authorProps.get("name")).get(0);

        authorCache.get(id, k -> name);

        // collect papers
        int numOfPapers = ((List<Integer>) authorProps.getOrDefault("numOfPapers", DEFAULT_LIST)).get(0);
        List<PaperResponse> papers = g.V(author).out("writes").limit(limit).toList()
            .stream()
            .map(v -> new PaperResponse((String) v.id(),
                (String) g.V(v).values("title").next(),
                (Integer) g.V(v).values("year").next()))
            .collect(Collectors.toList());

        // collect author references
        int numOfReferees = ((List<Integer>) authorProps.getOrDefault("numOfAuthorReferees", DEFAULT_LIST)).get(0);
        int numOfReferers = ((List<Integer>) authorProps.getOrDefault("numOfAuthorReferers", DEFAULT_LIST)).get(0);
        // collect paper references
        int numOfPaperReferees = ((List<Integer>) authorProps.getOrDefault("numOfPaperReferees", DEFAULT_LIST)).get(0);
        int numOfPaperReferers = ((List<Integer>) authorProps.getOrDefault("numOfPaperReferers", DEFAULT_LIST)).get(0);

        int numOfCoauthors = ((List<Integer>) authorProps.getOrDefault("numOfCoworkers", DEFAULT_LIST)).get(0);

        Map<String, String> idNameMap = new HashMap<>();
        buildNameMap(idNameMap, g.V(author).out("refers").limit(limit).toList());
        buildNameMap(idNameMap, g.V(author).in("refers").limit(limit).toList());

        List<Edge> referees = g.V(author).outE("refers").limit(limit).toList();
        List<CitationResponse> refereeResponse = new ArrayList<>();
        for (Edge referee : referees) {
            int refCount = (Integer) g.E(referee).values("refCount").next();
            String refereeId = (String) referee.inVertex().id();
            String refereeName = idNameMap.get(refereeId);
            CitationResponse citationResponse = new CitationResponse(new AuthorResponse(refereeName, refereeId), refCount);
            refereeResponse.add(citationResponse);
        }

        List<Edge> referers = g.V(author).inE("refers").limit(limit).toList();
        List<CitationResponse> refererResponse = new ArrayList<>();
        for (Edge referer : referers) {
            int refCount = (Integer) g.E(referer).values("refCount").tryNext().orElse(1);
            String refererId = (String) referer.outVertex().id();
            String refererName = idNameMap.get(refererId);
            CitationResponse citationResponse = new CitationResponse(new AuthorResponse(refererName, refererId), refCount);
            refererResponse.add(citationResponse);
        }

        AuthorResponse res = new AuthorResponse(name, id, numOfPapers, numOfReferees, numOfReferers,
            numOfPaperReferees, numOfPaperReferers, numOfCoauthors);
        res.setPapers(papers);
        res.setReferees(refereeResponse);
        res.setReferers(refererResponse);
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

    private void buildNameMap(Map<String, String> idNameMap, List<Vertex> authors) {
        for (Vertex v : authors) {
            idNameMap.putIfAbsent((String) v.id(), (String) g.V(v).values("name").tryNext().orElse("unknown"));
        }
    }
}
