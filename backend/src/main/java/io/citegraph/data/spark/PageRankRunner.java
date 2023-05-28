package io.citegraph.data.spark;

import org.apache.tinkerpop.gremlin.process.computer.ComputerResult;
import org.apache.tinkerpop.gremlin.process.computer.ranking.pagerank.PageRankVertexProgram;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.spark.process.computer.SparkGraphComputer;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.util.GraphFactory;

import static io.citegraph.data.spark.Utils.getSparkGraphConfig;
import static org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource.traversal;

/**
 * This Spark application runs page rank algorithm to calculate
 * a score for each author
 *
 * TODO: persist results
 */
public class PageRankRunner {
    public static void main(String[] args) throws Exception {
        Graph graph = GraphFactory.open(getSparkGraphConfig());
        long startTime = System.currentTimeMillis();
        ComputerResult result = graph.compute(SparkGraphComputer.class).program(
            PageRankVertexProgram.build().edges(__.outE("refers").asAdmin()).create()
        ).submit().get();
        GraphTraversalSource g = traversal().withEmbedded(result.graph());
        long duration = (System.currentTimeMillis() - startTime) / 1000;
        System.out.println("finished PageRank computation, elapsed time = " + duration + " seconds.");
        graph.close();
    }
}
