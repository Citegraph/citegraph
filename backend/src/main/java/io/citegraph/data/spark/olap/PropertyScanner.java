package io.citegraph.data.spark.olap;

import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.spark.process.computer.SparkGraphComputer;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.GraphFactory;

import java.util.List;
import java.util.Map;

import static io.citegraph.data.spark.Utils.getSparkGraphConfig;
import static org.apache.tinkerpop.gremlin.process.traversal.Order.desc;

/**
 * This Spark application iterates through property of each vertex and edge
 */
public class PropertyScanner {
    public static void main(String[] args) throws Exception {
        Graph graph = GraphFactory.open(getSparkGraphConfig());
        GraphTraversalSource g = graph.traversal().withComputer(SparkGraphComputer.class);
        long startTime = System.currentTimeMillis();

        boolean q1 = false, q2 = true, q3 = false, q4 = false;

        // which author is cited/referred by most people?
        if (q1) {
            int maxAuthorReferers = (Integer) g.V().has("type", "author").values("numOfAuthorReferers").max().next();
            Vertex v = g.V().has("type", "author").has("numOfAuthorReferers", maxAuthorReferers).next();
            System.out.println("Cited by most people: " + g.V(v).elementMap().next());
        }

        // which author has the most number of unique collaborators?
        // TODO: unfortunately, this finds 53f64dbfdabfae0633f15b0e Yang Liu, which seems to be a collection of multiple people
        // with the same name. They collectively have 4389 coworkers. Can we use clustering algorithm to separate them?
        if (q2) {
            List<Map<Object, Object>> elements = g.V().has("type", "author").has("numOfCoworkers", P.gt(2000)).elementMap().toList();
            System.out.println("Collaborated with most people: \n" + elements);
        }

        // which author has written the most number of papers?
        if (q3) {
            int maxPapers = (Integer) g.V().has("type", "author").values("numOfPapers").max().next();
            Vertex v = g.V().has("type", "author").has("numOfPapers", maxPapers).next();
            System.out.println("Wrote most paper: " + g.V(v).elementMap().next());
        }

        // which paper is cited most?
        if (q4) {
            int maxCitations = (Integer) g.V().has("type", "paper").values("numOfPaperReferers").max().next();
            Vertex v = g.V().has("type", "paper").has("numOfPaperReferers", maxCitations).next();
            System.out.println("Paper with most citations: " + g.V(v).elementMap().next());
        }

        long duration = (System.currentTimeMillis() - startTime) / 1000;
        System.out.println("elapsed time = " + duration + " seconds");
        graph.close();
    }
}
