/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.it.exporter;

import static io.camunda.it.exporter.ExporterTestUtil.PROCESS_ID_MANUAL_PROCESS;
import static io.camunda.it.exporter.ExporterTestUtil.RESOURCE_MANUAL_PROCESS;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.it.utils.BrokerITInvocationProvider;
import io.camunda.zeebe.qa.util.cluster.TestStandaloneBroker;
import java.time.Duration;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BrokerITInvocationProvider.class)
final class ProcessInstanceExporterIT {

  @TestTemplate
  void shouldExportCompletedProcessInstance(final TestStandaloneBroker testBroker) {
    // given
    final var client = testBroker.newClientBuilder().build();

    final var resource =
        client
            .newDeployResourceCommand()
            .addResourceFromClasspath(RESOURCE_MANUAL_PROCESS)
            .send()
            .join();

    final var processInstanceId = ExporterTestUtil.startProcessInstance(client, PROCESS_ID_MANUAL_PROCESS);

    // then
    Awaitility.await()
        .ignoreExceptions()
        .timeout(Duration.ofSeconds(30))
        .until(() -> client.newProcessInstanceQuery()
            .filter(f -> f.processInstanceKey(processInstanceId).state("COMPLETED"))
            .send().join().items().size() == 1);
  }
}
