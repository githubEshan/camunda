/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.test.util.asserts.grpc;

import io.camunda.client.api.command.ClientStatusException;
import io.grpc.Status;
import java.util.function.Consumer;
import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.InstanceOfAssertFactory;

public final class ClientStatusExceptionAssert
    extends AbstractThrowableAssert<ClientStatusExceptionAssert, ClientStatusException> {
  private static final InstanceOfAssertFactory<ClientStatusException, ClientStatusExceptionAssert>
      ASSERT_FACTORY =
          new InstanceOfAssertFactory<>(
              ClientStatusException.class, ClientStatusExceptionAssert::assertThat);

  public ClientStatusExceptionAssert(final ClientStatusException e) {
    super(e, ClientStatusExceptionAssert.class);
  }

  public static ClientStatusExceptionAssert assertThat(final ClientStatusException e) {
    return new ClientStatusExceptionAssert(e);
  }

  public static InstanceOfAssertFactory<ClientStatusException, ClientStatusExceptionAssert>
      assertFactory() {
    return ASSERT_FACTORY;
  }

  public ClientStatusExceptionAssert hasStatusSatisfying(final Consumer<Status> statusAssertions) {
    statusAssertions.accept(actual.getStatus());
    return myself;
  }
}
