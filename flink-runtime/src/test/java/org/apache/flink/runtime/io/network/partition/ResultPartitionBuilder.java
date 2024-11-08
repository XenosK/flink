/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.runtime.io.network.partition;

import org.apache.flink.configuration.NettyShuffleEnvironmentOptions.CompressionCodec;
import org.apache.flink.runtime.clusterframework.types.ResourceID;
import org.apache.flink.runtime.io.disk.BatchShuffleReadBufferPool;
import org.apache.flink.runtime.io.disk.FileChannelManager;
import org.apache.flink.runtime.io.disk.NoOpFileChannelManager;
import org.apache.flink.runtime.io.network.NettyShuffleEnvironment;
import org.apache.flink.runtime.io.network.buffer.BufferPool;
import org.apache.flink.runtime.io.network.buffer.NetworkBufferPool;
import org.apache.flink.runtime.shuffle.ShuffleDescriptor;
import org.apache.flink.util.function.SupplierWithException;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/** Utility class to encapsulate the logic of building a {@link ResultPartition} instance. */
public class ResultPartitionBuilder {

    private ResultPartitionID partitionId = new ResultPartitionID();

    private ResultPartitionType partitionType = ResultPartitionType.PIPELINED;

    private BoundedBlockingSubpartitionType blockingSubpartitionType =
            BoundedBlockingSubpartitionType.AUTO;

    private int partitionIndex = 0;

    private int numberOfPartitions = 1;

    private int numberOfSubpartitions = 1;

    private int numTargetKeyGroups = 1;

    private boolean isBroadcast = false;

    private ResultPartitionManager partitionManager = new ResultPartitionManager();

    private FileChannelManager channelManager = NoOpFileChannelManager.INSTANCE;

    private NetworkBufferPool networkBufferPool = new NetworkBufferPool(2, 1);

    private BatchShuffleReadBufferPool batchShuffleReadBufferPool =
            new BatchShuffleReadBufferPool(64 * 32 * 1024, 32 * 1024);

    private ScheduledExecutorService batchShuffleReadIOExecutor =
            Executors.newSingleThreadScheduledExecutor();

    private int networkBuffersPerChannel = 1;

    private int floatingNetworkBuffersPerGate = 1;

    private int sortShuffleMinBuffers = 100;

    private int sortShuffleMinParallelism = Integer.MAX_VALUE;

    private int maxBuffersPerChannel = Integer.MAX_VALUE;

    private int networkBufferSize = 1;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<SupplierWithException<BufferPool, IOException>> bufferPoolFactory =
            Optional.empty();

    private boolean blockingShuffleCompressionEnabled = false;

    private boolean sslEnabled = false;

    private CompressionCodec compressionCodec = CompressionCodec.LZ4;

    private int maxOverdraftBuffersPerGate = 5;

    public ResultPartitionBuilder setResultPartitionIndex(int partitionIndex) {
        this.partitionIndex = partitionIndex;
        return this;
    }

    public ResultPartitionBuilder setResultPartitionId(ResultPartitionID partitionId) {
        this.partitionId = partitionId;
        return this;
    }

    public ResultPartitionBuilder setResultPartitionType(ResultPartitionType partitionType) {
        this.partitionType = partitionType;
        return this;
    }

    public ResultPartitionBuilder setNumberOfSubpartitions(int numberOfSubpartitions) {
        this.numberOfSubpartitions = numberOfSubpartitions;
        return this;
    }

    public ResultPartitionBuilder setNumTargetKeyGroups(int numTargetKeyGroups) {
        this.numTargetKeyGroups = numTargetKeyGroups;
        return this;
    }

    public ResultPartitionBuilder setResultPartitionManager(
            ResultPartitionManager partitionManager) {
        this.partitionManager = partitionManager;
        return this;
    }

    public ResultPartitionBuilder setFileChannelManager(FileChannelManager channelManager) {
        this.channelManager = channelManager;
        return this;
    }

    public ResultPartitionBuilder setupBufferPoolFactoryFromNettyShuffleEnvironment(
            NettyShuffleEnvironment environment) {
        return setNetworkBuffersPerChannel(
                        environment.getConfiguration().networkBuffersPerChannel())
                .setFloatingNetworkBuffersPerGate(
                        environment.getConfiguration().floatingNetworkBuffersPerGate())
                .setNetworkBufferSize(environment.getConfiguration().networkBufferSize())
                .setNetworkBufferPool(environment.getNetworkBufferPool())
                .setBatchShuffleReadBufferPool(environment.getBatchShuffleReadBufferPool())
                .setBatchShuffleReadIOExecutor(environment.getBatchShuffleReadIOExecutor())
                .setSortShuffleMinBuffers(environment.getConfiguration().sortShuffleMinBuffers())
                .setSortShuffleMinParallelism(
                        environment.getConfiguration().sortShuffleMinParallelism());
    }

    public ResultPartitionBuilder setNetworkBufferPool(NetworkBufferPool networkBufferPool) {
        this.networkBufferPool = networkBufferPool;
        return this;
    }

    public ResultPartitionBuilder setBatchShuffleReadBufferPool(
            BatchShuffleReadBufferPool batchShuffleReadBufferPool) {
        this.batchShuffleReadBufferPool = batchShuffleReadBufferPool;
        return this;
    }

    public ResultPartitionBuilder setBatchShuffleReadIOExecutor(
            ScheduledExecutorService batchShuffleReadIOExecutor) {
        this.batchShuffleReadIOExecutor = batchShuffleReadIOExecutor;
        return this;
    }

    public ResultPartitionBuilder setNetworkBuffersPerChannel(int networkBuffersPerChannel) {
        this.networkBuffersPerChannel = networkBuffersPerChannel;
        return this;
    }

    public ResultPartitionBuilder setFloatingNetworkBuffersPerGate(
            int floatingNetworkBuffersPerGate) {
        this.floatingNetworkBuffersPerGate = floatingNetworkBuffersPerGate;
        return this;
    }

    ResultPartitionBuilder setNetworkBufferSize(int networkBufferSize) {
        this.networkBufferSize = networkBufferSize;
        return this;
    }

    public ResultPartitionBuilder setBufferPoolFactory(
            SupplierWithException<BufferPool, IOException> bufferPoolFactory) {
        this.bufferPoolFactory = Optional.of(bufferPoolFactory);
        return this;
    }

    public ResultPartitionBuilder setBlockingShuffleCompressionEnabled(
            boolean blockingShuffleCompressionEnabled) {
        this.blockingShuffleCompressionEnabled = blockingShuffleCompressionEnabled;
        return this;
    }

    public ResultPartitionBuilder setSortShuffleMinBuffers(int sortShuffleMinBuffers) {
        this.sortShuffleMinBuffers = sortShuffleMinBuffers;
        return this;
    }

    public ResultPartitionBuilder setSortShuffleMinParallelism(int sortShuffleMinParallelism) {
        this.sortShuffleMinParallelism = sortShuffleMinParallelism;
        return this;
    }

    public ResultPartitionBuilder setCompressionCodec(CompressionCodec compressionCodec) {
        this.compressionCodec = compressionCodec;
        return this;
    }

    ResultPartitionBuilder setBoundedBlockingSubpartitionType(
            @SuppressWarnings("SameParameterValue")
                    BoundedBlockingSubpartitionType blockingSubpartitionType) {
        this.blockingSubpartitionType = blockingSubpartitionType;
        return this;
    }

    public ResultPartitionBuilder setSSLEnabled(boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
        return this;
    }

    public ResultPartitionBuilder setMaxOverdraftBuffersPerGate(int maxOverdraftBuffersPerGate) {
        this.maxOverdraftBuffersPerGate = maxOverdraftBuffersPerGate;
        return this;
    }

    public ResultPartitionBuilder setBroadcast(boolean broadcast) {
        isBroadcast = broadcast;
        return this;
    }

    public ResultPartition build() {
        ResultPartitionFactory resultPartitionFactory =
                new ResultPartitionFactory(
                        partitionManager,
                        channelManager,
                        networkBufferPool,
                        batchShuffleReadBufferPool,
                        batchShuffleReadIOExecutor,
                        blockingSubpartitionType,
                        networkBuffersPerChannel,
                        floatingNetworkBuffersPerGate,
                        networkBufferSize,
                        Integer.MAX_VALUE,
                        blockingShuffleCompressionEnabled,
                        compressionCodec,
                        maxBuffersPerChannel,
                        sortShuffleMinBuffers,
                        sortShuffleMinParallelism,
                        sslEnabled,
                        maxOverdraftBuffersPerGate,
                        null);

        SupplierWithException<BufferPool, IOException> factory =
                bufferPoolFactory.orElseGet(
                        () ->
                                resultPartitionFactory.createBufferPoolFactory(
                                        numberOfSubpartitions, partitionType));

        return resultPartitionFactory.create(
                "Result Partition task",
                partitionIndex,
                partitionId,
                partitionType,
                numberOfPartitions,
                numberOfSubpartitions,
                numTargetKeyGroups,
                isBroadcast,
                new TestingShuffleDescriptor(partitionId, new ResourceID("test")),
                factory,
                false);
    }

    private static class TestingShuffleDescriptor implements ShuffleDescriptor {

        private final ResultPartitionID resultPartitionId;

        private final ResourceID location;

        TestingShuffleDescriptor(ResultPartitionID resultPartitionId, ResourceID location) {
            this.resultPartitionId = resultPartitionId;
            this.location = location;
        }

        @Override
        public ResultPartitionID getResultPartitionID() {
            return resultPartitionId;
        }

        @Override
        public Optional<ResourceID> storesLocalResourcesOn() {
            return Optional.of(location);
        }
    }
}
