/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.exporter.handlers.operation;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.intent.VariableDocumentIntent;
import io.camunda.zeebe.protocol.record.value.VariableDocumentRecordValue;

public class OperationFromVariableDocumentHandler
    extends AbstractOperationHandler<VariableDocumentRecordValue> {

  public OperationFromVariableDocumentHandler(final String indexName) {
    super(indexName);
  }

  @Override
  public ValueType getHandledValueType() {
    return ValueType.VARIABLE_DOCUMENT;
  }

  @Override
  public boolean handlesRecord(final Record<VariableDocumentRecordValue> record) {
    return VariableDocumentIntent.UPDATED.equals(record.getIntent());
  }
}
