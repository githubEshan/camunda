/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.spring.client.properties;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.spring.client.properties.CamundaClientProperties.ClientMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    classes = CamundaClientPropertiesTestConfig.class,
    properties = {"camunda.client.mode=self-managed", "camunda.client.auth.scope=zeebe-scope"})
public class ZeebeClientPropertiesSelfManagedTest {
  @Autowired CamundaClientProperties properties;

  @Test
  void shouldLoadDefaultsSelfManaged() {
    assertThat(properties.getMode()).isEqualTo(ClientMode.selfManaged);
    assertThat(properties.getGrpcAddress().toString()).isEqualTo("http://localhost:26500");
    assertThat(properties.getRestAddress().toString()).isEqualTo("http://localhost:8086");
    assertThat(properties.getPreferRestOverGrpc()).isEqualTo(false);
    assertThat(properties.getEnabled()).isEqualTo(true);
    assertThat(properties.getAuth().getAudience()).isEqualTo("zeebe-api");
    assertThat(properties.getAuth().getScope()).isEqualTo("zeebe-scope");
  }
}
