/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.application.commons.migration;

import io.camunda.exporter.schema.SearchEngineClient;
import io.camunda.exporter.utils.ReindexResult;
import io.camunda.search.connect.configuration.ConnectConfiguration;
import io.camunda.search.connect.configuration.DatabaseType;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public final class PrefixMigrationHelper {
  private static final Integer RUNTIME_INDICES_MIGRATION_RETRY_COUNT = 3;

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
      final SearchEngineClient searchEngineClient,
      final ThreadPoolTaskExecutor executor) {
    final var isElasticsearch = connectConfig.getTypeEnum() == DatabaseType.ELASTICSEARCH;

    retryIndexMigration(
        () ->
            migrateIndices(
                operatePrefix,
                connectConfig.getIndexPrefix(),
                searchEngineClient,
                isElasticsearch,
                OPERATE_INDICES_TO_MIGRATE,
                executor),
        searchEngineClient,
        executor);

    retryIndexMigration(
        () ->
            migrateIndices(
                tasklistPrefix,
                connectConfig.getIndexPrefix(),
                searchEngineClient,
                isElasticsearch,
                TASKLIST_INDICES_TO_MIGRATE,
                executor),
        searchEngineClient,
        executor);
  }

  private static void retryIndexMigration(
      final Supplier<List<ReindexResult>> migrationTask,
      final SearchEngineClient searchEngineClient,
      final ThreadPoolTaskExecutor executor) {

    var operateMigrationResults = migrationTask.get();

    for (int i = 0; i < RUNTIME_INDICES_MIGRATION_RETRY_COUNT; i++) {
      final var failedReindexSrcToDest =
          operateMigrationResults.stream()
              .filter(res -> !res.successful())
              .collect(Collectors.toMap(ReindexResult::source, ReindexResult::destination));

      if (failedReindexSrcToDest.isEmpty()) {
        break;
      }

      operateMigrationResults = searchEngineClient.reindex(failedReindexSrcToDest, executor);
    }
  }

  private static List<ReindexResult> migrateIndices(
      final String oldPrefix,
      final String newPrefix,
      final SearchEngineClient searchEngineClient,
      final boolean isElasticsearch,
      final Set<Class<? extends AbstractIndexDescriptor>> indicesToMigrateClasses,
      final ThreadPoolTaskExecutor executor) {
    final var indicesWithNewPrefix = new IndexDescriptors(newPrefix, isElasticsearch);

    final Map<String, String> indicesToMigrateSrcToDest =
        indicesToMigrateClasses.stream()
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

    return searchEngineClient.reindex(indicesToMigrateSrcToDest, executor);
  }
}
