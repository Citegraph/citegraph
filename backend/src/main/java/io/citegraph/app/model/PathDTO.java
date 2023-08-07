package io.citegraph.app.model;

import org.apache.tinkerpop.gremlin.process.traversal.Path;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PathDTO {
    private List<VertexDTO> vertices = new ArrayList<>();
    private List<EdgeDTO> edges = new ArrayList<>();
    public PathDTO(Path path) {
        List<Object> objects = path.objects();
        for (int i = 0; i < objects.size(); i += 2) {
            Map<Object, Object> map = (Map<Object, Object>) objects.get(i);
            vertices.add(new VertexDTO(
                (String) map.get(T.id),
                (String) map.get("name"),
                (String) map.get("title"),
                (Double) map.get("pagerank")
            ));
        }
        int vIdx = 0;
        for (int i = 1; i < objects.size(); i += 2) {
            String label = (String) objects.get(i);
            edges.add(new EdgeDTO(vertices.get(vIdx).getId(), vertices.get(vIdx+1).getId(), label));
            vIdx++;
        }
    }

    public List<VertexDTO> getVertices() {
        return vertices;
    }

    public void setVertices(List<VertexDTO> vertices) {
        this.vertices = vertices;
    }

    public List<EdgeDTO> getEdges() {
        return edges;
    }

    public void setEdges(List<EdgeDTO> edges) {
        this.edges = edges;
    }
}