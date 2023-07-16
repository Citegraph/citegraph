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
import org.apache.tinkerpop.gremlin.structure.util.star.StarGraph;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.hadoop.serialize.JanusGraphKryoRegistrator;

import java.net.URL;
import java.util.Objects;

import static io.citegraph.app.GraphConfiguration.GRAPH_CONFIG_NAME;
import static io.citegraph.data.spark.Utils.getSparkGraphConfig;

/**
 * A Spark program that loads all vertices and edges, does
 * some traversals and counting, and then store the computed
 * results as vertex properties to speed up OLTP read requests.
 *
 * This program is idempotent, and you can safely rerun it.
 *
 * Currently, it creates the following vertex properties:
 * 1) numOfPaperReferees: How many papers the current paper/author has cited
 * 2) numOfPaperReferers: How many papers have cited the current paper/author
 * 3) numOfAuthorReferees: How many authors the current paper/author has cited
 * 4) numOfAuthorReferers: How many authors have cited the current paper/author
 * 5) numOfCoworkers: How many authors the current author has collaborated with
 * 6) numOfPapers: How many papers the current author has written
 */
public class VertexPropertyEnricher {
    public static void main(String[] args) {
        SparkConf sparkConf = new SparkConf().setAppName("Spark Graph")
            .set(SparkLauncher.SPARK_MASTER, "local[*]")
            .set("gremlin.graph", HadoopGraph.class.getCanonicalName())
            .set(Constants.SPARK_SERIALIZER, KryoSerializer.class.getCanonicalName())
            .set(Constants.SPARK_KRYO_REGISTRATOR, JanusGraphKryoRegistrator.class.getCanonicalName());
        JavaSparkContext sparkContext = new JavaSparkContext(sparkConf);

        Configuration sparkGraphConfiguration = getSparkGraphConfig();

        URL resource = GraphInitializer.class.getClassLoader().getResource(GRAPH_CONFIG_NAME);
        assert resource != null;

        // load vertices
        JavaPairRDD<Object, VertexWritable> verticesRDD = SparkIOUtil.loadVertices(sparkGraphConfiguration, sparkContext);
        verticesRDD.values().foreachPartition(partition -> {
            // SparkRDDs are immutable, so we have to use normal JanusGraph transactions to
            // write updated data back to JanusGraph. For each partition, we open a transaction.
            JanusGraph graph;
            try {
                graph = JanusGraphFactory.open(resource.toURI().getPath());
            } catch (Exception ex) {
                ex.printStackTrace();
                return;
            }

            JanusGraph finalGraph = graph;
            partition.forEachRemaining(
                vertexWritable -> {
                    for (int i = 0; i < 3; i++) {
                        try {
                            GraphTraversalSource g = finalGraph.traversal();
                            StarGraph.StarVertex v = vertexWritable.get();
                            if (Objects.equals(v.value("type"), "author")) {
                                long numOfPaperReferees = g.V(v).out("writes").out("cites").count().next();
                                long numOfPaperReferers = g.V(v).out("writes").in("cites").count().next();
                                long numOfAuthorReferees = g.V(v).out("refers").count().next();
                                long numOfAuthorReferers = g.V(v).in("refers").count().next();
                                // FIXME: this does not exclude the author themself
                                long numOfCoworkers = g.V(v).out("writes").in("writes").dedup().count().next();
                                long numOfPapers = g.V(v).out("writes").count().next();
                                g.V(v).property("numOfPaperReferees", numOfPaperReferees)
                                    .property("numOfPaperReferers", numOfPaperReferers)
                                    .property("numOfAuthorReferees", numOfAuthorReferees)
                                    .property("numOfAuthorReferers", numOfAuthorReferers)
                                    .property("numOfCoworkers", numOfCoworkers)
                                    .property("numOfPapers", numOfPapers)
                                    .next();
                            } else if (Objects.equals(v.value("type"), "paper")) {
                                long numOfPaperReferees = g.V(v).out("cites").count().next();
                                long numOfPaperReferers = g.V(v).in("cites").count().next();
                                long numOfAuthorReferees = g.V(v).out("refers").in("writes").count().next();
                                long numOfAuthorReferers = g.V(v).in("refers").in("writes").count().next();
                                g.V(v).property("numOfPaperReferees", numOfPaperReferees)
                                    .property("numOfPaperReferers", numOfPaperReferers)
                                    .property("numOfAuthorReferees", numOfAuthorReferees)
                                    .property("numOfAuthorReferers", numOfAuthorReferers)
                                    .next();
                            }
                            g.tx().commit();
                            // commit successful, break the retry loop
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
