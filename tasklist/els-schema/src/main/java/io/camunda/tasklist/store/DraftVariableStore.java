/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.tasklist.store;

import io.camunda.webapps.schema.entities.tasklist.DraftTaskVariableEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DraftVariableStore {

  void createOrUpdate(Collection<DraftTaskVariableEntity> draftVariables);

  long deleteAllByTaskId(String taskId);

  List<DraftTaskVariableEntity> getVariablesByTaskIdAndVariableNames(
      String taskId, List<String> variableNames);

  Optional<DraftTaskVariableEntity> getById(String variableId);

  List<String> getDraftVariablesIdsByTaskIds(List<String> taskIds);
}
