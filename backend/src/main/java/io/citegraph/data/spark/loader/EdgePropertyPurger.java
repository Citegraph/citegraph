package io.citegraph.data.spark.loader;

import io.citegraph.data.GraphInitializer;
import org.apache.commons.configuration2.Configuration;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.launcher.SparkLauncher;
import org.apache.spark.serializer.KryoSerializer;
import org.apache.tinkerpop.gremlin.hadoop.Constants;
import org.apache.tinkerpop.gremlin.hadoop.structure.HadoopGraph;
import org.apache.tinkerpop.gremlin.hadoop.structure.io.VertexWritable;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.spark.structure.io.SparkIOUtil;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.star.StarGraph;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.hadoop.serialize.JanusGraphKryoRegistrator;

import java.net.URL;
import java.util.List;
import java.util.Objects;

import static io.citegraph.app.GraphConfiguration.GRAPH_CONFIG_NAME;
import static io.citegraph.data.spark.Utils.getSparkGraphConfig;

/**
 * This is an adhoc Spark program that purges useless edge properties
 * from the graph as specified
 */
public class EdgePropertyPurger {

    public static void main(String[] args) {
        SparkConf sparkConf = new SparkConf().setAppName("Spark Graph")
            .set(SparkLauncher.SPARK_MASTER, "local[*]")
            .set("gremlin.graph", HadoopGraph.class.getCanonicalName())
            .set(Constants.SPARK_SERIALIZER, KryoSerializer.class.getCanonicalName())
            .set(Constants.SPARK_KRYO_REGISTRATOR, JanusGraphKryoRegistrator.class.getCanonicalName());
        JavaSparkContext sparkContext = new JavaSparkContext(sparkConf);

        Configuration sparkGraphConfiguration = getSparkGraphConfig();

        URL resource = GraphInitializer.class.getClassLoader().getResource(GRAPH_CONFIG_NAME);

        // load vertices
        JavaPairRDD<Object, VertexWritable> verticesRDD = SparkIOUtil.loadVertices(sparkGraphConfiguration, sparkContext);
        verticesRDD.values().foreachPartition(partition -> {
            // SparkRDDs are immutable, so we have to use normal JanusGraph transactions to
            // write updated data back to JanusGraph. For each partition, we open a transaction.
            JanusGraph graph;
            try {
                graph = JanusGraphFactory.open(resource.toURI().getPath());
            } catch (Exception ex) {
                System.out.println(ex);
                return;
            }

            JanusGraph finalGraph = graph;
            partition.forEachRemaining(
                vertexWritable -> {
                    for (int i = 0; i < 3; i++) {
                        try {
                            GraphTraversalSource g = finalGraph.traversal();
                            StarGraph.StarVertex v = vertexWritable.get();
                            if (!Objects.equals(v.value("type"), "author")) return;
                            Vertex fromV = g.V(v.id()).next();
                            List<Object> edgeIds = g.V(fromV).outE("refers").id().toList();
                            int count = 0;
                            for (Object edgeId : edgeIds) {
                                count++;
                                Edge e = g.E(edgeId).next();
                                e.property("name").remove();
                                if (count % 10 == 0) {
                                    g.tx().commit();
                                }
                            }
                            g.tx().commit();
                            // commit successful, break the retry loop
                            return;
                        } catch (Exception ex) {
                            System.out.println("Commit failed, retry count = " + i);
                            try {
                                Thread.sleep((i + 1) * 5000L);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                });
            System.out.println("Partition finished");
            graph.close();
        });
    }
}
