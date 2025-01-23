/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.db.rdbms.write.queue;

public enum ContextType {
  AUTHORIZATION,
  EXPORTER_POSITION,
  DECISION_DEFINITION,
  DECISION_INSTANCE,
  GROUP,
  PROCESS_DEFINITION,
  PROCESS_INSTANCE,
  FLOW_NODE,
  TENANT,
  INCIDENT,
  VARIABLE,
  ROLE,
  USER,
  USER_TASK,
  FORM,
  MAPPING
}
