/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize.query.report.single.filter.data;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableSet;
import io.camunda.optimize.dto.optimize.query.report.FilterOperatorConstants;
import java.util.Set;

public enum FilterOperator {
  IN(FilterOperatorConstants.IN),
  NOT_IN(FilterOperatorConstants.NOT_IN),
  CONTAINS(FilterOperatorConstants.CONTAINS),
  NOT_CONTAINS(FilterOperatorConstants.NOT_CONTAINS),
  LESS_THAN(FilterOperatorConstants.LESS_THAN),
  LESS_THAN_EQUALS(FilterOperatorConstants.LESS_THAN_EQUALS),
  GREATER_THAN(FilterOperatorConstants.GREATER_THAN),
  GREATER_THAN_EQUALS(FilterOperatorConstants.GREATER_THAN_EQUALS);

  public static final Set<FilterOperator> RELATIVE_OPERATORS =
      ImmutableSet.of(LESS_THAN, LESS_THAN_EQUALS, GREATER_THAN, GREATER_THAN_EQUALS);
  private static final Set<FilterOperator> CONTAINS_OPERATORS =
      ImmutableSet.of(CONTAINS, NOT_CONTAINS);
  private static final Set<FilterOperator> EQUALS_OPERATORS = ImmutableSet.of(IN, NOT_IN);
  private final String id;

  FilterOperator(final String id) {
    this.id = id;
  }

  @JsonValue
  public String getId() {
    return id;
  }

  public static boolean isContainsOperation(final FilterOperator filterOperator) {
    return CONTAINS_OPERATORS.contains(filterOperator);
  }

  public static boolean isEqualsOperation(final FilterOperator filterOperator) {
    return EQUALS_OPERATORS.contains(filterOperator);
  }
}
