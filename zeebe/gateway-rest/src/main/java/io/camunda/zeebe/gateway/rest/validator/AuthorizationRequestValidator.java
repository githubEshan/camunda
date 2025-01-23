/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.gateway.rest.validator;

import static io.camunda.zeebe.gateway.rest.validator.ErrorMessages.ERROR_MESSAGE_EMPTY_ATTRIBUTE;
import static io.camunda.zeebe.gateway.rest.validator.ErrorMessages.ERROR_MESSAGE_EMPTY_NESTED_ATTRIBUTE;
import static io.camunda.zeebe.gateway.rest.validator.RequestValidator.validate;

import io.camunda.zeebe.gateway.protocol.rest.AuthorizationCreateRequest;
import io.camunda.zeebe.gateway.protocol.rest.AuthorizationPatchRequest;
import io.camunda.zeebe.gateway.protocol.rest.PermissionDTO;
import io.camunda.zeebe.gateway.protocol.rest.PermissionTypeEnum;
import io.camunda.zeebe.gateway.protocol.rest.ResourceTypeEnum;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ProblemDetail;

public final class AuthorizationRequestValidator {

  public static final String PERMISSION_TYPE_NOT_ALLOWED =
      "Permission type '%s' is allowed for resource type '%s'";
  private static final Map<PermissionTypeEnum, ResourceTypeEnum>
      RESOURCE_SPECIFIC_PERMISSION_TYPES =
          Map.of(
              PermissionTypeEnum.DELETE_PROCESS, ResourceTypeEnum.RESOURCE,
              PermissionTypeEnum.DELETE_DRD, ResourceTypeEnum.RESOURCE,
              PermissionTypeEnum.DELETE_FORM, ResourceTypeEnum.RESOURCE);

  public static Optional<ProblemDetail> validateAuthorizationCreateRequest(
      final AuthorizationCreateRequest request) {
    return validate(
        violations -> {
          // owner validation
          if (request.getOwnerId() == null || request.getOwnerId().isEmpty()) {
            violations.add(ERROR_MESSAGE_EMPTY_ATTRIBUTE.formatted("ownerId"));
          }
          if (request.getOwnerType() == null) {
            violations.add(ERROR_MESSAGE_EMPTY_ATTRIBUTE.formatted("ownerType"));
          }

          // resource validation
          if (request.getResourceId() == null || request.getResourceId().isEmpty()) {
            violations.add(ERROR_MESSAGE_EMPTY_ATTRIBUTE.formatted("resourceId"));
          }
          if (request.getResourceType() == null) {
            violations.add(ERROR_MESSAGE_EMPTY_ATTRIBUTE.formatted("resourceType"));
          }

          // permissions validation
          if (request.getPermissions() == null || request.getPermissions().isEmpty()) {
            violations.add(ERROR_MESSAGE_EMPTY_ATTRIBUTE.formatted("permissions"));
          }
        });
  }

  public static Optional<ProblemDetail> validateAuthorizationAssignRequest(
      final AuthorizationPatchRequest authorizationPatchRequest) {
    return validate(
        violations -> {
          if (authorizationPatchRequest.getAction() == null) {
            violations.add(ERROR_MESSAGE_EMPTY_ATTRIBUTE.formatted("action"));
          }

          if (authorizationPatchRequest.getResourceType() == null) {
            violations.add(ERROR_MESSAGE_EMPTY_ATTRIBUTE.formatted("resourceType"));
          }

          if (authorizationPatchRequest.getPermissions() == null
              || authorizationPatchRequest.getPermissions().isEmpty()) {
            violations.add(ERROR_MESSAGE_EMPTY_ATTRIBUTE.formatted("permissions"));
          } else {
            authorizationPatchRequest
                .getPermissions()
                .forEach(
                    permission ->
                        validatePermission(
                            permission, violations, authorizationPatchRequest.getResourceType()));
          }
        });
  }

  private static void validatePermission(
      final PermissionDTO permission,
      final List<String> violations,
      final ResourceTypeEnum resourceType) {
    if (permission.getPermissionType() == null) {
      violations.add(ERROR_MESSAGE_EMPTY_ATTRIBUTE.formatted("permissionType"));
    } else if (permission.getResourceIds() == null || permission.getResourceIds().isEmpty()) {
      violations.add(
          ERROR_MESSAGE_EMPTY_NESTED_ATTRIBUTE.formatted(
              "resourceIds", permission.getPermissionType()));
    } else if (isPermissionTypeAllowed(permission, resourceType)) {
      violations.add(
          PERMISSION_TYPE_NOT_ALLOWED.formatted(
              permission.getPermissionType().name(), resourceType.name()));
    }
  }

  private static boolean isPermissionTypeAllowed(
      final PermissionDTO permission, final ResourceTypeEnum resourceType) {
    return RESOURCE_SPECIFIC_PERMISSION_TYPES.getOrDefault(
            permission.getPermissionType(), resourceType)
        != resourceType;
  }
}
