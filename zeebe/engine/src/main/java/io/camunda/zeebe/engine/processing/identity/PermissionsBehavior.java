/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.processing.identity;

import io.camunda.zeebe.engine.processing.Rejection;
import io.camunda.zeebe.engine.processing.identity.AuthorizationCheckBehavior.AuthorizationRequest;
import io.camunda.zeebe.engine.state.immutable.AuthorizationState;
import io.camunda.zeebe.engine.state.immutable.ProcessingState;
import io.camunda.zeebe.protocol.impl.record.value.authorization.AuthorizationRecord;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.value.AuthorizationRecordValue.PermissionValue;
import io.camunda.zeebe.protocol.record.value.AuthorizationResourceType;
import io.camunda.zeebe.protocol.record.value.PermissionType;
import io.camunda.zeebe.stream.api.records.TypedRecord;
import io.camunda.zeebe.util.Either;
import java.util.HashSet;
import java.util.stream.Collectors;

public class PermissionsBehavior {

  public static final String OWNER_NOT_FOUND_MESSAGE =
      "Expected to find owner with key: '%d', but none was found";
  public static final String PERMISSION_ALREADY_EXISTS_MESSAGE =
      "Expected to add '%s' permission for resource '%s' and resource identifiers '%s' for owner '%s', but this permission for resource identifiers '%s' already exist. Existing resource ids are: '%s'";
  public static final String PERMISSION_NOT_FOUND_MESSAGE =
      "Expected to remove '%s' permission for resource '%s' and resource identifiers '%s' for owner '%s', but this permission for resource identifiers '%s' is not found. Existing resource ids are: '%s'";

  private final AuthorizationState authorizationState;
  private final AuthorizationCheckBehavior authCheckBehavior;

  public PermissionsBehavior(
      final ProcessingState processingState, final AuthorizationCheckBehavior authCheckBehavior) {
    authorizationState = processingState.getAuthorizationState();
    this.authCheckBehavior = authCheckBehavior;
  }

  public Either<Rejection, AuthorizationRecord> isAuthorized(
      final TypedRecord<AuthorizationRecord> command) {
    final var authorizationRequest =
        new AuthorizationRequest(
            command, AuthorizationResourceType.AUTHORIZATION, PermissionType.UPDATE);
    return authCheckBehavior.isAuthorized(authorizationRequest).map(unused -> command.getValue());
  }

  public Either<Rejection, AuthorizationRecord> ownerExists(
      final AuthorizationRecord authorizationRecord) {
    final var ownerKey = authorizationRecord.getOwnerKey();

    return authorizationState
        .getOwnerType(ownerKey)
        .map(
            ownerType -> {
              authorizationRecord.setOwnerType(ownerType);
              return Either.<Rejection, AuthorizationRecord>right(authorizationRecord);
            })
        .orElseGet(
            () ->
                Either.left(
                    new Rejection(
                        RejectionType.NOT_FOUND, OWNER_NOT_FOUND_MESSAGE.formatted(ownerKey))));
  }

  public Either<Rejection, AuthorizationRecord> permissionAlreadyExists(
      final AuthorizationRecord record) {
    for (final PermissionValue permission : record.getPermissions()) {
      final var addedResourceIds = permission.getResourceIds();
      final var currentResourceIds =
          authCheckBehavior.getDirectAuthorizedResourceIdentifiers(
              record.getOwnerType(),
              record.getOwnerId(),
              record.getResourceType(),
              permission.getPermissionType());

      final var duplicates = new HashSet<>(currentResourceIds);
      duplicates.retainAll(addedResourceIds);
      if (!duplicates.isEmpty()) {
        return Either.left(
            new Rejection(
                RejectionType.ALREADY_EXISTS,
                PERMISSION_ALREADY_EXISTS_MESSAGE.formatted(
                    permission.getPermissionType(),
                    record.getResourceType(),
                    addedResourceIds,
                    record.getOwnerKey(),
                    duplicates,
                    currentResourceIds)));
      }
    }

    return Either.right(record);
  }

  public Either<Rejection, AuthorizationRecord> permissionDoesNotExist(
      final AuthorizationRecord record) {
    for (final PermissionValue permission : record.getPermissions()) {
      final var currentResourceIdentifiers =
          authCheckBehavior.getDirectAuthorizedResourceIdentifiers(
              record.getOwnerType(),
              record.getOwnerId(),
              record.getResourceType(),
              permission.getPermissionType());

      final var removedResourceIds = permission.getResourceIds();
      if (!currentResourceIdentifiers.containsAll(removedResourceIds)) {
        final var differences = new HashSet<>(removedResourceIds);
        differences.removeAll(currentResourceIdentifiers);

        return Either.left(
            new Rejection(
                RejectionType.NOT_FOUND,
                PERMISSION_NOT_FOUND_MESSAGE.formatted(
                    permission.getPermissionType(),
                    record.getResourceType(),
                    removedResourceIds,
                    record.getOwnerKey(),
                    differences,
                    currentResourceIdentifiers)));
      }
    }

    return Either.right(record);
  }

  public Either<Rejection, AuthorizationRecord> hasValidPermissionTypes(
      final AuthorizationRecord record) {
    final var resourceType = record.getResourceType();
    final var permissionTypes =
        record.getPermissions().stream()
            .map(PermissionValue::getPermissionType)
            .collect(Collectors.toList());

    if (resourceType.getSupportedPermissionTypes().containsAll(permissionTypes)) {
      return Either.right(record);
    }

    permissionTypes.removeAll(resourceType.getSupportedPermissionTypes());

    return Either.left(
        new Rejection(
            RejectionType.INVALID_ARGUMENT,
            "Expected to add permission types '%s' for resource type '%s', but these permissions are not supported. Supported permission types are: '%s'"
                .formatted(
                    permissionTypes, resourceType, resourceType.getSupportedPermissionTypes())));
  }
}
