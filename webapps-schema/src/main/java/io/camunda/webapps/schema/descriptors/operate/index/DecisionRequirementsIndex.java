/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.webapps.schema.descriptors.operate.index;

import io.camunda.webapps.schema.descriptors.backup.Prio5Backup;
import io.camunda.webapps.schema.descriptors.operate.OperateIndexDescriptor;
import java.util.Optional;

public class DecisionRequirementsIndex extends OperateIndexDescriptor implements Prio5Backup {

  public static final String INDEX_NAME = "decision-requirements";
  public static final String ID = "id";
  public static final String KEY = "key";
  public static final String DECISION_DEFINITION_ID = "decisionDefinitionId";
  public static final String DECISION_REQUIREMENTS_ID = "decisionRequirementsId";
  public static final String NAME = "name";
  public static final String VERSION = "version";
  public static final String RESOURCE_NAME = "resourceName";
  public static final String XML = "xml";

  public DecisionRequirementsIndex(final String indexPrefix, final boolean isElasticsearch) {
    super(indexPrefix, isElasticsearch);
  }

  @Override
  public String getIndexName() {
    return INDEX_NAME;
  }

  @Override
  public Optional<String> getTenantIdField() {
    return Optional.of(TENANT_ID);
  }

  @Override
  public String getVersion() {
    return "8.3.0";
  }
}
