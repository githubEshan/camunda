/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.util;

import static io.camunda.optimize.service.db.schema.index.DecisionInstanceIndex.MULTIVALUE_FIELD_DATE;
import static io.camunda.optimize.service.db.schema.index.DecisionInstanceIndex.MULTIVALUE_FIELD_DOUBLE;
import static io.camunda.optimize.service.db.schema.index.DecisionInstanceIndex.MULTIVALUE_FIELD_LONG;
import static io.camunda.optimize.service.db.schema.index.DecisionInstanceIndex.VARIABLE_CLAUSE_ID;
import static io.camunda.optimize.service.db.schema.index.DecisionInstanceIndex.VARIABLE_VALUE;
import static io.camunda.optimize.service.db.schema.index.DecisionInstanceIndex.VARIABLE_VALUE_TYPE;

import io.camunda.optimize.dto.optimize.query.variable.VariableType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class DecisionVariableHelper {

  private static final List<VariableType> MULTIVALUE_TYPE_FIELDS =
      Collections.unmodifiableList(
          Arrays.asList(VariableType.DATE, VariableType.DOUBLE, VariableType.LONG));

  private DecisionVariableHelper() {}

  public static String getVariableValueField(final String variablePath) {
    return variablePath + "." + VARIABLE_VALUE;
  }

  public static List<VariableType> getVariableMultivalueFields() {
    return MULTIVALUE_TYPE_FIELDS;
  }

  public static String getVariableStringValueField(final String variablePath) {
    return getVariableValueFieldForType(variablePath, VariableType.STRING);
  }

  public static String getValueSearchField(
      final String variablePath, final String searchFieldName) {
    return getVariableStringValueField(variablePath) + "." + searchFieldName;
  }

  public static String buildWildcardQuery(final String valueFilter) {
    return "*" + valueFilter + "*";
  }

  public static String getVariableValueFieldForType(
      final String variablePath, final VariableType type) {
    return switch (Optional.ofNullable(type)
        .orElseThrow(() -> new IllegalArgumentException("No Type provided"))) {
      case BOOLEAN, STRING, OBJECT -> getVariableValueField(variablePath);
      case DOUBLE -> getVariableValueField(variablePath) + "." + MULTIVALUE_FIELD_DOUBLE;
      case SHORT, INTEGER, LONG ->
          getVariableValueField(variablePath) + "." + MULTIVALUE_FIELD_LONG;
      case DATE -> getVariableValueField(variablePath) + "." + MULTIVALUE_FIELD_DATE;
      default -> throw new IllegalArgumentException("Unhandled type: " + type);
    };
  }

  public static String getVariableClauseIdField(final String variablePath) {
    return variablePath + "." + VARIABLE_CLAUSE_ID;
  }

  public static String getVariableTypeField(final String variablePath) {
    return variablePath + "." + VARIABLE_VALUE_TYPE;
  }
}
