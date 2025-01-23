/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.application.commons.migration;

import io.camunda.exporter.schema.PrefixMigrationClient;
import io.camunda.exporter.utils.ReindexResult;
import io.camunda.search.connect.configuration.ConnectConfiguration;
import io.camunda.webapps.schema.descriptors.AbstractIndexDescriptor;
import io.camunda.webapps.schema.descriptors.IndexDescriptors;
import io.camunda.webapps.schema.descriptors.operate.index.DecisionIndex;
import io.camunda.webapps.schema.descriptors.operate.index.DecisionRequirementsIndex;
import io.camunda.webapps.schema.descriptors.operate.index.MetricIndex;
import io.camunda.webapps.schema.descriptors.operate.index.ProcessIndex;
import io.camunda.webapps.schema.descriptors.operate.template.BatchOperationTemplate;
import io.camunda.webapps.schema.descriptors.operate.template.DecisionInstanceTemplate;
import io.camunda.webapps.schema.descriptors.operate.template.EventTemplate;
import io.camunda.webapps.schema.descriptors.operate.template.FlowNodeInstanceTemplate;
import io.camunda.webapps.schema.descriptors.operate.template.IncidentTemplate;
import io.camunda.webapps.schema.descriptors.operate.template.JobTemplate;
import io.camunda.webapps.schema.descriptors.operate.template.ListViewTemplate;
import io.camunda.webapps.schema.descriptors.operate.template.MessageTemplate;
import io.camunda.webapps.schema.descriptors.operate.template.OperationTemplate;
import io.camunda.webapps.schema.descriptors.operate.template.PostImporterQueueTemplate;
import io.camunda.webapps.schema.descriptors.operate.template.SequenceFlowTemplate;
import io.camunda.webapps.schema.descriptors.operate.template.VariableTemplate;
import io.camunda.webapps.schema.descriptors.tasklist.index.FormIndex;
import io.camunda.webapps.schema.descriptors.tasklist.template.TaskTemplate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public final class PrefixMigrationHelper {
  private static final Integer RUNTIME_INDICES_MIGRATION_RETRY_COUNT = 3;
  private static final Logger LOG = LoggerFactory.getLogger(PrefixMigrationHelper.class);

  private static final Set<Class<? extends AbstractIndexDescriptor>> TASKLIST_INDICES_TO_MIGRATE =
      new HashSet<>(Arrays.asList(FormIndex.class, TaskTemplate.class));

  private static final Set<Class<? extends AbstractIndexDescriptor>> OPERATE_INDICES_TO_MIGRATE =
      new HashSet<>(
          Arrays.asList(
              ListViewTemplate.class,
              JobTemplate.class,
              MetricIndex.class,
              BatchOperationTemplate.class,
              ProcessIndex.class,
              DecisionRequirementsIndex.class,
              DecisionIndex.class,
              EventTemplate.class,
              VariableTemplate.class,
              PostImporterQueueTemplate.class,
              SequenceFlowTemplate.class,
              MessageTemplate.class,
              DecisionInstanceTemplate.class,
              IncidentTemplate.class,
              FlowNodeInstanceTemplate.class,
              OperationTemplate.class));

  private PrefixMigrationHelper() {}

  public static void migrateRuntimeIndices(
      final String operatePrefix,
      final String tasklistPrefix,
      final ConnectConfiguration connectConfig,
      final PrefixMigrationClient prefixMigrationClient,
      final ThreadPoolTaskExecutor executor) {

    final var srcToDestOperateMigrationMap =
        createSrcToDestMigrationMap(
            operatePrefix, connectConfig.getIndexPrefix(), OPERATE_INDICES_TO_MIGRATE);
    indexMigrationWithRetry(srcToDestOperateMigrationMap, executor, prefixMigrationClient);

    final var srcToDestTasklistMigrationMap =
        createSrcToDestMigrationMap(
            tasklistPrefix, connectConfig.getIndexPrefix(), TASKLIST_INDICES_TO_MIGRATE);

    indexMigrationWithRetry(srcToDestTasklistMigrationMap, executor, prefixMigrationClient);
  }

  private static void indexMigrationWithRetry(
      final Map<String, String> srcToDestMigrationMap,
      final ThreadPoolTaskExecutor executor,
      final PrefixMigrationClient prefixMigrationClient) {

    var failedReindex =
        migrateIndices(srcToDestMigrationMap, executor, prefixMigrationClient).stream()
            .filter(res -> !res.successful())
            .collect(Collectors.toMap(ReindexResult::source, ReindexResult::destination));

    for (int i = 0; i < RUNTIME_INDICES_MIGRATION_RETRY_COUNT; i++) {
      if (failedReindex.isEmpty()) {
        break;
      }

      failedReindex =
          migrateIndices(failedReindex, executor, prefixMigrationClient).stream()
              .filter(res -> !res.successful())
              .collect(Collectors.toMap(ReindexResult::source, ReindexResult::destination));
    }
  }

  private static Map<String, String> createSrcToDestMigrationMap(
      final String oldPrefix,
      final String newPrefix,
      final Set<Class<? extends AbstractIndexDescriptor>> indicesToMigrateClasses) {
    final var indicesWithNewPrefix = new IndexDescriptors(newPrefix, true);

    return indicesToMigrateClasses.stream()
        .map(
            descriptorClass -> {
              final var newIndex = indicesWithNewPrefix.get(descriptorClass);
              // we can use the new version index as it does not change
              final var oldIndexName =
                  String.format(
                      "%s-%s-%s_", oldPrefix, newIndex.getIndexName(), newIndex.getVersion());

              return Map.entry(oldIndexName, newIndex.getFullQualifiedName());
            })
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  private static List<ReindexResult> migrateIndices(
      final Map<String, String> indicesToMigrateSrcToDest,
      final ThreadPoolTaskExecutor executor,
      final PrefixMigrationClient prefixMigrationClient) {
    final var reindexFutures =
        indicesToMigrateSrcToDest.entrySet().stream()
            .map(
                (ent) ->
                    CompletableFuture.supplyAsync(
                        () -> prefixMigrationClient.reindex(ent.getKey(), ent.getValue()),
                        executor))
            .toList();

    CompletableFuture.allOf(reindexFutures.toArray(new CompletableFuture[0])).join();

    return reindexFutures.stream().map(CompletableFuture::join).toList();
  }
}
