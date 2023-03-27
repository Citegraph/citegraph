package io.citegraph.app;

import com.github.benmanes.caffeine.cache.Cache;
import io.citegraph.app.model.AuthorResponse;
import io.citegraph.app.model.CitationResponse;
import io.citegraph.app.model.PaperResponse;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.attribute.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/")
public class GraphController {

    private static final Logger LOG = LoggerFactory.getLogger(GraphController.class);

    private static final int DEFAULT_LIMIT = 100;

    private int searchLimit = DEFAULT_LIMIT;

    @Autowired
    private JanusGraph graph;

    @Autowired
    private Cache<String, String> authorCache;

    @CrossOrigin
    @GetMapping("/paper/{id}")
    public PaperResponse getPaper(@PathVariable String id) {
        GraphTraversalSource g = graph.traversal();
        if (!g.V().hasId(id).hasNext()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, String.format("Paper %s not found", id)
            );
        }
        Vertex paper = g.V().hasId(id).next();
        PaperResponse paperResponse = new PaperResponse(id, paper.value("title"), paper.value("year"));

        // collect authors
        List<AuthorResponse> authors = g.V(paper).in("writes").toList()
            .stream()
            .map(v -> new AuthorResponse(v.value("name"), (String) v.id()))
            .collect(Collectors.toList());
        paperResponse.setAuthors(authors);

        // collect paper citations
        long numOfReferees = g.V(paper).outE("cites").count().next();
        long numOfReferers = g.V(paper).inE("cites").count().next();
        paperResponse.setNumOfReferees((int) numOfReferees);
        paperResponse.setNumOfReferers((int) numOfReferers);

        List<PaperResponse> referees = g.V(paper).out("cites").limit(searchLimit).toList()
            .stream()
            .map(v -> new PaperResponse((String) v.id(), v.value("title"), v.value("year")))
            .collect(Collectors.toList());
        paperResponse.setReferees(referees);

        List<PaperResponse> referers = g.V(paper).in("cites").limit(searchLimit).toList()
            .stream()
            .map(v -> new PaperResponse((String) v.id(), v.value("title"), v.value("year")))
            .collect(Collectors.toList());
        paperResponse.setReferers(referers);

        return paperResponse;
    }

    @CrossOrigin
    @GetMapping("/author/{id}")
    public AuthorResponse getAuthor(@PathVariable String id) {
        GraphTraversalSource g = graph.traversal();
        if (!g.V().hasId(id).hasNext()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, String.format("Author %s not found", id)
            );
        }
        Vertex author = g.V().hasId(id).next();
        String name = (String) g.V(author).values("name").next();

        authorCache.get(id, k -> name);

        // collect papers
        long numOfPapers = g.V(author).out("writes").count().next();
        List<PaperResponse> papers = g.V(author).out("writes").limit(searchLimit).toList()
            .stream()
            .map(v -> new PaperResponse((String) v.id(), v.value("title"), v.value("year")))
            .collect(Collectors.toList());

        // collect author references
        long numOfReferees = g.V(author).outE("refers").count().next();
        long numOfReferers = g.V(author).inE("refers").count().next();

        Map<String, String> idNameMap = new HashMap<>();
        buildNameMap(idNameMap, g.V(author).out("refers").limit(searchLimit).toList());
        buildNameMap(idNameMap, g.V(author).in("refers").limit(searchLimit).toList());

        List<Edge> referees = g.V(author).outE("refers").limit(searchLimit).toList();
        List<CitationResponse> refereeResponse = new ArrayList<>();
        for (Edge referee : referees) {
            int refCount = referee.value("refCount");
            String refereeId = (String) referee.inVertex().id();
            String refereeName = idNameMap.get(refereeId);
            CitationResponse citationResponse = new CitationResponse(new AuthorResponse(refereeName, refereeId), refCount);
            refereeResponse.add(citationResponse);
        }

        List<Edge> referers = g.V(author).inE("refers").limit(searchLimit).toList();
        List<CitationResponse> refererResponse = new ArrayList<>();
        for (Edge referer : referers) {
            int refCount = referer.value("refCount");
            String refererId = (String) referer.outVertex().id();
            String refererName = idNameMap.get(refererId);
            CitationResponse citationResponse = new CitationResponse(new AuthorResponse(refererName, refererId), refCount);
            refererResponse.add(citationResponse);
        }

        AuthorResponse res = new AuthorResponse(name, id, (int) numOfPapers, (int) numOfReferees, (int) numOfReferers);
        res.setPapers(papers);
        res.setReferees(refereeResponse);
        res.setReferers(refererResponse);
        return res;
    }


    @CrossOrigin
    @GetMapping("/search/author/{name}")
    public List<AuthorResponse> getAuthorByName(@PathVariable String name) {
        GraphTraversalSource g = graph.traversal();
        List<AuthorResponse> authors = g.V().has("name", Text.textContains(name)).toList()
            .stream()
            .map(v -> new AuthorResponse(v.value("name"), (String) v.id()))
            .collect(Collectors.toList());
        return authors;
    }

    @CrossOrigin
    @GetMapping("/discover/authors")
    public List<AuthorResponse> getAuthors() {
        return authorCache.asMap().entrySet().stream()
            .map(entry -> new AuthorResponse(entry.getValue(), entry.getKey()))
            .collect(Collectors.toList());
    }

    private void buildNameMap(Map<String, String> idNameMap, List<Vertex> authors) {
        for (Vertex v : authors) {
            idNameMap.putIfAbsent((String) v.id(), v.value("name"));
        }
    }
}
