/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.dynamic.config;

import io.atomix.cluster.AtomixCluster;
import io.atomix.cluster.MemberId;
import io.atomix.cluster.Node;
import io.atomix.cluster.discovery.BootstrapDiscoveryProvider;
import io.atomix.cluster.impl.DiscoveryMembershipProtocol;
import io.atomix.primitive.partition.PartitionId;
import io.atomix.primitive.partition.PartitionMetadata;
import io.camunda.zeebe.dynamic.config.changes.ClusterChangeExecutor.NoopClusterChangeExecutor;
import io.camunda.zeebe.dynamic.config.changes.ConfigurationChangeCoordinator;
import io.camunda.zeebe.dynamic.config.changes.NoopPartitionChangeExecutor;
import io.camunda.zeebe.dynamic.config.changes.PartitionScalingChangeExecutor.NoopPartitionScalingChangeExecutor;
import io.camunda.zeebe.dynamic.config.gossip.ClusterConfigurationGossiperConfig;
import io.camunda.zeebe.dynamic.config.state.ClusterConfigurationChangeOperation.PartitionChangeOperation.PartitionJoinOperation;
import io.camunda.zeebe.dynamic.config.state.ClusterConfigurationChangeOperation.PartitionChangeOperation.PartitionLeaveOperation;
import io.camunda.zeebe.dynamic.config.state.DynamicPartitionConfig;
import io.camunda.zeebe.scheduler.ActorScheduler;
import io.camunda.zeebe.scheduler.ActorSchedulingService;
import io.camunda.zeebe.scheduler.future.ActorFuture;
import io.camunda.zeebe.test.util.socket.SocketUtil;
import io.camunda.zeebe.util.Either;
import io.camunda.zeebe.util.FileUtil;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClusterConfigurationManagementIntegrationTest {

  @TempDir Path rootDir;

  private final ActorScheduler actorScheduler = ActorScheduler.newActorScheduler().build();

  private final List<Node> clusterNodes =
      List.of(createNode("0"), createNode("1"), createNode("2"));
  private final Map<Integer, TestNode> nodes = new HashMap<>();
  private Set<MemberId> clusterMemberIds;
  @AutoClose private MeterRegistry meterRegistry = new SimpleMeterRegistry();

  @BeforeEach
  void setup() {
    actorScheduler.start();

    clusterMemberIds =
        clusterNodes.stream()
            .map(Node::id)
            .map(id -> MemberId.from(id.id()))
            .collect(Collectors.toSet());

    // start 3 clusterNodes
    createClusterNodes();
  }

  @AfterEach
  void tearDown() {
    nodes.values().forEach(TestNode::close);
    actorScheduler.stop();
  }

  private void createClusterNodes() {
    clusterNodes.forEach(
        node ->
            nodes.put(
                Integer.parseInt(node.id().id()),
                createClusterTopologyManagerService(
                    rootDir, createClusterNode(node, clusterNodes))));
  }

  @Test
  void shouldInitializeTopologyFromCoordinator() {
    // when - start node 1 and 2 with incorrect partition distribution
    final var startFutures = nodes.values().stream().map(this::startNode).toList();
    // nodes should start in parallel
    startFutures.forEach(ActorFuture::join);

    // then - all nodes should have the correct topology generated by node 0
    nodes
        .values()
        .forEach(
            node ->
                ClusterConfigurationAssert.assertThatClusterTopology(
                        node.service().getClusterTopology().join())
                    .describedAs(
                        "The cluster should initialize topology with correct configuration")
                    .isInitialized()
                    .hasOnlyMembers(Set.of(0, 1, 2)));
  }

  @Test
  void shouldUseExistingTopologyAfterRestart() {
    // given
    final var startFutures = nodes.values().stream().map(this::startNode).toList();
    startFutures.forEach(ActorFuture::join);

    // when
    nodes.values().forEach(TestNode::close);
    // restart with incorrect topology
    createClusterNodes();

    final var restartFutures =
        nodes.values().stream()
            .map(node -> node.start(actorScheduler, Set.of(), getIncorrectPartitionDistribution()))
            .toList();
    restartFutures.forEach(ActorFuture::join);

    // then - all nodes should have the correct topology generated from the first bootstrap
    nodes
        .values()
        .forEach(
            node ->
                ClusterConfigurationAssert.assertThatClusterTopology(
                        node.service().getClusterTopology().join())
                    .describedAs("The cluster should use the correct topology")
                    .isInitialized()
                    .hasOnlyMembers(Set.of(0, 1, 2)));
  }

  @Test
  void shouldInitializeCoordinatorFromPeers() throws IOException {
    // given
    final var startFutures = nodes.values().stream().map(this::startNode).toList();
    startFutures.forEach(ActorFuture::join);

    // when
    nodes.get(0).close();

    // recreate node 0 with empty topology file to simulate data loss
    final Path emptyTopologyFile = rootDir.resolve("new");
    FileUtil.ensureDirectoryExists(emptyTopologyFile);
    final TestNode restartedNode0 =
        createClusterTopologyManagerService(
            emptyTopologyFile, createClusterNode(clusterNodes.get(0), clusterNodes));
    nodes.put(0, restartedNode0);

    restartedNode0
        .start(actorScheduler, clusterMemberIds, getIncorrectPartitionDistribution())
        .join();

    // then - all nodes should have the correct topology generated before restart of node 0
    nodes
        .values()
        .forEach(
            node ->
                ClusterConfigurationAssert.assertThatClusterTopology(
                        node.service().getClusterTopology().join())
                    .describedAs("The cluster should use the correct topology")
                    .isInitialized()
                    .hasOnlyMembers(Set.of(0, 1, 2)));
  }

  @Test
  void shouldApplyTopologyChange() {
    // given - all members have partition 1
    final var startFutures = nodes.values().stream().map(this::startNode).toList();
    startFutures.forEach(ActorFuture::join);

    // when
    final ClusterConfigurationManagerService service = nodes.get(0).service();
    final ConfigurationChangeCoordinator coordinator =
        service.getTopologyChangeCoordinator().orElseThrow();
    coordinator
        .applyOperations(
            ignore ->
                Either.right(
                    List.of(
                        new PartitionJoinOperation(MemberId.from("0"), 2, 1),
                        new PartitionLeaveOperation(MemberId.from("1"), 1, 1))))
        .join();

    // then
    Awaitility.await("The topology change should complete.")
        .untilAsserted(
            () ->
                ClusterConfigurationAssert.assertThatClusterTopology(
                        service.getClusterTopology().join())
                    .hasPendingOperationsWithSize(0));
    ClusterConfigurationAssert.assertThatClusterTopology(service.getClusterTopology().join())
        .describedAs("The cluster must have the expected topology after change.")
        .hasMemberWithPartitions(0, Set.of(1, 2))
        .hasMemberWithPartitions(1, Set.of());
  }

  @Test
  void shouldApplyConsecutiveTopologyChangeOnSameMember() {
    // given - all members have partition 1
    final var startFutures = nodes.values().stream().map(this::startNode).toList();
    startFutures.forEach(ActorFuture::join);

    // when
    final ClusterConfigurationManagerService service = nodes.get(0).service();
    final ConfigurationChangeCoordinator coordinator =
        service.getTopologyChangeCoordinator().orElseThrow();
    coordinator
        .applyOperations(
            ignore ->
                Either.right(
                    List.of(
                        new PartitionJoinOperation(MemberId.from("0"), 2, 1),
                        new PartitionLeaveOperation(MemberId.from("1"), 1, 1),
                        new PartitionJoinOperation(MemberId.from("1"), 1, 1))))
        .join();

    // then
    Awaitility.await("The topology change should complete.")
        .untilAsserted(
            () ->
                ClusterConfigurationAssert.assertThatClusterTopology(
                        service.getClusterTopology().join())
                    .hasPendingOperationsWithSize(0));
    ClusterConfigurationAssert.assertThatClusterTopology(service.getClusterTopology().join())
        .describedAs("The cluster must have the expected topology after change.")
        .hasMemberWithPartitions(0, Set.of(1, 2))
        .hasMemberWithPartitions(1, Set.of(1));
  }

  private Node createNode(final String id) {
    return Node.builder().withId(id).withPort(SocketUtil.getNextAddress().getPort()).build();
  }

  private AtomixCluster createClusterNode(final Node localNode, final Collection<Node> nodes) {
    return AtomixCluster.builder(meterRegistry)
        .withAddress(localNode.address())
        .withMemberId(localNode.id().id())
        .withMembershipProvider(new BootstrapDiscoveryProvider(nodes))
        .withMembershipProtocol(new DiscoveryMembershipProtocol())
        .build();
  }

  private TestNode createClusterTopologyManagerService(
      final Path tempDir, final AtomixCluster cluster) {
    final var service =
        new ClusterConfigurationManagerService(
            tempDir.resolve(cluster.getMembershipService().getLocalMember().id().id()),
            cluster.getCommunicationService(),
            cluster.getMembershipService(),
            new ClusterConfigurationGossiperConfig(
                Duration.ofSeconds(1), Duration.ofMillis(100), 2),
            true,
            new NoopClusterChangeExecutor(),
            meterRegistry);
    return new TestNode(cluster, service);
  }

  private ActorFuture<Void> startNode(final TestNode node) {
    if (node.cluster.getMembershipService().getLocalMember().id().id().equals("0")) {
      return node.start(actorScheduler, clusterMemberIds, getExpectedPartitionDistribution());
    } else {
      return node.start(actorScheduler, clusterMemberIds, getIncorrectPartitionDistribution());
    }
  }

  private Set<PartitionMetadata> getExpectedPartitionDistribution() {
    return Set.of(
        new PartitionMetadata(
            PartitionId.from("test", 1),
            Set.of(MemberId.from("0"), MemberId.from("1"), MemberId.from("2")),
            Map.of(MemberId.from("0"), 1, MemberId.from("1"), 2, MemberId.from("2"), 3),
            3,
            MemberId.from("2")),
        new PartitionMetadata(
            PartitionId.from("test", 2),
            Set.of(MemberId.from("2")),
            Map.of(MemberId.from("2"), 1),
            1,
            MemberId.from("2")));
  }

  private Set<PartitionMetadata> getIncorrectPartitionDistribution() {
    return Set.of(
        new PartitionMetadata(
            PartitionId.from("test", 1),
            Set.of(MemberId.from("10"), MemberId.from("11"), MemberId.from("12")),
            Map.of(MemberId.from("10"), 1, MemberId.from("11"), 2, MemberId.from("12"), 3),
            3,
            MemberId.from("12")));
  }

  private record TestNode(AtomixCluster cluster, ClusterConfigurationManagerService service) {

    ActorFuture<Void> start(
        final ActorSchedulingService actorScheduler,
        final Set<MemberId> clusterMembers,
        final Set<PartitionMetadata> partitions) {
      cluster.start().join();
      final var startFuture =
          service.start(
              actorScheduler,
              new StaticConfiguration(
                  false,
                  new ControllablePartitionDistributor().withPartitions(partitions),
                  clusterMembers,
                  cluster.getMembershipService().getLocalMember().id(),
                  List.of(),
                  3,
                  DynamicPartitionConfig.init()));
      startFuture.onComplete(
          (ignore, error) -> {
            if (error == null) {
              service.registerPartitionChangeExecutors(
                  new NoopPartitionChangeExecutor(), new NoopPartitionScalingChangeExecutor());
            }
          },
          Runnable::run);
      return startFuture;
    }

    void close() {
      service.closeAsync().join();
      cluster.stop().join();
    }
  }
}
