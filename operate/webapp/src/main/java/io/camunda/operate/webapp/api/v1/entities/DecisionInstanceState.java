/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.operate.webapp.api.v1.entities;

public enum DecisionInstanceState {
  FAILED,
  EVALUATED,
  UNKNOWN,
  UNSPECIFIED;

  public static DecisionInstanceState getState(
      io.camunda.webapps.schema.entities.operate.dmn.DecisionInstanceState state) {
    if (state == null) {
      return UNSPECIFIED;
    }
    switch (state) {
      case FAILED:
        return FAILED;
      case EVALUATED:
        return EVALUATED;
      default:
        return UNKNOWN;
    }
  }
}
