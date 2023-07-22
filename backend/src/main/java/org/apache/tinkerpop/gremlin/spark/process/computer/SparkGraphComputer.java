/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tinkerpop.gremlin.spark.process.computer;

import org.apache.commons.configuration2.ConfigurationUtils;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.spark.HashPartitioner;
import org.apache.spark.Partitioner;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.launcher.SparkLauncher;
import org.apache.spark.serializer.KryoRegistrator;
import org.apache.spark.serializer.KryoSerializer;
import org.apache.spark.serializer.Serializer;
import org.apache.spark.storage.StorageLevel;
import org.apache.tinkerpop.gremlin.hadoop.Constants;
import org.apache.tinkerpop.gremlin.hadoop.process.computer.AbstractHadoopGraphComputer;
import org.apache.tinkerpop.gremlin.hadoop.process.computer.util.ComputerSubmissionHelper;
import org.apache.tinkerpop.gremlin.hadoop.structure.HadoopConfiguration;
import org.apache.tinkerpop.gremlin.hadoop.structure.HadoopGraph;
import org.apache.tinkerpop.gremlin.hadoop.structure.io.FileSystemStorage;
import org.apache.tinkerpop.gremlin.hadoop.structure.io.GraphFilterAware;
import org.apache.tinkerpop.gremlin.hadoop.structure.io.HadoopPoolShimService;
import org.apache.tinkerpop.gremlin.hadoop.structure.io.VertexWritable;
import org.apache.tinkerpop.gremlin.hadoop.structure.util.ConfUtil;
import org.apache.tinkerpop.gremlin.process.computer.ComputerResult;
import org.apache.tinkerpop.gremlin.process.computer.GraphComputer;
import org.apache.tinkerpop.gremlin.process.computer.MapReduce;
import org.apache.tinkerpop.gremlin.process.computer.Memory;
import org.apache.tinkerpop.gremlin.process.computer.VertexProgram;
import org.apache.tinkerpop.gremlin.process.computer.clone.CloneVertexProgram;
import org.apache.tinkerpop.gremlin.process.computer.util.DefaultComputerResult;
import org.apache.tinkerpop.gremlin.process.computer.util.MapMemory;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategies;
import org.apache.tinkerpop.gremlin.process.traversal.util.TraversalInterruptedException;
import org.apache.tinkerpop.gremlin.spark.process.computer.payload.ViewIncomingPayload;
import org.apache.tinkerpop.gremlin.spark.process.computer.traversal.strategy.SparkVertexProgramInterceptor;
import org.apache.tinkerpop.gremlin.spark.process.computer.traversal.strategy.optimization.SparkInterceptorStrategy;
import org.apache.tinkerpop.gremlin.spark.process.computer.traversal.strategy.optimization.SparkSingleIterationStrategy;
import org.apache.tinkerpop.gremlin.spark.process.computer.traversal.strategy.optimization.interceptor.SparkCloneVertexProgramInterceptor;
import org.apache.tinkerpop.gremlin.spark.structure.Spark;
import org.apache.tinkerpop.gremlin.spark.structure.io.InputFormatRDD;
import org.apache.tinkerpop.gremlin.spark.structure.io.InputOutputHelper;
import org.apache.tinkerpop.gremlin.spark.structure.io.InputRDD;
import org.apache.tinkerpop.gremlin.spark.structure.io.OutputFormatRDD;
import org.apache.tinkerpop.gremlin.spark.structure.io.OutputRDD;
import org.apache.tinkerpop.gremlin.spark.structure.io.PersistedInputRDD;
import org.apache.tinkerpop.gremlin.spark.structure.io.PersistedOutputRDD;
import org.apache.tinkerpop.gremlin.spark.structure.io.SparkContextStorage;
import org.apache.tinkerpop.gremlin.spark.structure.io.SparkIOUtil;
import org.apache.tinkerpop.gremlin.spark.structure.io.gryo.GryoRegistrator;
import org.apache.tinkerpop.gremlin.spark.structure.io.gryo.kryoshim.unshaded.UnshadedKryoShimService;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.io.IoRegistry;
import org.apache.tinkerpop.gremlin.structure.io.Storage;
import org.apache.tinkerpop.gremlin.structure.io.gryo.kryoshim.KryoShimServiceLoader;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import static org.apache.tinkerpop.gremlin.hadoop.Constants.GREMLIN_SPARK_GRAPH_STORAGE_LEVEL;
import static org.apache.tinkerpop.gremlin.hadoop.Constants.GREMLIN_SPARK_PERSIST_CONTEXT;
import static org.apache.tinkerpop.gremlin.hadoop.Constants.GREMLIN_SPARK_PERSIST_STORAGE_LEVEL;
import static org.apache.tinkerpop.gremlin.hadoop.Constants.GREMLIN_SPARK_SKIP_GRAPH_CACHE;
import static org.apache.tinkerpop.gremlin.hadoop.Constants.GREMLIN_SPARK_SKIP_PARTITIONER;
import static org.apache.tinkerpop.gremlin.hadoop.Constants.SPARK_KRYO_REGISTRATION_REQUIRED;
import static org.apache.tinkerpop.gremlin.hadoop.Constants.SPARK_SERIALIZER;

/**
 * {@link GraphComputer} implementation for Apache Spark.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public final class SparkGraphComputer extends AbstractHadoopGraphComputer {

    private final org.apache.commons.configuration2.Configuration sparkConfiguration;
    private boolean workersSet = false;
    private final ThreadFactory threadFactoryBoss = new BasicThreadFactory.Builder().namingPattern(SparkGraphComputer.class.getSimpleName() + "-boss").build();

    private static final Set<String> KEYS_PASSED_IN_JVM_SYSTEM_PROPERTIES = new HashSet<>(Arrays.asList(
        KryoShimServiceLoader.KRYO_SHIM_SERVICE,
        IoRegistry.IO_REGISTRY));

    /**
     * An {@code ExecutorService} that schedules up background work. Since a {@link GraphComputer} is only used once
     * for a {@link VertexProgram} a single threaded executor is sufficient.
     */
    private final ExecutorService computerService = Executors.newSingleThreadExecutor(threadFactoryBoss);

    static {
        TraversalStrategies.GlobalCache.registerStrategies(SparkGraphComputer.class,
            TraversalStrategies.GlobalCache.getStrategies(GraphComputer.class).clone().addStrategies(
                SparkSingleIterationStrategy.instance(),
                SparkInterceptorStrategy.instance()));
    }

    public SparkGraphComputer(final HadoopGraph hadoopGraph) {
        super(hadoopGraph);
        this.sparkConfiguration = new HadoopConfiguration();
    }

    /**
     * Sets the number of workers. If the {@code spark.master} configuration is configured with "local" then it will
     * change that configuration to use the specified number of worker threads.
     */
    @Override
    public SparkGraphComputer workers(final int workers) {
        super.workers(workers);
        if (this.sparkConfiguration.containsKey(SparkLauncher.SPARK_MASTER) && this.sparkConfiguration.getString(SparkLauncher.SPARK_MASTER).startsWith("local")) {
            this.sparkConfiguration.setProperty(SparkLauncher.SPARK_MASTER, "local[" + this.workers + "]");
        }
        this.workersSet = true;
        return this;
    }

    @Override
    public SparkGraphComputer configure(final String key, final Object value) {
        this.sparkConfiguration.setProperty(key, value);
        return this;
    }

    /**
     * Sets the configuration option for {@code spark.master} which is the cluster manager to connect to which may be
     * one of the <a href="https://spark.apache.org/docs/latest/submitting-applications.html#master-urls">allowed master URLs</a>.
     */
    public SparkGraphComputer master(final String clusterManager) {
        return configure(SparkLauncher.SPARK_MASTER, clusterManager);
    }

    /**
     * Determines if the Spark context should be left open preventing Spark from garbage collecting unreferenced RDDs.
     */
    public SparkGraphComputer persistContext(final boolean persist) {
        return configure(GREMLIN_SPARK_PERSIST_CONTEXT, persist);
    }

    /**
     * Specifies the method by which the {@link VertexProgram} created graph is persisted. By default, it is configured
     * to use {@code StorageLevel#MEMORY_ONLY()}
     */
    public SparkGraphComputer graphStorageLevel(final StorageLevel storageLevel) {
        return configure(GREMLIN_SPARK_GRAPH_STORAGE_LEVEL, storageLevel.description());
    }

    public SparkGraphComputer persistStorageLevel(final StorageLevel storageLevel) {
        return configure(GREMLIN_SPARK_PERSIST_STORAGE_LEVEL, storageLevel.description());
    }

    /**
     * Determines if the graph RDD should be partitioned or not. By default, this value is {@code false}.
     */
    public SparkGraphComputer skipPartitioner(final boolean skip) {
        return configure(GREMLIN_SPARK_SKIP_PARTITIONER, skip);
    }

    /**
     * Determines if the graph RDD should be cached or not. If {@code true} then
     * {@link #graphStorageLevel(StorageLevel)} is ignored. By default, this value is {@code false}.
     */
    public SparkGraphComputer skipGraphCache(final boolean skip) {
        return configure(GREMLIN_SPARK_SKIP_GRAPH_CACHE, skip);
    }

    /**
     * Specifies the {@code org.apache.spark.serializer.Serializer} implementation to use. By default, this value is
     * set to {@code org.apache.spark.serializer.KryoSerializer}.
     */
    public SparkGraphComputer serializer(final Class<? extends Serializer> serializer) {
        return configure(SPARK_SERIALIZER, serializer.getCanonicalName());
    }

    /**
     * Specifies the {@code org.apache.spark.serializer.KryoRegistrator} to use to install additional types. By
     * default this value is set to TinkerPop's {@link GryoRegistrator}.
     */
    public SparkGraphComputer sparkKryoRegistrator(final Class<? extends KryoRegistrator> registrator) {
        return configure(Constants.SPARK_KRYO_REGISTRATOR, registrator.getCanonicalName());
    }

    /**
     * Determines if kryo registration is required such that attempts to serialize classes that are not registered
     * will result in an error. By default this value is {@code false}.
     */
    public SparkGraphComputer kryoRegistrationRequired(final boolean required) {
        return configure(SPARK_KRYO_REGISTRATION_REQUIRED, required);
    }

    @Override
    public Future<ComputerResult> submit() {
        this.validateStatePriorToExecution();
        return ComputerSubmissionHelper.runWithBackgroundThread(this::submitWithExecutor, "SparkSubmitter");
    }

    private Future<ComputerResult> submitWithExecutor(Executor exec) {
        // create the completable future
        final Future<ComputerResult> result = computerService.submit(() -> {
            final long startTime = System.currentTimeMillis();
            logger.info("============= DEBUG =============");
            //////////////////////////////////////////////////
            /////// PROCESS SHIM AND SYSTEM PROPERTIES ///////
            //////////////////////////////////////////////////
            ConfigurationUtils.copy(this.hadoopGraph.configuration(), this.sparkConfiguration);
            final String shimService = KryoSerializer.class.getCanonicalName().equals(this.sparkConfiguration.getString(Constants.SPARK_SERIALIZER, null)) ?
                UnshadedKryoShimService.class.getCanonicalName() :
                HadoopPoolShimService.class.getCanonicalName();
            this.sparkConfiguration.setProperty(KryoShimServiceLoader.KRYO_SHIM_SERVICE, shimService);
            ///////////
            final StringBuilder params = new StringBuilder();
            this.sparkConfiguration.getKeys().forEachRemaining(key -> {
                if (KEYS_PASSED_IN_JVM_SYSTEM_PROPERTIES.contains(key)) {
                    params.append(" -D").append("tinkerpop.").append(key).append("=").append(this.sparkConfiguration.getProperty(key));
                    System.setProperty("tinkerpop." + key, this.sparkConfiguration.getProperty(key).toString());
                }
            });
            if (params.length() > 0) {
                this.sparkConfiguration.setProperty(SparkLauncher.EXECUTOR_EXTRA_JAVA_OPTIONS,
                    (this.sparkConfiguration.getString(SparkLauncher.EXECUTOR_EXTRA_JAVA_OPTIONS, "") + params.toString()).trim());
                this.sparkConfiguration.setProperty(SparkLauncher.DRIVER_EXTRA_JAVA_OPTIONS,
                    (this.sparkConfiguration.getString(SparkLauncher.DRIVER_EXTRA_JAVA_OPTIONS, "") + params.toString()).trim());
            }
            KryoShimServiceLoader.applyConfiguration(this.sparkConfiguration);
            //////////////////////////////////////////////////
            //////////////////////////////////////////////////
            //////////////////////////////////////////////////
            // apache and hadoop configurations that are used throughout the graph computer computation
            final org.apache.commons.configuration2.Configuration graphComputerConfiguration = new HadoopConfiguration(this.sparkConfiguration);
            if (!graphComputerConfiguration.containsKey(Constants.SPARK_SERIALIZER)) {
                graphComputerConfiguration.setProperty(Constants.SPARK_SERIALIZER, KryoSerializer.class.getCanonicalName());
                if (!graphComputerConfiguration.containsKey(Constants.SPARK_KRYO_REGISTRATOR))
                    graphComputerConfiguration.setProperty(Constants.SPARK_KRYO_REGISTRATOR, GryoRegistrator.class.getCanonicalName());
            }
            graphComputerConfiguration.setProperty(Constants.GREMLIN_HADOOP_GRAPH_WRITER_HAS_EDGES, this.persist.equals(GraphComputer.Persist.EDGES));
            final Configuration hadoopConfiguration = ConfUtil.makeHadoopConfiguration(graphComputerConfiguration);
            final Storage fileSystemStorage = FileSystemStorage.open(hadoopConfiguration);
            final boolean inputFromHDFS = FileInputFormat.class.isAssignableFrom(hadoopConfiguration.getClass(Constants.GREMLIN_HADOOP_GRAPH_READER, Object.class));
            final boolean inputFromSpark = PersistedInputRDD.class.isAssignableFrom(hadoopConfiguration.getClass(Constants.GREMLIN_HADOOP_GRAPH_READER, Object.class));
            final boolean outputToHDFS = FileOutputFormat.class.isAssignableFrom(hadoopConfiguration.getClass(Constants.GREMLIN_HADOOP_GRAPH_WRITER, Object.class));
            final boolean outputToSpark = PersistedOutputRDD.class.isAssignableFrom(hadoopConfiguration.getClass(Constants.GREMLIN_HADOOP_GRAPH_WRITER, Object.class));
            final boolean skipPartitioner = graphComputerConfiguration.getBoolean(GREMLIN_SPARK_SKIP_PARTITIONER, false);
            final boolean skipPersist = graphComputerConfiguration.getBoolean(GREMLIN_SPARK_SKIP_GRAPH_CACHE, false);
            if (inputFromHDFS) {
                String inputLocation = Constants
                    .getSearchGraphLocation(hadoopConfiguration.get(Constants.GREMLIN_HADOOP_INPUT_LOCATION),
                        fileSystemStorage).orElse(null);
                if (null != inputLocation) {
                    try {
                        graphComputerConfiguration.setProperty(Constants.MAPREDUCE_INPUT_FILEINPUTFORMAT_INPUTDIR,
                            FileSystem.get(hadoopConfiguration).getFileStatus(new Path(inputLocation)).getPath()
                                .toString());
                        hadoopConfiguration.set(Constants.MAPREDUCE_INPUT_FILEINPUTFORMAT_INPUTDIR,
                            FileSystem.get(hadoopConfiguration).getFileStatus(new Path(inputLocation)).getPath()
                                .toString());
                    } catch (final IOException e) {
                        throw new IllegalStateException(e.getMessage(), e);
                    }
                }
            }
            final InputRDD inputRDD = SparkIOUtil.createInputRDD(hadoopConfiguration);
            final boolean filtered;
            // if the input class can filter on load, then set the filters
            if (inputRDD instanceof InputFormatRDD && GraphFilterAware.class.isAssignableFrom(hadoopConfiguration.getClass(Constants.GREMLIN_HADOOP_GRAPH_READER, InputFormat.class, InputFormat.class))) {
                GraphFilterAware.storeGraphFilter(graphComputerConfiguration, hadoopConfiguration, this.graphFilter);
                filtered = false;
            } else if (inputRDD instanceof GraphFilterAware) {
                ((GraphFilterAware) inputRDD).setGraphFilter(this.graphFilter);
                filtered = false;
            } else if (this.graphFilter.hasFilter()) {
                filtered = true;
            } else {
                filtered = false;
            }

            final OutputRDD outputRDD;
            try {
                outputRDD = OutputRDD.class.isAssignableFrom(hadoopConfiguration.getClass(Constants.GREMLIN_HADOOP_GRAPH_WRITER, Object.class)) ?
                    hadoopConfiguration.getClass(Constants.GREMLIN_HADOOP_GRAPH_WRITER, OutputRDD.class, OutputRDD.class).newInstance() :
                    OutputFormatRDD.class.newInstance();
            } catch (final InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }

            // create the spark context from the graph computer configuration
            final JavaSparkContext sparkContext = new JavaSparkContext(Spark.create(hadoopConfiguration));
            final Storage sparkContextStorage = SparkContextStorage.open();

            SparkMemory memory = null;
            // delete output location
            final String outputLocation = hadoopConfiguration.get(Constants.GREMLIN_HADOOP_OUTPUT_LOCATION, null);
            if (null != outputLocation) {
                if (outputToHDFS && fileSystemStorage.exists(outputLocation))
                    fileSystemStorage.rm(outputLocation);
                if (outputToSpark && sparkContextStorage.exists(outputLocation))
                    sparkContextStorage.rm(outputLocation);
            }

            // the Spark application name will always be set by SparkContextStorage, thus, INFO the name to make it easier to debug
            logger.debug(Constants.GREMLIN_HADOOP_SPARK_JOB_PREFIX + (null == this.vertexProgram ? "No VertexProgram" : this.vertexProgram) + "[" + this.mapReducers + "]");

            // execute the vertex program and map reducers and if there is a failure, auto-close the spark context
            try {
                this.loadJars(hadoopConfiguration, sparkContext); // add the project jars to the cluster
                updateLocalConfiguration(sparkContext, hadoopConfiguration);
                // create a message-passing friendly rdd from the input rdd
                boolean partitioned = false;
                JavaPairRDD<Object, VertexWritable> loadedGraphRDD = SparkIOUtil.loadVertices(inputRDD, graphComputerConfiguration, sparkContext);
                // if there are vertex or edge filters, filter the loaded graph rdd prior to partitioning and persisting
                if (filtered) {
                    this.logger.debug("Filtering the loaded graphRDD: " + this.graphFilter);
                    loadedGraphRDD = SparkExecutor.applyGraphFilter(loadedGraphRDD, this.graphFilter);
                }
                // if the loaded graph RDD is already partitioned use that partitioner, else partition it with HashPartitioner
                if (loadedGraphRDD.partitioner().isPresent())
                    this.logger.debug("Using the existing partitioner associated with the loaded graphRDD: " + loadedGraphRDD.partitioner().get());
                else {
                    if (!skipPartitioner) {
                        final Partitioner partitioner = new HashPartitioner(this.workersSet ? this.workers : loadedGraphRDD.partitions().size());
                        this.logger.debug("Partitioning the loaded graphRDD: " + partitioner);
                        loadedGraphRDD = loadedGraphRDD.partitionBy(partitioner);
                        partitioned = true;
                        assert loadedGraphRDD.partitioner().isPresent();
                    } else {
                        assert skipPartitioner == !loadedGraphRDD.partitioner().isPresent(); // no easy way to test this with a test case
                        this.logger.debug("Partitioning has been skipped for the loaded graphRDD via " + GREMLIN_SPARK_SKIP_PARTITIONER);
                    }
                }
                // if the loaded graphRDD was already partitioned previous, then this coalesce/repartition will not take place
                if (this.workersSet) {
                    if (loadedGraphRDD.partitions().size() > this.workers) // ensures that the loaded graphRDD does not have more partitions than workers
                        loadedGraphRDD = loadedGraphRDD.coalesce(this.workers);
                    else if (loadedGraphRDD.partitions().size() < this.workers) // ensures that the loaded graphRDD does not have less partitions than workers
                        loadedGraphRDD = loadedGraphRDD.repartition(this.workers);
                }
                // persist the vertex program loaded graph as specified by configuration or else use default cache() which is MEMORY_ONLY
                if (!skipPersist && (!inputFromSpark || partitioned || filtered))
                    loadedGraphRDD = loadedGraphRDD.persist(StorageLevel.fromString(hadoopConfiguration.get(GREMLIN_SPARK_GRAPH_STORAGE_LEVEL, "MEMORY_ONLY")));

                // final graph with view (for persisting and/or mapReducing -- may be null and thus, possible to save space/time)
                JavaPairRDD<Object, VertexWritable> computedGraphRDD = null;
                ////////////////////////////////
                // process the vertex program //
                ////////////////////////////////
                if (null != this.vertexProgram) {
                    memory = new SparkMemory(this.vertexProgram, this.mapReducers, sparkContext);
                    // build a shortcut (which reduces the total Spark stages from 3 to 2) for CloneVertexProgram since it does nothing
                    // and this improves the overall performance a lot
                    if (this.vertexProgram.getClass().equals(CloneVertexProgram.class) &&
                        !graphComputerConfiguration.containsKey(Constants.GREMLIN_HADOOP_VERTEX_PROGRAM_INTERCEPTOR)) {
                        graphComputerConfiguration.setProperty(Constants.GREMLIN_HADOOP_VERTEX_PROGRAM_INTERCEPTOR, SparkCloneVertexProgramInterceptor.class.getName());
                    }
                    /////////////////
                    // if there is a registered VertexProgramInterceptor, use it to bypass the GraphComputer semantics
                    if (graphComputerConfiguration.containsKey(Constants.GREMLIN_HADOOP_VERTEX_PROGRAM_INTERCEPTOR)) {
                        try {
                            final SparkVertexProgramInterceptor<VertexProgram> interceptor =
                                (SparkVertexProgramInterceptor) Class.forName(graphComputerConfiguration.getString(Constants.GREMLIN_HADOOP_VERTEX_PROGRAM_INTERCEPTOR)).newInstance();
                            computedGraphRDD = interceptor.apply(this.vertexProgram, loadedGraphRDD, memory);
                        } catch (final ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                            throw new IllegalStateException(e.getMessage());
                        }
                    } else {  // standard GraphComputer semantics
                        // get a configuration that will be propagated to all workers
                        final HadoopConfiguration vertexProgramConfiguration = new HadoopConfiguration();
                        this.vertexProgram.storeState(vertexProgramConfiguration);
                        // set up the vertex program and wire up configurations
                        this.vertexProgram.setup(memory);
                        JavaPairRDD<Object, ViewIncomingPayload<Object>> viewIncomingRDD = null;
                        memory.broadcastMemory(sparkContext);
                        // execute the vertex program
                        while (true) {
                            if (Thread.interrupted()) {
                                sparkContext.cancelAllJobs();
                                throw new TraversalInterruptedException();
                            }
                            memory.setInExecute(true);
                            logger.info("###### READY TO START VERTEX PROGRAM ######");
                            viewIncomingRDD = SparkExecutor.executeVertexProgramIteration(loadedGraphRDD, viewIncomingRDD, memory, graphComputerConfiguration, vertexProgramConfiguration);
                            logger.info("###### FINISH ITERATION OF VERTEX PROGRAM ######");
                            memory.setInExecute(false);
                            if (this.vertexProgram.terminate(memory))
                                break;
                            else {
                                memory.incrIteration();
                                memory.broadcastMemory(sparkContext);
                            }
                        }
                        // if the graph will be continued to be used (persisted or mapreduced), then generate a view+graph
                        if ((null != outputRDD && !this.persist.equals(Persist.NOTHING)) || !this.mapReducers.isEmpty()) {
                            computedGraphRDD = SparkExecutor.prepareFinalGraphRDD(loadedGraphRDD, viewIncomingRDD, this.vertexProgram.getVertexComputeKeys());
                            assert null != computedGraphRDD && computedGraphRDD != loadedGraphRDD;
                        } else {
                            // ensure that the computedGraphRDD was not created
                            assert null == computedGraphRDD;
                        }
                    }
                    /////////////////
                    memory.complete(); // drop all transient memory keys
                    // write the computed graph to the respective output (rdd or output format)
                    if (null != outputRDD && !this.persist.equals(Persist.NOTHING)) {
                        assert null != computedGraphRDD; // the logic holds that a computeGraphRDD must be created at this point
                        outputRDD.writeGraphRDD(graphComputerConfiguration, computedGraphRDD);
                    }
                }

                final boolean computedGraphCreated = computedGraphRDD != null && computedGraphRDD != loadedGraphRDD;
                if (!computedGraphCreated)
                    computedGraphRDD = loadedGraphRDD;

                final Memory.Admin finalMemory = null == memory ? new MapMemory() : new MapMemory(memory);

                //////////////////////////////
                // process the map reducers //
                //////////////////////////////
                if (!this.mapReducers.isEmpty()) {
                    // create a mapReduceRDD for executing the map reduce jobs on
                    JavaPairRDD<Object, VertexWritable> mapReduceRDD = computedGraphRDD;
                    if (computedGraphCreated && !outputToSpark) {
                        // drop all the edges of the graph as they are not used in mapReduce processing
                        mapReduceRDD = computedGraphRDD.mapValues(vertexWritable -> {
                            vertexWritable.get().dropEdges(Direction.BOTH);
                            return vertexWritable;
                        });
                        // if there is only one MapReduce to execute, don't bother wasting the clock cycles.
                        if (this.mapReducers.size() > 1)
                            mapReduceRDD = mapReduceRDD.persist(StorageLevel.fromString(hadoopConfiguration.get(GREMLIN_SPARK_GRAPH_STORAGE_LEVEL, "MEMORY_ONLY")));
                    }

                    for (final MapReduce mapReduce : this.mapReducers) {
                        // execute the map reduce job
                        final HadoopConfiguration newApacheConfiguration = new HadoopConfiguration(graphComputerConfiguration);
                        mapReduce.storeState(newApacheConfiguration);
                        // map
                        final JavaPairRDD mapRDD = SparkExecutor.executeMap((JavaPairRDD) mapReduceRDD, mapReduce, newApacheConfiguration);
                        // combine
                        final JavaPairRDD combineRDD = mapReduce.doStage(MapReduce.Stage.COMBINE) ? SparkExecutor.executeCombine(mapRDD, newApacheConfiguration) : mapRDD;
                        // reduce
                        final JavaPairRDD reduceRDD = mapReduce.doStage(MapReduce.Stage.REDUCE) ? SparkExecutor.executeReduce(combineRDD, mapReduce, newApacheConfiguration) : combineRDD;
                        // write the map reduce output back to disk and computer result memory
                        if (null != outputRDD)
                            mapReduce.addResultToMemory(finalMemory, outputRDD.writeMemoryRDD(graphComputerConfiguration, mapReduce.getMemoryKey(), reduceRDD));
                    }
                    // if the mapReduceRDD is not simply the computed graph, unpersist the mapReduceRDD
                    if (computedGraphCreated && !outputToSpark) {
                        assert loadedGraphRDD != computedGraphRDD;
                        assert mapReduceRDD != computedGraphRDD;
                        mapReduceRDD.unpersist();
                    } else {
                        assert mapReduceRDD == computedGraphRDD;
                    }
                }

                // unpersist the loaded graph if it will not be used again (no PersistedInputRDD)
                // if the graphRDD was loaded from Spark, but then partitioned or filtered, its a different RDD
                if (!inputFromSpark || partitioned || filtered)
                    loadedGraphRDD.unpersist();
                // unpersist the computed graph if it will not be used again (no PersistedOutputRDD)
                // if the computed graph is the loadedGraphRDD because it was not mutated and not-unpersisted, then don't unpersist the computedGraphRDD/loadedGraphRDD
                if ((!outputToSpark || this.persist.equals(GraphComputer.Persist.NOTHING)) && computedGraphCreated)
                    computedGraphRDD.unpersist();
                // delete any file system or rdd data if persist nothing
                if (null != outputLocation && this.persist.equals(GraphComputer.Persist.NOTHING)) {
                    if (outputToHDFS)
                        fileSystemStorage.rm(outputLocation);
                    if (outputToSpark)
                        sparkContextStorage.rm(outputLocation);
                }
                // update runtime and return the newly computed graph
                finalMemory.setRuntime(System.currentTimeMillis() - startTime);
                // clear properties that should not be propagated in an OLAP chain
                graphComputerConfiguration.clearProperty(Constants.GREMLIN_HADOOP_GRAPH_FILTER);
                graphComputerConfiguration.clearProperty(Constants.GREMLIN_HADOOP_VERTEX_PROGRAM_INTERCEPTOR);
                graphComputerConfiguration.clearProperty(GREMLIN_SPARK_SKIP_GRAPH_CACHE);
                graphComputerConfiguration.clearProperty(GREMLIN_SPARK_SKIP_PARTITIONER);
                return new DefaultComputerResult(InputOutputHelper.getOutputGraph(graphComputerConfiguration, this.resultGraph, this.persist), finalMemory.asImmutable());
            } finally {
                if (!graphComputerConfiguration.getBoolean(GREMLIN_SPARK_PERSIST_CONTEXT, false))
                    Spark.close();
            }
        });
        computerService.shutdown();
        return result;
    }

    /////////////////

    @Override
    protected void loadJar(final Configuration hadoopConfiguration, final File file, final Object... params) {
        final JavaSparkContext sparkContext = (JavaSparkContext) params[0];
        sparkContext.addJar(file.getAbsolutePath());
    }

    /**
     * When using a persistent context the running Context's configuration will override a passed
     * in configuration. Spark allows us to override these inherited properties via
     * SparkContext.setLocalProperty
     */
    private void updateLocalConfiguration(final JavaSparkContext sparkContext, final Configuration configuration) {
        /*
         * While we could enumerate over the entire SparkConfiguration and copy into the Thread
         * Local properties of the Spark Context this could cause adverse effects with future
         * versions of Spark. Since the api for setting multiple local properties at once is
         * restricted as private, we will only set those properties we know can effect SparkGraphComputer
         * Execution rather than applying the entire configuration.
         */
        final String[] validPropertyNames = {
            "spark.job.description",
            "spark.jobGroup.id",
            "spark.job.interruptOnCancel",
            "spark.scheduler.pool"
        };

        for (String propertyName : validPropertyNames) {
            String propertyValue = configuration.get(propertyName);
            if (propertyValue != null) {
                this.logger.info("Setting Thread Local SparkContext Property - "
                    + propertyName + " : " + propertyValue);
                sparkContext.setLocalProperty(propertyName, configuration.get(propertyName));
            }
        }
    }

    public static void main(final String[] args) throws Exception {
        final Configurations configs = new Configurations();
        final org.apache.commons.configuration2.Configuration configuration = configs.properties(args[0]);
        new SparkGraphComputer(HadoopGraph.open(configuration)).program(VertexProgram.createVertexProgram(HadoopGraph.open(configuration), configuration)).submit().get();
    }
}
