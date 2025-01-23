/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.webapps.schema.descriptors;

import io.camunda.webapps.schema.descriptors.operate.index.DecisionIndex;
import io.camunda.webapps.schema.descriptors.operate.index.DecisionRequirementsIndex;
import io.camunda.webapps.schema.descriptors.operate.index.ImportPositionIndex;
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
import io.camunda.webapps.schema.descriptors.tasklist.index.TasklistImportPositionIndex;
import io.camunda.webapps.schema.descriptors.tasklist.index.TasklistMetricIndex;
import io.camunda.webapps.schema.descriptors.tasklist.template.DraftTaskVariableTemplate;
import io.camunda.webapps.schema.descriptors.tasklist.template.SnapshotTaskVariableTemplate;
import io.camunda.webapps.schema.descriptors.tasklist.template.TaskTemplate;
import io.camunda.webapps.schema.descriptors.usermanagement.index.AuthorizationIndex;
import io.camunda.webapps.schema.descriptors.usermanagement.index.GroupIndex;
import io.camunda.webapps.schema.descriptors.usermanagement.index.MappingIndex;
import io.camunda.webapps.schema.descriptors.usermanagement.index.PersistentWebSessionIndexDescriptor;
import io.camunda.webapps.schema.descriptors.usermanagement.index.RoleIndex;
import io.camunda.webapps.schema.descriptors.usermanagement.index.TenantIndex;
import io.camunda.webapps.schema.descriptors.usermanagement.index.UserIndex;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IndexDescriptors {
  private final Map<Class<?>, IndexDescriptor> map;

  public IndexDescriptors(final String indexPrefix, final boolean isElasticsearch) {
    map =
        Stream.of(
                new AuthorizationIndex(indexPrefix, isElasticsearch),
                new BatchOperationTemplate(indexPrefix, isElasticsearch),
                new DecisionIndex(indexPrefix, isElasticsearch),
                new DecisionInstanceTemplate(indexPrefix, isElasticsearch),
                new DecisionRequirementsIndex(indexPrefix, isElasticsearch),
                new DraftTaskVariableTemplate(indexPrefix, isElasticsearch),
                new EventTemplate(indexPrefix, isElasticsearch),
                new FlowNodeInstanceTemplate(indexPrefix, isElasticsearch),
                new FormIndex(indexPrefix, isElasticsearch),
                new GroupIndex(indexPrefix, isElasticsearch),
                new ImportPositionIndex(indexPrefix, isElasticsearch),
                new IncidentTemplate(indexPrefix, isElasticsearch),
                new JobTemplate(indexPrefix, isElasticsearch),
                new ListViewTemplate(indexPrefix, isElasticsearch),
                new MappingIndex(indexPrefix, isElasticsearch),
                new MetricIndex(indexPrefix, isElasticsearch),
                new OperationTemplate(indexPrefix, isElasticsearch),
                new PersistentWebSessionIndexDescriptor(indexPrefix, isElasticsearch),
                new PostImporterQueueTemplate(indexPrefix, isElasticsearch),
                new ProcessIndex(indexPrefix, isElasticsearch),
                new RoleIndex(indexPrefix, isElasticsearch),
                new SequenceFlowTemplate(indexPrefix, isElasticsearch),
                new SnapshotTaskVariableTemplate(indexPrefix, isElasticsearch),
                new TaskTemplate(indexPrefix, isElasticsearch),
                new TasklistImportPositionIndex(indexPrefix, isElasticsearch),
                new TasklistMetricIndex(indexPrefix, isElasticsearch),
                new TenantIndex(indexPrefix, isElasticsearch),
                new UserIndex(indexPrefix, isElasticsearch),
                new VariableTemplate(indexPrefix, isElasticsearch),
                new MessageTemplate(indexPrefix, isElasticsearch))
            .collect(Collectors.toMap(Object::getClass, Function.identity()));
  }

  public <T extends IndexDescriptor> T get(final Class<T> cls) {
    return cls.cast(map.get(cls));
  }

  public Collection<IndexDescriptor> all() {
    return map.values();
  }

  public Collection<IndexDescriptor> indices() {
    return all().stream()
        .filter(indexDescriptor -> !(indexDescriptor instanceof IndexTemplateDescriptor))
        .toList();
  }

  public Collection<IndexTemplateDescriptor> templates() {
    return all().stream()
        .flatMap(
            indexDescriptor ->
                indexDescriptor instanceof final IndexTemplateDescriptor templateDescriptor
                    ? Stream.of(templateDescriptor)
                    : Stream.of())
        .toList();
  }
}
