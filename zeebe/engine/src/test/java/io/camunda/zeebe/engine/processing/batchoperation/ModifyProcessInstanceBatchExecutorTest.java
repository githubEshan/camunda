/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.processing.batchoperation;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RecordType;
import io.camunda.zeebe.protocol.record.intent.BatchOperationExecutionIntent;
import io.camunda.zeebe.protocol.record.intent.JobIntent;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceModificationIntent;
import io.camunda.zeebe.test.util.record.RecordingExporter;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public final class ModifyProcessInstanceBatchExecutorTest extends AbstractBatchOperationTest {

  @Test
  public void shouldModifyProcessInstance() {
    // given
    final Map<String, Object> claims = Map.of("claim1", "value1", "claim2", "value2");

    // create a process with two user tasks
    final long processDefinitionKey =
        engine
            .deployment()
            .withXmlResource(
                Bpmn.createExecutableProcess("process")
                    .startEvent()
                    .userTask("userTaskA")
                    .userTask("userTaskB")
                    .done())
            .deploy()
            .getValue()
            .getProcessesMetadata()
            .get(0)
            .getProcessDefinitionKey();

    final var processInstanceKey = engine.processInstance().ofBpmnProcessId("process").create();

    // wait for the user task to exist
    RecordingExporter.jobRecords()
        .withProcessInstanceKey(processInstanceKey)
        .withIntent(JobIntent.CREATED)
        .getFirst();

    // then start the batch where we modify the process instance and move the token the userTaskB
    final var batchOperationKey =
        createNewModifyProcessInstanceBatchOperation(
            Set.of(processInstanceKey), "userTaskA", "userTaskB", claims);

    // then we have executed and completed event
    assertThat(
            RecordingExporter.batchOperationExecutionRecords()
                .withBatchOperationKey(batchOperationKey)
                .onlyEvents())
        .extracting(Record::getIntent)
        .containsSequence(
            BatchOperationExecutionIntent.EXECUTED, BatchOperationExecutionIntent.COMPLETED);

    // and a follow op up command to execute again
    assertThat(
            RecordingExporter.batchOperationExecutionRecords()
                .withBatchOperationKey(batchOperationKey)
                .onlyCommands())
        .extracting(Record::getIntent)
        .containsSequence(BatchOperationExecutionIntent.EXECUTE);

    // and we have a modify command
    final var modificationCommands =
        RecordingExporter.processInstanceModificationRecords()
            .withRecordType(RecordType.COMMAND)
            .withRecordKey(processInstanceKey)
            .toList();
    assertThat(modificationCommands).hasSize(1);
    assertThat(modificationCommands.getFirst().getIntent())
        .isEqualTo(ProcessInstanceModificationIntent.MODIFY);
    assertThat(modificationCommands.getFirst().getAuthorizations()).isEqualTo(claims);
  }

  @Test
  public void shouldModifyProcessInstanceFail() {
    // given
    final Map<String, Object> claims = Map.of("claim1", "value1", "claim2", "value2");

    // create a process with two user tasks
    final long processDefinitionKey =
        engine
            .deployment()
            .withXmlResource(
                Bpmn.createExecutableProcess("process")
                    .startEvent()
                    .userTask("userTaskA")
                    .userTask("userTaskB")
                    .done())
            .deploy()
            .getValue()
            .getProcessesMetadata()
            .get(0)
            .getProcessDefinitionKey();

    final var processInstanceKey = engine.processInstance().ofBpmnProcessId("process").create();

    // wait for the user task to exist
    RecordingExporter.jobRecords()
        .withProcessInstanceKey(processInstanceKey)
        .withIntent(JobIntent.CREATED)
        .getFirst();

    // then start the batch where we modify the process instance and move the token the userTaskB
    final var batchOperationKey =
        createNewModifyProcessInstanceBatchOperation(
            Set.of(processInstanceKey), "userTaskA", "userTaskC", claims);

    // then we have executed and completed event
    assertThat(
            RecordingExporter.batchOperationExecutionRecords()
                .withBatchOperationKey(batchOperationKey)
                .onlyEvents())
        .extracting(Record::getIntent)
        .containsSequence(
            BatchOperationExecutionIntent.EXECUTED, BatchOperationExecutionIntent.COMPLETED);

    // and a follow op up command to execute again
    assertThat(
            RecordingExporter.batchOperationExecutionRecords()
                .withBatchOperationKey(batchOperationKey)
                .onlyCommands())
        .extracting(Record::getIntent)
        .containsSequence(BatchOperationExecutionIntent.EXECUTE);

    // and we have a modified event
    assertThat(
            RecordingExporter.processInstanceModificationRecords()
                .withRecordKey(processInstanceKey)
                .onlyCommandRejections())
        .extracting(Record::getIntent)
        .containsSequence(ProcessInstanceModificationIntent.MODIFY);
  }

  @Test
  public void shouldModifyProcessInstanceFailNoProcessInstance() {
    // given
    final Map<String, Object> claims = Map.of("claim1", "value1", "claim2", "value2");

    // then start the batch where we modify the non-existing process instance
    final var batchOperationKey =
        createNewModifyProcessInstanceBatchOperation(Set.of(42L), "userTaskA", "userTaskC", claims);

    // then we have executed and completed event
    assertThat(
            RecordingExporter.batchOperationExecutionRecords()
                .withBatchOperationKey(batchOperationKey)
                .onlyEvents())
        .extracting(Record::getIntent)
        .containsSequence(
            BatchOperationExecutionIntent.EXECUTED, BatchOperationExecutionIntent.COMPLETED);

    // and a follow op up command to execute again
    assertThat(
            RecordingExporter.batchOperationExecutionRecords()
                .withBatchOperationKey(batchOperationKey)
                .onlyCommands())
        .extracting(Record::getIntent)
        .containsSequence(BatchOperationExecutionIntent.EXECUTE);

    // and we have a modified event
    assertThat(
            RecordingExporter.processInstanceModificationRecords()
                .withRecordKey(42L)
                .onlyCommandRejections())
        .extracting(Record::getIntent)
        .containsSequence(ProcessInstanceModificationIntent.MODIFY);
  }
}
