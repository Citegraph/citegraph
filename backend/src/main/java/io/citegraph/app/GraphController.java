package io.citegraph.app;

import io.citegraph.app.model.AuthorResponse;
import io.citegraph.data.DblpParser;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
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

import java.util.List;

@RestController
@RequestMapping("/")
public class GraphController {

    private static final Logger LOG = LoggerFactory.getLogger(GraphController.class);
    @Autowired
    private JanusGraph graph;

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
        List<Vertex> papers = g.V(author).out("writes").toList();
        List<Vertex> referees = g.V(author).out("refers").toList();
        List<Vertex> referers = g.V(author).in("refers").toList();
        AuthorResponse res = new AuthorResponse(name, id, papers.size(), referees.size(), referers.size());
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
}
