package io.citegraph.data.spark.loader;

import org.apache.commons.configuration2.Configuration;
import org.apache.spark.launcher.SparkLauncher;
import org.apache.tinkerpop.gremlin.hadoop.Constants;
import org.apache.tinkerpop.gremlin.hadoop.structure.io.graphson.GraphSONOutputFormat;
import org.apache.tinkerpop.gremlin.process.computer.ComputerResult;
import org.apache.tinkerpop.gremlin.process.computer.GraphComputer;
import org.apache.tinkerpop.gremlin.process.computer.ranking.pagerank.PageRankVertexProgram;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.spark.process.computer.SparkGraphComputer;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.util.GraphFactory;

import static io.citegraph.data.spark.Utils.getSparkGraphConfig;
import static org.apache.tinkerpop.gremlin.hadoop.Constants.GREMLIN_HADOOP_GRAPH_WRITER;
import static org.apache.tinkerpop.gremlin.hadoop.Constants.GREMLIN_HADOOP_OUTPUT_LOCATION;

/**
 * This Spark application runs page rank algorithm to calculate
 * a score for each paper
 *
 * NOTE: on a single machine with 8 cores and 16GB memory, this
 * program always gets OOM for authors but not for papers
 */
public class PageRankRunner {
    public static void main(String[] args) throws Exception {
        Configuration sparkGraphConfiguration = getSparkGraphConfig();
        sparkGraphConfiguration.setProperty(Constants.GREMLIN_SPARK_GRAPH_STORAGE_LEVEL, "MEMORY_AND_DISK");
        sparkGraphConfiguration.setProperty(GREMLIN_HADOOP_GRAPH_WRITER, GraphSONOutputFormat.class.getCanonicalName());
        sparkGraphConfiguration.setProperty(GREMLIN_HADOOP_OUTPUT_LOCATION, "/Users/liboxuan/workspace/sparkgraph/");
        sparkGraphConfiguration.setProperty(SparkLauncher.EXECUTOR_MEMORY, "1g");
        Graph graph = GraphFactory.open(sparkGraphConfiguration);

        long startTime = System.currentTimeMillis();
        ComputerResult result = graph.compute(SparkGraphComputer.class)
            .vertices(__.has("type", "paper"))
            .edges(__.bothE("cites"))
            .properties(__.properties("type"))
            .persist(GraphComputer.Persist.VERTEX_PROPERTIES)
            .program(PageRankVertexProgram.build()
                .edges(__.outE("cites").asAdmin())
                .property("pagerank")
                .iterations(100)
                .create())
            .submit()
            .get();
        long duration = (System.currentTimeMillis() - startTime) / 1000;
        System.out.println("Result memory: " + result.memory().asMap());
        System.out.println("finished PageRank computation, elapsed time = " + duration + " seconds.");
        graph.close();
    }
}
