/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.webapps.schema.descriptors.tasklist.index;

import io.camunda.webapps.schema.descriptors.backup.Prio1Backup;
import io.camunda.webapps.schema.descriptors.tasklist.TasklistIndexDescriptor;

public class TasklistImportPositionIndex extends TasklistIndexDescriptor implements Prio1Backup {

  public static final String INDEX_NAME = "import-position";
  public static final String INDEX_VERSION = "8.2.0";

  public static final String ALIAS_NAME = "aliasName";
  public static final String ID = "id";
  public static final String POSITION = "position";
  public static final String SEQUENCE = "sequence";
  public static final String FIELD_INDEX_NAME = "indexName";
  public static final String COMPLETED = "completed";
  public static final String PARTITION_ID = "partitionId";

  public TasklistImportPositionIndex(final String indexPrefix, final boolean isElasticsearch) {
    super(indexPrefix, isElasticsearch);
  }

  @Override
  public String getIndexName() {
    return INDEX_NAME;
  }

  @Override
  public String getVersion() {
    return INDEX_VERSION;
  }
}
