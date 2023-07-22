package io.citegraph.data.spark.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.citegraph.data.model.GraphSONVertex;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

import java.io.IOException;

/**
 * Read GraphSON files and load specified property to the graph
 */
public class GraphSONVertexPropertyLoader {

    public static void main(String[] args) {
        // Initialize SparkConf and JavaSparkContext
        SparkConf sparkConf = new SparkConf().setAppName("JSON Parser").setMaster("local[*]");
        JavaSparkContext jsc = new JavaSparkContext(sparkConf);

        // Read files into JavaRDD
        JavaRDD<String> input = jsc.textFile("/Users/liboxuan/workspace/sparkgraph/paperrank/part-r-*");

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

        data.foreachPartition(partition -> {
            // open JG instance
            partition.forEachRemaining(vertex -> {
                String vid = vertex.getId();
                double pagerank = vertex.getProperties().getPageRank().get(0).getValue().getValue();
                if (pagerank > 1e-4) {
                    System.out.println("########## vid: " + vid + ", pagerank: " + pagerank);
                }
            });
        });

        System.out.println("number of vertices: " + data.count());
    }
}
