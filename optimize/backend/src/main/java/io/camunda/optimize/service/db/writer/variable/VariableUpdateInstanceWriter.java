/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.writer.variable;

import io.camunda.optimize.service.db.repository.VariableRepository;
import java.util.List;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class VariableUpdateInstanceWriter {

  private static final Logger LOG =
      org.slf4j.LoggerFactory.getLogger(VariableUpdateInstanceWriter.class);
  private final VariableRepository variableRepository;

  public VariableUpdateInstanceWriter(final VariableRepository variableRepository) {
    this.variableRepository = variableRepository;
  }

  public void deleteByProcessInstanceIds(final List<String> processInstanceIds) {
    LOG.info("Deleting variable updates for [{}] processInstanceIds", processInstanceIds.size());
    variableRepository.deleteByProcessInstanceIds(processInstanceIds);
  }
}
