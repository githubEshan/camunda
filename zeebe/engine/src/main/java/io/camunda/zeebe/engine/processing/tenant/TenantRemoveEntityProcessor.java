/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.processing.tenant;

import io.camunda.zeebe.engine.processing.Rejection;
import io.camunda.zeebe.engine.processing.distribution.CommandDistributionBehavior;
import io.camunda.zeebe.engine.processing.identity.AuthorizationCheckBehavior;
import io.camunda.zeebe.engine.processing.identity.AuthorizationCheckBehavior.AuthorizationRequest;
import io.camunda.zeebe.engine.processing.streamprocessor.DistributedTypedRecordProcessor;
import io.camunda.zeebe.engine.processing.streamprocessor.writers.StateWriter;
import io.camunda.zeebe.engine.processing.streamprocessor.writers.TypedRejectionWriter;
import io.camunda.zeebe.engine.processing.streamprocessor.writers.TypedResponseWriter;
import io.camunda.zeebe.engine.processing.streamprocessor.writers.Writers;
import io.camunda.zeebe.engine.state.distribution.DistributionQueue;
import io.camunda.zeebe.engine.state.immutable.GroupState;
import io.camunda.zeebe.engine.state.immutable.MappingState;
import io.camunda.zeebe.engine.state.immutable.ProcessingState;
import io.camunda.zeebe.engine.state.immutable.TenantState;
import io.camunda.zeebe.engine.state.immutable.UserState;
import io.camunda.zeebe.protocol.impl.record.value.tenant.TenantRecord;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.intent.TenantIntent;
import io.camunda.zeebe.protocol.record.value.AuthorizationResourceType;
import io.camunda.zeebe.protocol.record.value.EntityType;
import io.camunda.zeebe.protocol.record.value.PermissionType;
import io.camunda.zeebe.stream.api.records.TypedRecord;
import io.camunda.zeebe.stream.api.state.KeyGenerator;

public class TenantRemoveEntityProcessor implements DistributedTypedRecordProcessor<TenantRecord> {

  private static final String ENTITY_NOT_ASSIGNED_ERROR_MESSAGE =
      "Expected to remove entity with key '%s' from tenant with key '%s', but the entity is not assigned to this tenant.";
  private final TenantState tenantState;
  private final UserState userState;
  private final MappingState mappingState;
  private final GroupState groupState;
  private final AuthorizationCheckBehavior authCheckBehavior;
  private final KeyGenerator keyGenerator;
  private final StateWriter stateWriter;
  private final TypedRejectionWriter rejectionWriter;
  private final TypedResponseWriter responseWriter;
  private final CommandDistributionBehavior commandDistributionBehavior;

  public TenantRemoveEntityProcessor(
      final ProcessingState state,
      final AuthorizationCheckBehavior authCheckBehavior,
      final KeyGenerator keyGenerator,
      final Writers writers,
      final CommandDistributionBehavior commandDistributionBehavior) {
    tenantState = state.getTenantState();
    userState = state.getUserState();
    mappingState = state.getMappingState();
    groupState = state.getGroupState();
    this.authCheckBehavior = authCheckBehavior;
    this.keyGenerator = keyGenerator;
    stateWriter = writers.state();
    rejectionWriter = writers.rejection();
    responseWriter = writers.response();
    this.commandDistributionBehavior = commandDistributionBehavior;
  }

  @Override
  public void processNewCommand(final TypedRecord<TenantRecord> command) {
    final var record = command.getValue();
    final var tenantKey = record.getTenantKey();
    final var persistedTenant = tenantState.getTenantByKey(tenantKey);

    if (persistedTenant.isEmpty()) {
      rejectCommand(
          command,
          RejectionType.NOT_FOUND,
          "Expected to remove entity from tenant with key '%s', but no tenant with this key exists."
              .formatted(tenantKey));
      return;
    }

    final var authorizationRequest =
        new AuthorizationRequest(command, AuthorizationResourceType.TENANT, PermissionType.UPDATE)
            .addResourceId(persistedTenant.get().getTenantId());
    final var isAuthorized = authCheckBehavior.isAuthorized(authorizationRequest);
    if (isAuthorized.isLeft()) {
      rejectCommandWithUnauthorizedError(command, isAuthorized.getLeft());
      return;
    }

    if (!isEntityPresent(record.getEntityKey(), record.getEntityType())) {
      rejectCommand(
          command,
          RejectionType.NOT_FOUND,
          "Expected to remove entity with key '%s' from tenant with key '%s', but the entity does not exist."
              .formatted(record.getEntityKey(), tenantKey));
      return;
    }

    if (!tenantState.isEntityAssignedToTenant(record.getEntityKey(), tenantKey)) {
      rejectCommand(
          command,
          RejectionType.NOT_FOUND,
          ENTITY_NOT_ASSIGNED_ERROR_MESSAGE.formatted(record.getEntityKey(), tenantKey));
      return;
    }

    stateWriter.appendFollowUpEvent(tenantKey, TenantIntent.ENTITY_REMOVED, record);
    responseWriter.writeEventOnCommand(tenantKey, TenantIntent.ENTITY_REMOVED, record, command);
    distributeCommand(command);
  }

  @Override
  public void processDistributedCommand(final TypedRecord<TenantRecord> command) {
    final var record = command.getValue();
    tenantState
        .getEntityType(record.getTenantKey(), record.getEntityKey())
        .ifPresentOrElse(
            entityType ->
                stateWriter.appendFollowUpEvent(
                    command.getKey(), TenantIntent.ENTITY_REMOVED, record),
            () -> {
              final var message =
                  ENTITY_NOT_ASSIGNED_ERROR_MESSAGE.formatted(
                      record.getEntityKey(), record.getTenantKey());
              rejectionWriter.appendRejection(command, RejectionType.NOT_FOUND, message);
            });

    commandDistributionBehavior.acknowledgeCommand(command);
  }

  private boolean isEntityPresent(final long entityKey, final EntityType entityType) {
    return switch (entityType) {
      case USER -> userState.getUser(entityKey).isPresent();
      case MAPPING -> mappingState.get(entityKey).isPresent();
      case GROUP -> groupState.get(entityKey).isPresent();
      default -> false;
    };
  }

  private void rejectCommandWithUnauthorizedError(
      final TypedRecord<TenantRecord> command, final Rejection rejection) {
    rejectCommand(command, rejection.type(), rejection.reason());
  }

  private void rejectCommand(
      final TypedRecord<TenantRecord> command,
      final RejectionType type,
      final String errorMessage) {
    rejectionWriter.appendRejection(command, type, errorMessage);
    responseWriter.writeRejectionOnCommand(command, type, errorMessage);
  }

  private void distributeCommand(final TypedRecord<TenantRecord> command) {
    commandDistributionBehavior
        .withKey(keyGenerator.nextKey())
        .inQueue(DistributionQueue.IDENTITY.getQueueId())
        .distribute(command);
  }
}
