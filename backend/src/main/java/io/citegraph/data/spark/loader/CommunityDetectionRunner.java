package io.citegraph.data.spark.loader;

import org.apache.commons.configuration2.Configuration;
import org.apache.spark.launcher.SparkLauncher;
import org.apache.tinkerpop.gremlin.hadoop.Constants;
import org.apache.tinkerpop.gremlin.hadoop.structure.io.graphson.GraphSONOutputFormat;
import org.apache.tinkerpop.gremlin.process.computer.ComputerResult;
import org.apache.tinkerpop.gremlin.process.computer.GraphComputer;
import org.apache.tinkerpop.gremlin.process.computer.clustering.peerpressure.PeerPressureVertexProgram;
import org.apache.tinkerpop.gremlin.process.computer.ranking.pagerank.PageRankVertexProgram;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.spark.process.computer.SparkGraphComputer;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.util.GraphFactory;

import static io.citegraph.data.spark.Utils.getSparkGraphConfig;
import static org.apache.tinkerpop.gremlin.hadoop.Constants.GREMLIN_HADOOP_GRAPH_WRITER;
import static org.apache.tinkerpop.gremlin.hadoop.Constants.GREMLIN_HADOOP_OUTPUT_LOCATION;

/**
 * This Spark application runs PeerPressureVertexProgram to calculate
 * a cluster id for each author based on collaboration edges
 *
 */
public class CommunityDetectionRunner {
    public static void main(String[] args) throws Exception {
        Configuration sparkGraphConfiguration = getSparkGraphConfig();
        sparkGraphConfiguration.setProperty(Constants.GREMLIN_SPARK_GRAPH_STORAGE_LEVEL, "MEMORY_AND_DISK");
        sparkGraphConfiguration.setProperty(GREMLIN_HADOOP_GRAPH_WRITER, GraphSONOutputFormat.class.getCanonicalName());
        sparkGraphConfiguration.setProperty(GREMLIN_HADOOP_OUTPUT_LOCATION, "/Users/liboxuan/workspace/sparkgraph/");
        sparkGraphConfiguration.setProperty(SparkLauncher.EXECUTOR_MEMORY, "1g");
        Graph graph = GraphFactory.open(sparkGraphConfiguration);

        long startTime = System.currentTimeMillis();
        ComputerResult result = graph.compute(SparkGraphComputer.class)
            .vertices(__.has("type", "author"))
            .edges(__.bothE("collaborates"))
            .properties(__.properties("dummy"))
            .persist(GraphComputer.Persist.VERTEX_PROPERTIES)
            .program(PeerPressureVertexProgram.build()
                .edges(__.bothE("collaborates").asAdmin())
                .property("cluster")
                .maxIterations(100)
                .create())
            .submit()
            .get();
        long duration = (System.currentTimeMillis() - startTime) / 1000;
        System.out.println("finished PeerPressure computation, elapsed time = " + duration + " seconds.");
        graph.close();
    }
}
