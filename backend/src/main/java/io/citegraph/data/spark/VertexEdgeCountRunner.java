package io.citegraph.data.spark;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.spark.process.computer.SparkGraphComputer;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.util.GraphFactory;

import static io.citegraph.data.spark.Utils.getSparkGraphConfig;

/**
 * This Spark application counts number of vertices and edges stored in the graph
 * by their types
 */
public class VertexEdgeCountRunner {
    public static void main(String[] args) throws Exception {
        Graph graph = GraphFactory.open(getSparkGraphConfig());
        GraphTraversalSource g = graph.traversal().withComputer(SparkGraphComputer.class);
        long startTime = System.currentTimeMillis();
        long numV = g.V().count().next();
        long duration = (System.currentTimeMillis() - startTime) / 1000;
        System.out.println("number of vertices = " + numV + ", elapsed time = " + duration + " seconds.");
        graph.close();
    }
}
