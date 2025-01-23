/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.operate.webapp.rest.exception;

import java.util.UUID;

public abstract class InternalAPIException extends RuntimeException {

  @SuppressWarnings("checkstyle:MutableException")
  private String instance = UUID.randomUUID().toString();

  protected InternalAPIException(final String message) {
    super(message);
  }

  protected InternalAPIException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public String getInstance() {
    return instance;
  }

  public InternalAPIException setInstance(final String instance) {
    this.instance = instance;
    return this;
  }
}
