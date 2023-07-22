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

import org.apache.spark.api.java.Optional;
import org.apache.commons.configuration2.Configuration;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.tinkerpop.gremlin.hadoop.structure.HadoopGraph;
import org.apache.tinkerpop.gremlin.hadoop.structure.io.VertexWritable;
import org.apache.tinkerpop.gremlin.process.computer.GraphFilter;
import org.apache.tinkerpop.gremlin.process.computer.MapReduce;
import org.apache.tinkerpop.gremlin.process.computer.MessageCombiner;
import org.apache.tinkerpop.gremlin.process.computer.VertexComputeKey;
import org.apache.tinkerpop.gremlin.process.computer.VertexProgram;
import org.apache.tinkerpop.gremlin.process.computer.util.ComputerGraph;
import org.apache.tinkerpop.gremlin.process.computer.util.VertexProgramHelper;
import org.apache.tinkerpop.gremlin.spark.process.computer.payload.MessagePayload;
import org.apache.tinkerpop.gremlin.spark.process.computer.payload.Payload;
import org.apache.tinkerpop.gremlin.spark.process.computer.payload.ViewIncomingPayload;
import org.apache.tinkerpop.gremlin.spark.process.computer.payload.ViewOutgoingPayload;
import org.apache.tinkerpop.gremlin.spark.process.computer.payload.ViewPayload;
import org.apache.tinkerpop.gremlin.structure.io.gryo.kryoshim.KryoShimServiceLoader;
import org.apache.tinkerpop.gremlin.structure.util.Attachable;
import org.apache.tinkerpop.gremlin.structure.util.detached.DetachedFactory;
import org.apache.tinkerpop.gremlin.structure.util.detached.DetachedVertexProperty;
import org.apache.tinkerpop.gremlin.structure.util.star.StarGraph;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public final class SparkExecutor {

    private SparkExecutor() {
    }

    //////////////////
    // DATA LOADING //
    //////////////////

    public static JavaPairRDD<Object, VertexWritable> applyGraphFilter(final JavaPairRDD<Object, VertexWritable> graphRDD, final GraphFilter graphFilter) {
        return graphRDD.mapPartitionsToPair(partitionIterator -> {
            final GraphFilter gFilter = graphFilter.clone();
            return IteratorUtils.filter(partitionIterator, tuple -> (tuple._2().get().applyGraphFilter(gFilter)).isPresent());
        }, true);
    }


    ////////////////////
    // VERTEX PROGRAM //
    ////////////////////

    public static <M> JavaPairRDD<Object, ViewIncomingPayload<M>> executeVertexProgramIteration(
        final JavaPairRDD<Object, VertexWritable> graphRDD,
        final JavaPairRDD<Object, ViewIncomingPayload<M>> viewIncomingRDD,
        final SparkMemory memory,
        final Configuration graphComputerConfiguration,    // has the Graph/GraphComputer.configuration() information
        final Configuration vertexProgramConfiguration) { // has the VertexProgram.loadState() information

        boolean partitionedGraphRDD = graphRDD.partitioner().isPresent();

        if (partitionedGraphRDD && null != viewIncomingRDD) // the graphRDD and the viewRDD must have the same partitioner
            assert graphRDD.partitioner().get().equals(viewIncomingRDD.partitioner().get());
        final JavaPairRDD<Object, ViewOutgoingPayload<M>> viewOutgoingRDD = ((null == viewIncomingRDD) ?
            graphRDD.mapValues(vertexWritable -> new Tuple2<>(vertexWritable, Optional.<ViewIncomingPayload<M>>absent())) : // first iteration will not have any views or messages
            graphRDD.leftOuterJoin(viewIncomingRDD))                                                   // every other iteration may have views and messages
            // for each partition of vertices emit a view and their outgoing messages
            .mapPartitionsToPair(partitionIterator -> {
                KryoShimServiceLoader.applyConfiguration(graphComputerConfiguration);

                // if the partition is empty, return without starting a new VP iteration
                if (!partitionIterator.hasNext())
                    return Collections.emptyIterator();

                final VertexProgram<M> workerVertexProgram = VertexProgram.createVertexProgram(HadoopGraph.open(graphComputerConfiguration), vertexProgramConfiguration); // each partition(Spark)/worker(TP3) has a local copy of the vertex program (a worker's task)
                final String[] vertexComputeKeysArray = VertexProgramHelper.vertexComputeKeysAsArray(workerVertexProgram.getVertexComputeKeys()); // the compute keys as an array
                final SparkMessenger<M> messenger = new SparkMessenger<>();

                workerVertexProgram.workerIterationStart(memory.asImmutable()); // start the worker
                return IteratorUtils.map(partitionIterator, vertexViewIncoming -> {
                    final StarGraph.StarVertex vertex = vertexViewIncoming._2()._1().get(); // get the vertex from the vertex writable
                    final boolean hasViewAndMessages = vertexViewIncoming._2()._2().isPresent(); // if this is the first iteration, then there are no views or messages
                    final List<DetachedVertexProperty<Object>> previousView = hasViewAndMessages ? vertexViewIncoming._2()._2().get().getView() : memory.isInitialIteration() ? new ArrayList<>() : Collections.emptyList();
                    // revive compute properties if they already exist
                    if (memory.isInitialIteration() && vertexComputeKeysArray.length > 0)
                        vertex.properties(vertexComputeKeysArray).forEachRemaining(vertexProperty -> previousView.add(DetachedFactory.detach(vertexProperty, true)));
                    // drop any computed properties that are cached in memory
                    vertex.dropVertexProperties(vertexComputeKeysArray);
                    final List<M> incomingMessages = hasViewAndMessages ? vertexViewIncoming._2()._2().get().getIncomingMessages() : Collections.emptyList();
                    IteratorUtils.removeOnNext(previousView.iterator()).forEachRemaining(property -> property.attach(Attachable.Method.create(vertex)));  // attach the view to the vertex
                    assert previousView.isEmpty();
                    // do the vertex's vertex program iteration
                    messenger.setVertexAndIncomingMessages(vertex, incomingMessages); // set the messenger with the incoming messages
                    workerVertexProgram.execute(ComputerGraph.vertexProgram(vertex, workerVertexProgram), messenger, memory); // execute the vertex program on this vertex for this iteration
                    // assert incomingMessages.isEmpty();  // maybe the program didn't read all the messages
                    incomingMessages.clear();
                    // detached the compute property view from the vertex
                    final List<DetachedVertexProperty<Object>> nextView = vertexComputeKeysArray.length == 0 ?  // not all vertex programs have compute keys
                        Collections.emptyList() :
                        IteratorUtils.list(IteratorUtils.map(vertex.properties(vertexComputeKeysArray), vertexProperty -> DetachedFactory.detach(vertexProperty, true)));
                    // drop compute property view as it has now been detached from the vertex
                    vertex.dropVertexProperties(vertexComputeKeysArray);
                    final List<Tuple2<Object, M>> outgoingMessages = messenger.getOutgoingMessages(); // get the outgoing messages being sent by this vertex
                    if (!partitionIterator.hasNext())
                        workerVertexProgram.workerIterationEnd(memory.asImmutable()); // if no more vertices in the partition, end the worker's iteration
                    return (nextView.isEmpty() && outgoingMessages.isEmpty()) ?
                        null : // if there is no view nor outgoing messages, emit nothing
                        new Tuple2<>(vertex.id(), new ViewOutgoingPayload<>(nextView, outgoingMessages));  // else, emit the vertex id, its view, and its outgoing messages
                });
            }, true)  // true means that the partition is preserved
            .filter(tuple -> null != tuple); // if there are no messages or views, then the tuple is null (memory optimization)
        // the graphRDD and the viewRDD must have the same partitioner
        if (partitionedGraphRDD)
            assert graphRDD.partitioner().get().equals(viewOutgoingRDD.partitioner().get());
        /////////////////////////////////////////////////////////////
        /////////////////////////////////////////////////////////////
        final PairFlatMapFunction<Tuple2<Object, ViewOutgoingPayload<M>>, Object, Payload> messageFunction =
            tuple -> IteratorUtils.concat(
                IteratorUtils.of(new Tuple2<>(tuple._1(), tuple._2().getView())),      // emit the view payload
                IteratorUtils.map(tuple._2().getOutgoingMessages().iterator(), message -> new Tuple2<>(message._1(), new MessagePayload<>(message._2()))));
        final MessageCombiner<M> messageCombiner = VertexProgram.<VertexProgram<M>>createVertexProgram(HadoopGraph.open(vertexProgramConfiguration), vertexProgramConfiguration).getMessageCombiner().orElse(null);
        final Function2<Payload, Payload, Payload> reducerFunction = (a, b) -> {      // reduce the view and outgoing messages into a single payload object representing the new view and incoming messages for a vertex
            if (a instanceof ViewIncomingPayload) {
                ((ViewIncomingPayload<M>) a).mergePayload(b, messageCombiner);
                return a;
            } else if (b instanceof ViewIncomingPayload) {
                ((ViewIncomingPayload<M>) b).mergePayload(a, messageCombiner);
                return b;
            } else {
                final ViewIncomingPayload<M> c = new ViewIncomingPayload<>(messageCombiner);
                c.mergePayload(a, messageCombiner);
                c.mergePayload(b, messageCombiner);
                return c;
            }
        };
        /////////////////////////////////////////////////////////////
        /////////////////////////////////////////////////////////////
        // "message pass" by reducing on the vertex object id of the view and message payloads
        final JavaPairRDD<Object, ViewIncomingPayload<M>> newViewIncomingRDD =
            (partitionedGraphRDD ?
                viewOutgoingRDD.flatMapToPair(messageFunction).reduceByKey(graphRDD.partitioner().get(), reducerFunction) :
                viewOutgoingRDD.flatMapToPair(messageFunction).reduceByKey(reducerFunction))
                .mapValues(payload -> { // handle various corner cases of when views don't exist, messages don't exist, or neither exists.
                    if (payload instanceof ViewIncomingPayload) // this happens if there is a vertex view with incoming messages
                        return (ViewIncomingPayload<M>) payload;
                    else if (payload instanceof ViewPayload)    // this happens if there is a vertex view with no incoming messages
                        return new ViewIncomingPayload<>((ViewPayload) payload);
                    else                                        // this happens when there is a single message to a vertex that has no view or outgoing messages
                        return new ViewIncomingPayload<>((MessagePayload<M>) payload);
                });
        // the graphRDD and the viewRDD must have the same partitioner
        if (partitionedGraphRDD)
            assert graphRDD.partitioner().get().equals(newViewIncomingRDD.partitioner().get());
        newViewIncomingRDD
            .foreachPartition(partitionIterator -> {
                KryoShimServiceLoader.applyConfiguration(graphComputerConfiguration);
            }); // need to complete a task so its BSP and the memory for this iteration is updatedß
        return newViewIncomingRDD;
    }

    public static <M> JavaPairRDD<Object, VertexWritable> prepareFinalGraphRDD(
        final JavaPairRDD<Object, VertexWritable> graphRDD,
        final JavaPairRDD<Object, ViewIncomingPayload<M>> viewIncomingRDD,
        final Set<VertexComputeKey> vertexComputeKeys) {
        // the graphRDD and the viewRDD must have the same partitioner
        if (graphRDD.partitioner().isPresent())
            assert (graphRDD.partitioner().get().equals(viewIncomingRDD.partitioner().get()));
        final String[] vertexComputeKeysArray = VertexProgramHelper.vertexComputeKeysAsArray(vertexComputeKeys); // the compute keys as an array
        return graphRDD.leftOuterJoin(viewIncomingRDD)
            .mapValues(tuple -> {
                final StarGraph.StarVertex vertex = tuple._1().get();
                vertex.dropVertexProperties(vertexComputeKeysArray); // drop all existing compute keys
                // attach the final computed view to the cached graph
                final List<DetachedVertexProperty<Object>> view = tuple._2().isPresent() ? tuple._2().get().getView() : Collections.emptyList();
                for (final DetachedVertexProperty<Object> property : view) {
                    if (!VertexProgramHelper.isTransientVertexComputeKey(property.key(), vertexComputeKeys))
                        property.attach(Attachable.Method.create(vertex));
                }
                return tuple._1();
            });
    }

    /////////////////
    // MAP REDUCE //
    ////////////////

    public static <K, V> JavaPairRDD<K, V> executeMap(
        final JavaPairRDD<Object, VertexWritable> graphRDD, final MapReduce<K, V, ?, ?, ?> mapReduce,
        final Configuration graphComputerConfiguration) {
        JavaPairRDD<K, V> mapRDD = graphRDD.mapPartitionsToPair(partitionIterator -> {
            KryoShimServiceLoader.applyConfiguration(graphComputerConfiguration);
            return new MapIterator<>(MapReduce.<MapReduce<K, V, ?, ?, ?>>createMapReduce(HadoopGraph.open(graphComputerConfiguration), graphComputerConfiguration), partitionIterator);
        });
        if (mapReduce.getMapKeySort().isPresent())
            mapRDD = mapRDD.sortByKey(mapReduce.getMapKeySort().get(), true, 1);
        return mapRDD;
    }

    public static <K, V, OK, OV> JavaPairRDD<OK, OV> executeCombine(final JavaPairRDD<K, V> mapRDD,
                                                                    final Configuration graphComputerConfiguration) {
        return mapRDD.mapPartitionsToPair(partitionIterator -> {
            KryoShimServiceLoader.applyConfiguration(graphComputerConfiguration);
            return new CombineIterator<>(MapReduce.<MapReduce<K, V, OK, OV, ?>>createMapReduce(HadoopGraph.open(graphComputerConfiguration), graphComputerConfiguration), partitionIterator);
        });
    }

    public static <K, V, OK, OV> JavaPairRDD<OK, OV> executeReduce(
        final JavaPairRDD<K, V> mapOrCombineRDD, final MapReduce<K, V, OK, OV, ?> mapReduce,
        final Configuration graphComputerConfiguration) {
        JavaPairRDD<OK, OV> reduceRDD = mapOrCombineRDD.groupByKey().mapPartitionsToPair(partitionIterator -> {
            KryoShimServiceLoader.applyConfiguration(graphComputerConfiguration);
            return new ReduceIterator<>(MapReduce.<MapReduce<K, V, OK, OV, ?>>createMapReduce(HadoopGraph.open(graphComputerConfiguration), graphComputerConfiguration), partitionIterator);
        });
        if (mapReduce.getReduceKeySort().isPresent())
            reduceRDD = reduceRDD.sortByKey(mapReduce.getReduceKeySort().get(), true, 1);
        return reduceRDD;
    }
}