/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.it.client;

import static io.camunda.it.util.TestHelper.deployResource;
import static io.camunda.it.util.TestHelper.waitForProcessesToBeDeployed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.Process;
import io.camunda.client.api.search.enums.AdHocSubProcessActivityResultType;
import io.camunda.client.api.search.response.AdHocSubProcessActivityResponse.AdHocSubProcessActivity;
import io.camunda.qa.util.multidb.MultiDbTest;
import org.junit.jupiter.api.Test;

@MultiDbTest
public class AdHocSubProcessActivitySearchTest {

  private static CamundaClient camundaClient;

  @Test
  void findsAdHocSubProcessActivities() {
    final var process = deployAdHocSubProcessProcess();
    final var response =
        camundaClient
            .newAdHocSubProcessActivitySearchRequest(
                process.getProcessDefinitionKey(), "TestAdHocSubProcess")
            .send()
            .join();

    assertThat(response.getItems())
        .hasSize(2)
        // TestServiceTask is no root node (has incoming sequence flow)
        .noneMatch(activity -> activity.getElementId().equals("TestServiceTask"))
        .extracting(
            AdHocSubProcessActivity::getProcessDefinitionKey,
            AdHocSubProcessActivity::getProcessDefinitionId,
            AdHocSubProcessActivity::getAdHocSubProcessId,
            AdHocSubProcessActivity::getElementId,
            AdHocSubProcessActivity::getElementName,
            AdHocSubProcessActivity::getType,
            AdHocSubProcessActivity::getDocumentation,
            AdHocSubProcessActivity::getTenantId)
        .containsExactlyInAnyOrder(
            tuple(
                process.getProcessDefinitionKey(),
                process.getBpmnProcessId(),
                "TestAdHocSubProcess",
                "TestScriptTask",
                "test script task",
                AdHocSubProcessActivityResultType.SCRIPT_TASK,
                "This is a test script task",
                "<default>"),
            tuple(
                process.getProcessDefinitionKey(),
                process.getBpmnProcessId(),
                "TestAdHocSubProcess",
                "TestUserTask",
                "test user task",
                AdHocSubProcessActivityResultType.USER_TASK,
                null,
                "<default>"));
  }

  private Process deployAdHocSubProcessProcess() {
    final var deployedProcesses =
        deployResource(camundaClient, "process/ad_hoc_subprocess_activities.bpmn").getProcesses();
    assertThat(deployedProcesses).hasSize(1);

    waitForProcessesToBeDeployed(camundaClient, 1);

    return deployedProcesses.getFirst();
  }
}
