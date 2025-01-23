/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.exporter.handlers.operation;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.intent.IncidentIntent;
import io.camunda.zeebe.protocol.record.value.IncidentRecordValue;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OperationFromIncidentHandlerTest extends AbstractOperationHandlerTest<IncidentRecordValue> {

  @BeforeEach
  void setUp() {
    underTest = new OperationFromIncidentHandler(indexName);
    valueType = ValueType.INCIDENT;
  }

  @Test
  void shouldHandleRecord() {
    final Record<IncidentRecordValue> record = generateRecord(IncidentIntent.RESOLVED);
    assertThat(underTest.handlesRecord(record)).isTrue();
  }

  @Test
  void shouldNotHandleRecord() {
    Stream.of(IncidentIntent.values())
        .filter(intent -> intent != IncidentIntent.RESOLVED)
        .map(this::generateRecord)
        .forEach(record -> assertThat(underTest.handlesRecord(record)).isFalse());
  }
}
