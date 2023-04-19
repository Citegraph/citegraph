package io.citegraph.data;

import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.launcher.SparkLauncher;
import org.apache.spark.serializer.KryoSerializer;
import org.apache.tinkerpop.gremlin.hadoop.Constants;
import org.apache.tinkerpop.gremlin.hadoop.structure.HadoopGraph;
import org.apache.tinkerpop.gremlin.spark.structure.io.SparkIOUtil;
import org.janusgraph.hadoop.formats.cql.CqlInputFormat;
import org.janusgraph.hadoop.serialize.JanusGraphKryoRegistrator;

public class AuthorRefEdgeLoader {
    public static void main(String[] args) {
        SparkConf sparkConf = new SparkConf().setAppName("Spark Graph")
            .set(SparkLauncher.SPARK_MASTER, "local[*]")
            .set("gremlin.graph", HadoopGraph.class.getCanonicalName())
            .set(Constants.SPARK_SERIALIZER, KryoSerializer.class.getCanonicalName())
            .set(Constants.SPARK_KRYO_REGISTRATOR, JanusGraphKryoRegistrator.class.getCanonicalName());
        JavaSparkContext sparkContext = new JavaSparkContext(sparkConf);

        Configuration sparkGraphConfiguration = new BaseConfiguration();
        sparkGraphConfiguration.setProperty(Constants.GREMLIN_HADOOP_GRAPH_READER, CqlInputFormat.class.getCanonicalName());
        sparkGraphConfiguration.setProperty("janusgraphmr.ioformat.conf.storage.backend", "cql");
        sparkGraphConfiguration.setProperty("janusgraphmr.ioformat.conf.storage.hostname", "127.0.0.1");
        sparkGraphConfiguration.setProperty("janusgraphmr.ioformat.conf.storage.port", 9042);
        sparkGraphConfiguration.setProperty("janusgraphmr.ioformat.conf.storage.cql.keyspace", "janusgraph");
        sparkGraphConfiguration.setProperty("cassandra.input.partitioner.class", "org.apache.cassandra.dht.Murmur3Partitioner");

        // load vertices
        JavaPairRDD verticesRDD = SparkIOUtil.loadVertices(sparkGraphConfiguration, sparkContext);
        System.out.println("number of RDDs:" + verticesRDD.count());
    }
}
