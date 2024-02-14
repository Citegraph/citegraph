package io.citegraph.data.spark.olap;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.spark.process.computer.SparkGraphComputer;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.util.GraphFactory;

import java.util.Map;

import static io.citegraph.data.spark.Utils.getSparkGraphConfig;

/**
 * This Spark application counts number of vertices and edges stored in the graph
 * by their types
 *
 * 07/15/2023:
 * number of vertices by type is: {paper=5259858, author=5839765}
 * number of edges by type is: {collaborates=21188837, cites=32724482, refers=227942008, writes=16863579}
 */
public class VertexEdgeCountRunner {
    public static void main(String[] args) throws Exception {
        Graph graph = GraphFactory.open(getSparkGraphConfig());
        GraphTraversalSource g = graph.traversal().withComputer(SparkGraphComputer.class);
        long startTime = System.currentTimeMillis();
        Map<Object, Long> numOfVerticesByType = g.V().groupCount().by("type").next();
        Map<Object, Long> numOfEdgesByLabel = g.E().groupCount().by(T.label).next();
        long duration = (System.currentTimeMillis() - startTime) / 1000;
        System.out.println("number of vertices by type is: " + numOfVerticesByType);
        System.out.println("number of edges by type is: " + numOfEdgesByLabel);
        System.out.println("elapsed time = " + duration + " seconds");
        graph.close();
    }
}
