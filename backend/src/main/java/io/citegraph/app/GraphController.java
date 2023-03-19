package io.citegraph.app;

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
    @Autowired
    private JanusGraph graph;

    /**
     * @param id
     * @return
     */
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

        // collect papers
        List<PaperResponse> papers = g.V(author).out("writes").toList()
            .stream()
            .map(v -> new PaperResponse((String) v.id(), v.value("title"), v.value("year")))
            .collect(Collectors.toList());

        // collect author references
        Map<String, String> idNameMap = new HashMap<>();
        buildNameMap(idNameMap, g.V(author).both("refers").toList());

        List<Edge> referees = g.V(author).outE("refers").toList();
        List<CitationResponse> refereeResponse = new ArrayList<>();
        for (Edge referee : referees) {
            int refCount = referee.value("refCount");
            String refereeId = (String) referee.inVertex().id();
            String refereeName = idNameMap.get(refereeId);
            CitationResponse citationResponse = new CitationResponse(new AuthorResponse(refereeName, refereeId), refCount);
            refereeResponse.add(citationResponse);
        }

        List<Edge> referers = g.V(author).inE("refers").toList();
        List<CitationResponse> refererResponse = new ArrayList<>();
        for (Edge referer : referers) {
            int refCount = referer.value("refCount");
            String refererId = (String) referer.outVertex().id();
            String refererName = idNameMap.get(refererId);
            CitationResponse citationResponse = new CitationResponse(new AuthorResponse(refererName, refererId), refCount);
            refererResponse.add(citationResponse);
        }

        AuthorResponse res = new AuthorResponse(name, id, papers.size(), referees.size(), referers.size());
        res.setPapers(papers);
        res.setReferees(refereeResponse);
        res.setReferers(refererResponse);
        return res;
    }


    @CrossOrigin
    @GetMapping("/search/author/{name}")
    public AuthorResponse getAuthorByName(@PathVariable String name) {
        GraphTraversalSource g = graph.traversal();
        // TODO: for now, we assume the first match is the right one
        List<Object> ids = g.V().has("name", Text.textContains(name)).id().toList();
        LOG.info("Authors are {}", ids);
        if (ids.isEmpty()) {
            return null;
        }
        return getAuthor((String) ids.get(0));
    }

    private void buildNameMap(Map<String, String> idNameMap, List<Vertex> authors) {
        for (Vertex v : authors) {
            idNameMap.putIfAbsent((String) v.id(), v.value("name"));
        }
    }
}
