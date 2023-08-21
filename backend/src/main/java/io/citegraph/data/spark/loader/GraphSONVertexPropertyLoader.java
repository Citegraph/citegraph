package io.citegraph.data.spark.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.citegraph.data.GraphInitializer;
import io.citegraph.data.model.GraphSONVertex;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;

import java.io.IOException;
import java.net.URL;

import static io.citegraph.app.GraphConfiguration.GRAPH_CONFIG_NAME;

/**
 * Read GraphSON files and load specified property to the graph
 * For now, it's only used to load cluster id property
 */
public class GraphSONVertexPropertyLoader {

    public static void main(String[] args) {
        // Initialize SparkConf and JavaSparkContext
        SparkConf sparkConf = new SparkConf().setAppName("JSON Parser").setMaster("local[*]");
        JavaSparkContext jsc = new JavaSparkContext(sparkConf);

        // Read files into JavaRDD
        JavaRDD<String> input = jsc.textFile("/Users/liboxuan/workspace/cluster/part-r-*");

        // Parse JSON in each line
        JavaRDD<GraphSONVertex> data = input.map(line -> {
            ObjectMapper mapper = new ObjectMapper();
            try {
                return mapper.readValue(line, GraphSONVertex.class);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        });

        URL resource = GraphInitializer.class.getClassLoader().getResource(GRAPH_CONFIG_NAME);

        data.foreachPartition(partition -> {
            // open JG instance
            JanusGraph graph;
            try {
                graph = JanusGraphFactory.open(resource.toURI().getPath());
            } catch (Exception ex) {
                System.out.println(ex);
                return;
            }

            JanusGraph finalGraph = graph;
            partition.forEachRemaining(vertex -> {
                for (int i = 0; i < 3; i++) {
                    try {
                        String vid = vertex.getId();
                        String cluster = vertex.getProperties().getCluster().get(0).getValue();
                        GraphTraversalSource g = finalGraph.traversal();
                        if (g.V(vid).values("cluster").hasNext()) {
                            return;
                        }
                        g.V(vid).property("cluster", cluster).next();
                        g.tx().commit();
                        return;
                    } catch (Exception ex) {
                        System.out.println("Commit failed, retry count = " + i);
                    }
                }
            });
            System.out.println("Partition finished");
            graph.close();
        });
    }
}
