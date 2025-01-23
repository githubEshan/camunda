/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.webapps.schema.entities;

import io.camunda.webapps.schema.entities.tasklist.TasklistEntity;
import java.time.OffsetDateTime;
import java.util.Objects;

public class MetricEntity extends TasklistEntity<MetricEntity> {
  private String event;
  private String value;
  private OffsetDateTime eventTime;

  public MetricEntity() {
    super();
  }

  public MetricEntity(final String event, final String value, final OffsetDateTime eventTime) {
    this.event = event;
    this.value = value;
    this.eventTime = eventTime;
  }

  public String getEvent() {
    return event;
  }

  public MetricEntity setEvent(final String event) {
    this.event = event;
    return this;
  }

  public String getValue() {
    return value;
  }

  public MetricEntity setValue(final String value) {
    this.value = value;
    return this;
  }

  public OffsetDateTime getEventTime() {
    return eventTime;
  }

  public MetricEntity setEventTime(final OffsetDateTime eventTime) {
    this.eventTime = eventTime;
    return this;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), event, value, eventTime);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    final MetricEntity that = (MetricEntity) o;
    return Objects.equals(event, that.event)
        && Objects.equals(value, that.value)
        && Objects.equals(eventTime, that.eventTime);
  }
}
