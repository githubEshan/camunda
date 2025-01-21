/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.it.client;

import static io.camunda.it.client.QueryTest.assertSorted;
import static io.camunda.it.client.QueryTest.deployResource;
import static io.camunda.it.client.QueryTest.startProcessInstance;
import static io.camunda.it.client.QueryTest.waitForFlowNodeInstances;
import static io.camunda.it.client.QueryTest.waitForProcessInstancesToStart;
import static io.camunda.it.client.QueryTest.waitForProcessesToBeDeployed;
import static io.camunda.it.client.QueryTest.waitUntilFlowNodeInstanceHasIncidents;
import static io.camunda.it.client.QueryTest.waitUntilProcessInstanceHasIncidents;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.command.ProblemException;
import io.camunda.client.api.response.DeploymentEvent;
import io.camunda.client.api.response.Process;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.client.api.search.response.FlowNodeInstance;
import io.camunda.client.api.search.response.ProcessInstance;
import io.camunda.client.protocol.rest.ProcessInstanceStateEnum;
import io.camunda.client.protocol.rest.ProcessInstanceVariableFilterRequest;
import io.camunda.it.utils.BrokerITInvocationProvider;
import io.camunda.zeebe.qa.util.cluster.TestStandaloneBroker;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BrokerITInvocationProvider.class)
public class ProcessInstanceAndFlowNodeInstanceQueryTest {

  private static final List<String> PROCESS_DEFINITIONS = List.of(
      "service_tasks_v1.bpmn",
      "service_tasks_v2.bpmn",
      "incident_process_v1.bpmn",
      "manual_process.bpmn",
      "parent_process_v1.bpmn",
      "child_process_v1.bpmn"
  );

  private static FlowNodeInstance flowNodeInstance;
  private static FlowNodeInstance flowNodeInstanceWithIncident;

  public List<Process> deployProcesses(final CamundaClient camundaClient) throws InterruptedException {

    final var processes = PROCESS_DEFINITIONS.parallelStream()
        .map(process -> deployResource(camundaClient, String.format("process/%s", process)))
        .map(DeploymentEvent::getProcesses)
        .flatMap(List::stream)
        .toList();

    waitForProcessesToBeDeployed(camundaClient, processes.size());

    return processes;
  }

  public List<ProcessInstanceEvent> startProcesses(final CamundaClient camundaClient) {

    final var processInstances = new ArrayList<ProcessInstanceEvent>();

    processInstances.add(
        startProcessInstance(camundaClient, "service_tasks_v1", "{\"xyz\":\"bar\"}"));
    processInstances.add(
        startProcessInstance(camundaClient, "service_tasks_v2", "{\"path\":222}"));
    processInstances.add(startProcessInstance(camundaClient, "manual_process"));
    processInstances.add(startProcessInstance(camundaClient, "incident_process_v1"));
    processInstances.add(startProcessInstance(camundaClient, "parent_process_v1"));

    waitForProcessInstancesToStart(camundaClient, 6);
    waitForFlowNodeInstances(camundaClient, 20);
    waitUntilFlowNodeInstanceHasIncidents(camundaClient, 1);
    waitUntilProcessInstanceHasIncidents(camundaClient, 1);
    // store flow node instances for querying
    final var allFlowNodeInstances =
        camundaClient
            .newFlownodeInstanceQuery()
            .page(p -> p.limit(100))
            .sort(s -> s.flowNodeId().asc())
            .send()
            .join()
            .items();
    flowNodeInstance = allFlowNodeInstances.getFirst();
    flowNodeInstanceWithIncident =
        allFlowNodeInstances.stream()
            .filter(f -> f.getIncidentKey() != null)
            .findFirst()
            .orElseThrow();

    return processInstances;
  }

  @TestTemplate
  void shouldGetProcessInstanceByKey(final TestStandaloneBroker testBroker) throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployProcesses(camundaClient);
    final var processes = startProcesses(camundaClient);

    final String bpmnProcessId = "service_tasks_v1";
    final ProcessInstanceEvent processInstanceEvent =
        processes.stream()
            .filter(p -> Objects.equals(bpmnProcessId, p.getBpmnProcessId()))
            .findFirst()
            .orElseThrow();
    final long processInstanceKey = processInstanceEvent.getProcessInstanceKey();
    final long processDefinitionKey = processInstanceEvent.getProcessDefinitionKey();

    // when
    final var result = camundaClient.newProcessInstanceGetRequest(processInstanceKey).send().join();

    // then
    assertThat(result).isNotNull();
    assertThat(result.getProcessInstanceKey()).isEqualTo(processInstanceKey);
    assertThat(result.getProcessDefinitionId()).isEqualTo(bpmnProcessId);
    assertThat(result.getProcessDefinitionName()).isEqualTo("Service tasks v1");
    assertThat(result.getProcessDefinitionVersion()).isEqualTo(1);
    assertThat(result.getProcessDefinitionKey()).isEqualTo(processDefinitionKey);
    assertThat(result.getStartDate()).isNotNull();
    assertThat(result.getEndDate()).isNull();
    assertThat(result.getState()).isEqualTo("ACTIVE");
    assertThat(result.getHasIncident()).isFalse();
    assertThat(result.getTenantId()).isEqualTo("<default>");
  }

  @TestTemplate
  void shouldThrownExceptionIfProcessInstanceNotFoundByKey(final TestStandaloneBroker testBroker) throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployProcesses(camundaClient);
    startProcesses(camundaClient);
    final long invalidProcessInstanceKey = 100L;

    // when / then
    final var exception =
        assertThrowsExactly(
            ProblemException.class,
            () ->
                camundaClient
                    .newProcessInstanceGetRequest(invalidProcessInstanceKey)
                    .send()
                    .join());
    assertThat(exception.getMessage()).startsWith("Failed with code 404");
    assertThat(exception.details()).isNotNull();
    assertThat(exception.details().getTitle()).isEqualTo("NOT_FOUND");
    assertThat(exception.details().getStatus()).isEqualTo(404);
    assertThat(exception.details().getDetail())
        .isEqualTo("Process instance with key 100 not found");
  }

  @TestTemplate
  void shouldQueryAllProcessInstancesByDefault(final TestStandaloneBroker testBroker) throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployProcesses(camundaClient);
    final var processInstances = startProcesses(camundaClient);

    final List<String> expectedBpmnProcessIds =
        new ArrayList<>(
            processInstances.stream().map(ProcessInstanceEvent::getBpmnProcessId).toList());
    expectedBpmnProcessIds.add("child_process_v1");

    // when
    final var result = camundaClient.newProcessInstanceQuery().send().join();

    // then
    assertThat(result.page().totalItems()).isEqualTo(expectedBpmnProcessIds.size());
    assertThat(result.items().stream().map(ProcessInstance::getProcessDefinitionId).toList())
        .containsExactlyInAnyOrderElementsOf(expectedBpmnProcessIds);
  }

  @TestTemplate
  void shouldQueryProcessInstancesByKey(final TestStandaloneBroker testBroker) throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployProcesses(camundaClient);
    final var processInstances = startProcesses(camundaClient);

    final long processInstanceKey =
        processInstances.stream().findFirst().orElseThrow().getProcessInstanceKey();

    // when
    final var result =
        camundaClient
            .newProcessInstanceQuery()
            .filter(f -> f.processInstanceKey(processInstanceKey))
            .send()
            .join();

    // then
    assertThat(result.items().size()).isEqualTo(1);
    assertThat(result.items().getFirst().getProcessInstanceKey()).isEqualTo(processInstanceKey);
  }

  @TestTemplate
  void shouldQueryProcessInstancesByKeyFilterGtLt(final TestStandaloneBroker testBroker) throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployProcesses(camundaClient);
    final var processInstances = startProcesses(camundaClient);
    final List<Long> processInstanceKeys =
        processInstances.subList(0, 2).stream()
            .map(ProcessInstanceEvent::getProcessInstanceKey)
            .toList();

    // when
    final var result =
        camundaClient
            .newProcessInstanceQuery()
            .filter(f -> f.processInstanceKey(b -> b.in(processInstanceKeys)))
            .send()
            .join();

    // then
    assertThat(result.items().size()).isEqualTo(2);
    assertThat(result.items())
        .extracting("processInstanceKey")
        .containsExactlyInAnyOrderElementsOf(processInstanceKeys);
  }

  @TestTemplate
  void shouldQueryProcessInstancesByProcessDefinitionId(final TestStandaloneBroker testBroker) throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployProcesses(camundaClient);
    final var processInstances = startProcesses(camundaClient);
    final String bpmnProcessId = "service_tasks_v1";
    final long processInstanceKey =
        processInstances.stream()
            .filter(p -> Objects.equals(bpmnProcessId, p.getBpmnProcessId()))
            .findFirst()
            .orElseThrow()
            .getProcessInstanceKey();

    // when
    final var result =
        camundaClient
            .newProcessInstanceQuery()
            .filter(f -> f.processDefinitionId(b -> b.eq(bpmnProcessId)))
            .send()
            .join();

    // then
    assertThat(result.items().size()).isEqualTo(1);
    assertThat(result.items().getFirst().getProcessInstanceKey()).isEqualTo(processInstanceKey);
  }

  @TestTemplate
  void shouldQueryProcessInstancesByProcessDefinitionIdFilterIn(final TestStandaloneBroker testBroker) throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployProcesses(camundaClient);
    final var processInstances = startProcesses(camundaClient);
    final String bpmnProcessId = "service_tasks_v1";
    final long processInstanceKey =
        processInstances.stream()
            .filter(p -> Objects.equals(bpmnProcessId, p.getBpmnProcessId()))
            .findFirst()
            .orElseThrow()
            .getProcessInstanceKey();

    // when
    final var result =
        camundaClient
            .newProcessInstanceQuery()
            .filter(f -> f.processDefinitionId(b -> b.in("not-found", bpmnProcessId)))
            .send()
            .join();

    // then
    assertThat(result.items()).hasSize(1);
    assertThat(result.items()).extracting("processInstanceKey").containsExactly(processInstanceKey);
  }

  @TestTemplate
  void shouldRetrieveProcessInstancesByProcessDefinitionIdFilterLikeMultiple(final TestStandaloneBroker testBroker) throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployProcesses(camundaClient);
    final var processInstances = startProcesses(camundaClient);
    final String bpmnProcessId = "service_tasks";
    final List<Long> processInstanceKeys =
        processInstances.stream()
            .filter(p -> p.getBpmnProcessId().startsWith(bpmnProcessId))
            .map(ProcessInstanceEvent::getProcessInstanceKey)
            .toList();

    // when
    final var result =
        camundaClient
            .newProcessInstanceQuery()
            .filter(f -> f.processDefinitionId(b -> b.like(bpmnProcessId.replace("_", "?") + "*")))
            .send()
            .join();

    // then
    assertThat(result.items()).hasSize(2);
    assertThat(result.items())
        .extracting("processInstanceKey")
        .containsExactlyInAnyOrder(processInstanceKeys.toArray());
  }

  @TestTemplate
  void shouldRetrieveProcessInstancesByStartDateFilterGtLt(final TestStandaloneBroker testBroker) throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployProcesses(camundaClient);
    startProcesses(camundaClient);
    final var pi =
        camundaClient
            .newProcessInstanceQuery()
            .page(p -> p.limit(1))
            .send()
            .join()
            .items()
            .getFirst();
    final var startDate = OffsetDateTime.parse(pi.getStartDate());

    // when
    final var result =
        camundaClient
            .newProcessInstanceQuery()
            .filter(
                f ->
                    f.startDate(
                        b ->
                            b.gt(startDate.minus(1, ChronoUnit.MILLIS))
                                .lt(startDate.plus(1, ChronoUnit.MILLIS))))
            .send()
            .join();

    // then
    assertThat(result.items()).hasSize(1);
    assertThat(result.items().getFirst().getProcessInstanceKey())
        .isEqualTo(pi.getProcessInstanceKey());
  }

  @TestTemplate
  void shouldRetrieveProcessInstancesByExistEndDates(final TestStandaloneBroker testBroker) throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // when
    deployProcesses(camundaClient);
    startProcesses(camundaClient);
    final var result =
        camundaClient
            .newProcessInstanceQuery()
            .filter(f -> f.endDate(b -> b.exists(true)))
            .send()
            .join();

    // then
    // validate all end dates are not null
    assertThat(result.items()).allMatch(p -> p.getEndDate() != null);
  }

  @TestTemplate
  void shouldRetrieveProcessInstancesByNotExistEndDates(final TestStandaloneBroker testBroker) throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // when
    deployProcesses(camundaClient);
    startProcesses(camundaClient);
    final var result =
        camundaClient
            .newProcessInstanceQuery()
            .filter(f -> f.endDate(b -> b.exists(false)))
            .send()
            .join();

    // then
    // validate all end dates are not null
    assertThat(result.items()).allMatch(p -> p.getEndDate() == null);
  }

  @TestTemplate
  void shouldRetrieveProcessInstancesByEndDateFilterGteLte(final TestStandaloneBroker testBroker) throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployProcesses(camundaClient);
    startProcesses(camundaClient);
    final var pi =
        camundaClient
            .newProcessInstanceQuery()
            .page(p -> p.limit(1))
            .send()
            .join()
            .items()
            .getFirst();
    final var startDate = OffsetDateTime.parse(pi.getStartDate());

    // when
    final var result =
        camundaClient
            .newProcessInstanceQuery()
            .filter(f -> f.startDate(b -> b.gte(startDate).lte(startDate)))
            .send()
            .join();

    // then
    assertThat(result.items()).hasSize(1);
    assertThat(result.items().getFirst().getProcessInstanceKey())
        .isEqualTo(pi.getProcessInstanceKey());
  }

  @TestTemplate
  void shouldQueryProcessInstancesByProcessDefinitionKey(final TestStandaloneBroker testBroker) throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployProcesses(camundaClient);
    final var processInstances = startProcesses(camundaClient);
    final String bpmnProcessId = "service_tasks_v1";
    final ProcessInstanceEvent processInstanceEvent =
        processInstances.stream()
            .filter(p -> Objects.equals(bpmnProcessId, p.getBpmnProcessId()))
            .findFirst()
            .orElseThrow();
    final long processInstanceKey = processInstanceEvent.getProcessInstanceKey();
    final long processDefinitionKey = processInstanceEvent.getProcessDefinitionKey();

    // when
    final var result =
        camundaClient
            .newProcessInstanceQuery()
            .filter(f -> f.processDefinitionKey(processDefinitionKey))
            .send()
            .join();

    // then
    assertThat(result.items().size()).isEqualTo(1);
    assertThat(result.items().getFirst().getProcessInstanceKey()).isEqualTo(processInstanceKey);
  }

  @TestTemplate
  void shouldQueryProcessInstancesByStateActive(final TestStandaloneBroker testBroker) throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // when
    deployProcesses(camundaClient);
    startProcesses(camundaClient);
    final var result =
        camundaClient.newProcessInstanceQuery().filter(f -> f.state("ACTIVE")).send().join();

    // then
    assertThat(result.items().size()).isEqualTo(3);
    assertThat(result.items().stream().map(ProcessInstance::getProcessDefinitionId).toList())
        .containsExactlyInAnyOrder("service_tasks_v1", "service_tasks_v2", "incident_process_v1");
  }

  @TestTemplate
  void shouldQueryProcessInstancesByStateFilterLike(final TestStandaloneBroker testBroker) throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // when
    deployProcesses(camundaClient);
    startProcesses(camundaClient);
    final var result =
        camundaClient
            .newProcessInstanceQuery()
            .filter(f -> f.state(b -> b.like("ACT*")))
            .send()
            .join();

    // then
    assertThat(result.items().size()).isEqualTo(3);
    assertThat(result.items().stream().map(ProcessInstance::getProcessDefinitionId).toList())
        .containsExactlyInAnyOrder("service_tasks_v1", "service_tasks_v2", "incident_process_v1");
  }

  @TestTemplate
  void shouldQueryProcessInstancesByStateFilterNeq(final TestStandaloneBroker testBroker) throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // when
    deployProcesses(camundaClient);
    startProcesses(camundaClient);
    final var result =
        camundaClient
            .newProcessInstanceQuery()
            .filter(f -> f.state(b -> b.neq(ProcessInstanceStateEnum.ACTIVE)))
            .send()
            .join();

    // then
    assertThat(result.items().size()).isEqualTo(3);
    assertThat(result.items()).extracting("state").doesNotContain(ProcessInstanceStateEnum.ACTIVE);
  }
  @TestTemplate
  void shouldQueryProcessInstancesByStateCompleted(final TestStandaloneBroker testBroker) throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // when
    deployProcesses(camundaClient);
    startProcesses(camundaClient);
    final var result =
        camundaClient.newProcessInstanceQuery().filter(f -> f.state("COMPLETED")).send().join();

    // then
    assertThat(result.items().size()).isEqualTo(3);
    assertThat(result.items().stream().map(ProcessInstance::getProcessDefinitionId).toList())
        .containsExactlyInAnyOrder("parent_process_v1", "child_process_v1", "manual_process");
  }

  @TestTemplate
  void shouldQueryProcessInstancesWithIncidents(final TestStandaloneBroker testBroker) throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // when
    deployProcesses(camundaClient);
    startProcesses(camundaClient);
    final var result =
        camundaClient.newProcessInstanceQuery().filter(f -> f.hasIncident(true)).send().join();

    // then
    assertThat(result.items().size()).isEqualTo(1);
    assertThat(result.items().stream().map(ProcessInstance::getProcessDefinitionId).toList())
        .containsExactlyInAnyOrder("incident_process_v1");
  }

  @TestTemplate
  void shouldQueryProcessInstancesByParentProcessInstanceKey(final TestStandaloneBroker testBroker) throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployProcesses(camundaClient);
    final var processInstances = startProcesses(camundaClient);
    final long parentProcessInstanceKey =
        processInstances.stream()
            .filter(p -> Objects.equals("parent_process_v1", p.getBpmnProcessId()))
            .findFirst()
            .orElseThrow()
            .getProcessInstanceKey();

    // when
    final var result =
        camundaClient
            .newProcessInstanceQuery()
            .filter(f -> f.parentProcessInstanceKey(parentProcessInstanceKey))
            .send()
            .join();

    // then
    assertThat(result.items().size()).isEqualTo(1);
    assertThat(result.items().stream().map(ProcessInstance::getProcessDefinitionId).toList())
        .containsExactlyInAnyOrder("child_process_v1");
  }

  @TestTemplate
  void shouldQueryProcessInstancesWithReverseSorting(final TestStandaloneBroker testBroker) throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployProcesses(camundaClient);
    startProcesses(camundaClient);
    final List<String> expectedBpmnProcessIds =
        new ArrayList<>(
            camundaClient.newProcessInstanceQuery().send().join().items().stream()
                .map(ProcessInstance::getProcessDefinitionId)
                .toList());
    expectedBpmnProcessIds.sort(Comparator.reverseOrder());

    // when
    final var result =
        camundaClient
            .newProcessInstanceQuery()
            .sort(s -> s.processDefinitionId().desc())
            .send()
            .join();

    // then
    assertThat(result.items().size()).isEqualTo(expectedBpmnProcessIds.size());
    assertThat(result.items().stream().map(ProcessInstance::getProcessDefinitionId).toList())
        .containsExactlyElementsOf(expectedBpmnProcessIds);
  }

  @TestTemplate
  void shouldQueryProcessInstancesByVariableSingle(final TestStandaloneBroker testBroker) throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployProcesses(camundaClient);
    startProcesses(camundaClient);
    final List<ProcessInstanceVariableFilterRequest> variables =
        List.of(new ProcessInstanceVariableFilterRequest().name("xyz").value("\"bar\""));

    // when
    final var result =
        camundaClient.newProcessInstanceQuery().filter(f -> f.variables(variables)).send().join();

    // then
    assertThat(result.items().size()).isEqualTo(1);
    assertThat(result.items().stream().map(ProcessInstance::getProcessDefinitionId).toList())
        .containsExactlyInAnyOrder("service_tasks_v1");
  }

  @TestTemplate
  void shouldQueryProcessInstancesByVariableSingleUsingMap(final TestStandaloneBroker testBroker) throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployProcesses(camundaClient);
    startProcesses(camundaClient);

    // when
    final var result =
        camundaClient
            .newProcessInstanceQuery()
            .filter(f -> f.variables(Map.of("xyz", "\"bar\"")))
            .send()
            .join();

    // then
    assertThat(result.items().size()).isEqualTo(1);
    assertThat(result.items().stream().map(ProcessInstance::getProcessDefinitionId).toList())
        .containsExactlyInAnyOrder("service_tasks_v1");
  }

  @TestTemplate
  void shouldQueryProcessInstancesByVariableMulti(final TestStandaloneBroker testBroker) throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // given
    deployProcesses(camundaClient);
    startProcesses(camundaClient);
    final List<ProcessInstanceVariableFilterRequest> variables =
        List.of(
            new ProcessInstanceVariableFilterRequest().name("xyz").value("\"bar\""),
            new ProcessInstanceVariableFilterRequest().name("path").value("222"));

    // when
    final var result =
        camundaClient.newProcessInstanceQuery().filter(f -> f.variables(variables)).send().join();

    // then
    assertThat(result.items().size()).isEqualTo(2);
    assertThat(result.items().stream().map(ProcessInstance::getProcessDefinitionId).toList())
        .containsExactlyInAnyOrder("service_tasks_v1", "service_tasks_v2");
  }

  @TestTemplate
  void shouldSortProcessInstancesByProcessInstanceKey(final TestStandaloneBroker testBroker) throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // when
    deployProcesses(camundaClient);
    startProcesses(camundaClient);
    final var resultAsc =
        camundaClient
            .newProcessInstanceQuery()
            .sort(s -> s.processInstanceKey().asc())
            .send()
            .join();
    final var resultDesc =
        camundaClient
            .newProcessInstanceQuery()
            .sort(s -> s.processInstanceKey().desc())
            .send()
            .join();

    assertSorted(resultAsc, resultDesc, ProcessInstance::getProcessInstanceKey);
  }

  @TestTemplate
  void shouldSortProcessInstancesByProcessDefinitionName(final TestStandaloneBroker testBroker) throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // when
    deployProcesses(camundaClient);
    startProcesses(camundaClient);
    final var resultAsc =
        camundaClient
            .newProcessInstanceQuery()
            .sort(s -> s.processDefinitionName().asc())
            .send()
            .join();
    final var resultDesc =
        camundaClient
            .newProcessInstanceQuery()
            .sort(s -> s.processDefinitionName().desc())
            .send()
            .join();

    assertSorted(resultAsc, resultDesc, ProcessInstance::getProcessDefinitionName);
  }

  @TestTemplate
  void shouldSortProcessInstancesByProcessDefinitionKey(final TestStandaloneBroker testBroker) throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // when
    deployProcesses(camundaClient);
    startProcesses(camundaClient);
    final var resultAsc =
        camundaClient
            .newProcessInstanceQuery()
            .sort(s -> s.processDefinitionKey().asc())
            .send()
            .join();
    final var resultDesc =
        camundaClient
            .newProcessInstanceQuery()
            .sort(s -> s.processDefinitionKey().desc())
            .send()
            .join();

    assertSorted(resultAsc, resultDesc, ProcessInstance::getProcessDefinitionKey);
  }

  @TestTemplate
  void shouldSortProcessInstancesByParentProcessInstanceKey(final TestStandaloneBroker testBroker) throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // when
    deployProcesses(camundaClient);
    startProcesses(camundaClient);
    final var resultAsc =
        camundaClient
            .newProcessInstanceQuery()
            .sort(s -> s.parentProcessInstanceKey().asc())
            .send()
            .join();
    final var resultDesc =
        camundaClient
            .newProcessInstanceQuery()
            .sort(s -> s.parentFlowNodeInstanceKey().desc())
            .send()
            .join();

    assertSorted(resultAsc, resultDesc, ProcessInstance::getParentProcessInstanceKey);
  }

  @TestTemplate
  void shouldSortProcessInstancesByStartDate(final TestStandaloneBroker testBroker) throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // when
    deployProcesses(camundaClient);
    startProcesses(camundaClient);
    final var resultAsc =
        camundaClient.newProcessInstanceQuery().sort(s -> s.startDate().asc()).send().join();
    final var resultDesc =
        camundaClient.newProcessInstanceQuery().sort(s -> s.startDate().desc()).send().join();

    assertSorted(resultAsc, resultDesc, ProcessInstance::getStartDate);
  }

  @TestTemplate
  void shouldSortProcessInstancesByState(final TestStandaloneBroker testBroker) throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // when
    deployProcesses(camundaClient);
    startProcesses(camundaClient);
    final var resultAsc =
        camundaClient.newProcessInstanceQuery().sort(s -> s.state().asc()).send().join();
    final var resultDesc =
        camundaClient.newProcessInstanceQuery().sort(s -> s.state().desc()).send().join();

    assertSorted(resultAsc, resultDesc, ProcessInstance::getState);
  }

  @TestTemplate
  void shouldValidateProcessInstancePagination(final TestStandaloneBroker testBroker) throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // when
    deployProcesses(camundaClient);
    startProcesses(camundaClient);
    final var result = camundaClient.newProcessInstanceQuery().page(p -> p.limit(2)).send().join();
    assertThat(result.items().size()).isEqualTo(2);
    final var key = result.items().getFirst().getProcessInstanceKey();
    // apply searchAfter
    final var resultAfter =
        camundaClient
            .newProcessInstanceQuery()
            .page(p -> p.searchAfter(Collections.singletonList(key)))
            .send()
            .join();

    assertThat(resultAfter.items().size()).isEqualTo(5);
    final var keyAfter = resultAfter.items().getFirst().getProcessInstanceKey();
    // apply searchBefore
    final var resultBefore =
        camundaClient
            .newProcessInstanceQuery()
            .page(p -> p.searchBefore(Collections.singletonList(keyAfter)))
            .send()
            .join();
    assertThat(result.items().size()).isEqualTo(2);
    assertThat(resultBefore.items().getFirst().getProcessInstanceKey()).isEqualTo(key);
  }

  @TestTemplate
  void shouldValidateFlowNodeInstancePagination(final TestStandaloneBroker testBroker) throws InterruptedException {
    final var camundaClient = testBroker.newClientBuilder().build();

    // when
    deployProcesses(camundaClient);
    startProcesses(camundaClient);
    final var result = camundaClient.newFlownodeInstanceQuery().page(p -> p.limit(2)).send().join();
    assertThat(result.items().size()).isEqualTo(2);
    final var key = result.items().getFirst().getFlowNodeInstanceKey();
    // apply searchAfter
    final var resultAfter =
        camundaClient
            .newFlownodeInstanceQuery()
            .page(p -> p.searchAfter(Collections.singletonList(key)))
            .send()
            .join();

    assertThat(resultAfter.items().size()).isEqualTo(19);
    final var keyAfter = resultAfter.items().getFirst().getFlowNodeInstanceKey();
    // apply searchBefore
    final var resultBefore =
        camundaClient
            .newFlownodeInstanceQuery()
            .page(p -> p.searchBefore(Collections.singletonList(keyAfter)))
            .send()
            .join();
    assertThat(result.items().size()).isEqualTo(2);
    assertThat(resultBefore.items().getFirst().getFlowNodeInstanceKey()).isEqualTo(key);
  }
}
