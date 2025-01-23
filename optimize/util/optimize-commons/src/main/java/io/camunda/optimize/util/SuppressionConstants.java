/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.util;

public final class SuppressionConstants {

  public static final String UNUSED = "unused";
  public static final String UNCHECKED_CAST = "unchecked";
  public static final String RAW_TYPES = "rawtypes";
  public static final String SAME_PARAM_VALUE = "SameParameterValue";
  public static final String OPTIONAL_FIELD_OR_PARAM = "OptionalUsedAsFieldOrParameterType";
  public static final String OPTIONAL_ASSIGNED_TO_NULL = "OptionalAssignedToNull";

  private SuppressionConstants() {}
}
