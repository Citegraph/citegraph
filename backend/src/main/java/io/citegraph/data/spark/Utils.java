package io.citegraph.data.spark;

import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.spark.launcher.SparkLauncher;
import org.apache.tinkerpop.gremlin.hadoop.Constants;
import org.apache.tinkerpop.gremlin.hadoop.structure.HadoopGraph;
import org.janusgraph.hadoop.formats.cql.CqlInputFormat;

public class Utils {
    public static Configuration getSparkGraphConfig() {
        org.apache.commons.configuration2.Configuration sparkGraphConfiguration = new BaseConfiguration();
        sparkGraphConfiguration.setProperty(Constants.GREMLIN_HADOOP_GRAPH_READER, CqlInputFormat.class.getCanonicalName());
        sparkGraphConfiguration.setProperty("janusgraphmr.ioformat.conf.storage.backend", "cql");
        sparkGraphConfiguration.setProperty("janusgraphmr.ioformat.conf.storage.hostname", "127.0.0.1");
        sparkGraphConfiguration.setProperty("janusgraphmr.ioformat.conf.storage.port", 9042);
        sparkGraphConfiguration.setProperty("janusgraphmr.ioformat.conf.storage.cql.keyspace", "janusgraph");
        sparkGraphConfiguration.setProperty("cassandra.input.partitioner.class", "org.apache.cassandra.dht.Murmur3Partitioner");
        sparkGraphConfiguration.setProperty("gremlin.graph", HadoopGraph.class.getCanonicalName());
        sparkGraphConfiguration.setProperty(SparkLauncher.SPARK_MASTER, "local[*]");
        return sparkGraphConfiguration;
    }
}
