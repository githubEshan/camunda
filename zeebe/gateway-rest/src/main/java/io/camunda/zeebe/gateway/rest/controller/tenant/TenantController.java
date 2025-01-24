/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.gateway.rest.controller.tenant;

import static io.camunda.zeebe.gateway.rest.RestErrorMapper.mapErrorToResponse;

import io.camunda.search.query.TenantQuery;
import io.camunda.service.TenantServices;
import io.camunda.service.TenantServices.TenantDTO;
import io.camunda.zeebe.gateway.protocol.rest.TenantCreateRequest;
import io.camunda.zeebe.gateway.protocol.rest.TenantItem;
import io.camunda.zeebe.gateway.protocol.rest.TenantSearchQueryRequest;
import io.camunda.zeebe.gateway.protocol.rest.TenantSearchQueryResponse;
import io.camunda.zeebe.gateway.protocol.rest.TenantUpdateRequest;
import io.camunda.zeebe.gateway.rest.RequestMapper;
import io.camunda.zeebe.gateway.rest.ResponseMapper;
import io.camunda.zeebe.gateway.rest.RestErrorMapper;
import io.camunda.zeebe.gateway.rest.SearchQueryRequestMapper;
import io.camunda.zeebe.gateway.rest.SearchQueryResponseMapper;
import io.camunda.zeebe.gateway.rest.annotation.CamundaDeleteMapping;
import io.camunda.zeebe.gateway.rest.annotation.CamundaGetMapping;
import io.camunda.zeebe.gateway.rest.annotation.CamundaPatchMapping;
import io.camunda.zeebe.gateway.rest.annotation.CamundaPostMapping;
import io.camunda.zeebe.gateway.rest.annotation.CamundaPutMapping;
import io.camunda.zeebe.gateway.rest.controller.CamundaRestController;
import io.camunda.zeebe.protocol.record.value.EntityType;
import java.util.concurrent.CompletableFuture;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CamundaRestController
@RequestMapping("/v2/tenants")
public class TenantController {
  private final TenantServices tenantServices;

  public TenantController(final TenantServices tenantServices) {
    this.tenantServices = tenantServices;
  }

  @CamundaPostMapping
  public CompletableFuture<ResponseEntity<Object>> createTenant(
      @RequestBody final TenantCreateRequest createTenantRequest) {
    return RequestMapper.toTenantCreateDto(createTenantRequest)
        .fold(RestErrorMapper::mapProblemToCompletedResponse, this::createTenant);
  }

  @CamundaGetMapping(path = "/{tenantId}")
  public ResponseEntity<TenantItem> getTenant(@PathVariable final String tenantId) {
    try {
      return ResponseEntity.ok()
          .body(
              SearchQueryResponseMapper.toTenant(
                  tenantServices
                      .withAuthentication(RequestMapper.getAuthentication())
                      .getById(tenantId)));
    } catch (final Exception exception) {
      return RestErrorMapper.mapErrorToResponse(exception);
    }
  }

  @CamundaPostMapping(path = "/search")
  public ResponseEntity<TenantSearchQueryResponse> searchTenants(
      @RequestBody(required = false) final TenantSearchQueryRequest query) {
    return SearchQueryRequestMapper.toTenantQuery(query)
        .fold(RestErrorMapper::mapProblemToResponse, this::search);
  }

  @CamundaPatchMapping(path = "/{tenantId}")
  public CompletableFuture<ResponseEntity<Object>> updateTenant(
      @PathVariable final String tenantId,
      @RequestBody final TenantUpdateRequest tenantUpdateRequest) {
    return RequestMapper.toTenantUpdateDto(tenantId, tenantUpdateRequest)
        .fold(RestErrorMapper::mapProblemToCompletedResponse, this::updateTenant);
  }

  @CamundaPutMapping(path = "/{tenantId}/users/{username}")
  public CompletableFuture<ResponseEntity<Object>> assignUsersToTenant(
      @PathVariable final String tenantId, @PathVariable final String username) {
    return RequestMapper.executeServiceMethodWithNoContentResult(
        () ->
            tenantServices
                .withAuthentication(RequestMapper.getAuthentication())
                .addMember(tenantId, EntityType.USER, username));
  }

  @CamundaDeleteMapping(path = "/{tenantId}")
  public CompletableFuture<ResponseEntity<Object>> deleteTenant(
      @PathVariable final String tenantId) {
    return RequestMapper.executeServiceMethodWithNoContentResult(
        () ->
            tenantServices
                .withAuthentication(RequestMapper.getAuthentication())
                .deleteTenant(tenantId));
  }

  @CamundaPutMapping(path = "/{tenantKey}/mapping-rules/{mappingKey}")
  public CompletableFuture<ResponseEntity<Object>> assignMappingToTenant(
      @PathVariable final long tenantKey, @PathVariable final long mappingKey) {
    return RequestMapper.executeServiceMethodWithNoContentResult(
        () ->
            tenantServices
                .withAuthentication(RequestMapper.getAuthentication())
                .addMember(tenantKey, EntityType.MAPPING, mappingKey));
  }

  @CamundaPutMapping(path = "/{tenantKey}/groups/{groupKey}")
  public CompletableFuture<ResponseEntity<Object>> assignGroupToTenant(
      @PathVariable final long tenantKey, @PathVariable final long groupKey) {
    return RequestMapper.executeServiceMethodWithNoContentResult(
        () ->
            tenantServices
                .withAuthentication(RequestMapper.getAuthentication())
                .addMember(tenantKey, EntityType.GROUP, groupKey));
  }

  @CamundaDeleteMapping(path = "/{tenantKey}/users/{userKey}")
  public CompletableFuture<ResponseEntity<Object>> removeUserFromTenant(
      @PathVariable final long tenantKey, @PathVariable final long userKey) {
    return RequestMapper.executeServiceMethodWithNoContentResult(
        () ->
            tenantServices
                .withAuthentication(RequestMapper.getAuthentication())
                .removeMember(tenantKey, EntityType.USER, userKey));
  }

  @CamundaDeleteMapping(path = "/{tenantKey}/mapping-rules/{mappingKey}")
  public CompletableFuture<ResponseEntity<Object>> removeMappingFromTenant(
      @PathVariable final long tenantKey, @PathVariable final long mappingKey) {
    return RequestMapper.executeServiceMethodWithNoContentResult(
        () ->
            tenantServices
                .withAuthentication(RequestMapper.getAuthentication())
                .removeMember(tenantKey, EntityType.MAPPING, mappingKey));
  }

  @CamundaDeleteMapping(path = "/{tenantKey}/groups/{groupKey}")
  public CompletableFuture<ResponseEntity<Object>> removeGroupFromTenant(
      @PathVariable final long tenantKey, @PathVariable final long groupKey) {
    return RequestMapper.executeServiceMethodWithNoContentResult(
        () ->
            tenantServices
                .withAuthentication(RequestMapper.getAuthentication())
                .removeMember(tenantKey, EntityType.GROUP, groupKey));
  }

  private CompletableFuture<ResponseEntity<Object>> createTenant(final TenantDTO tenantDTO) {
    return RequestMapper.executeServiceMethod(
        () ->
            tenantServices
                .withAuthentication(RequestMapper.getAuthentication())
                .createTenant(tenantDTO),
        ResponseMapper::toTenantCreateResponse);
  }

  private ResponseEntity<TenantSearchQueryResponse> search(final TenantQuery query) {
    try {
      final var result =
          tenantServices.withAuthentication(RequestMapper.getAuthentication()).search(query);
      return ResponseEntity.ok(SearchQueryResponseMapper.toTenantSearchQueryResponse(result));
    } catch (final Exception e) {
      return mapErrorToResponse(e);
    }
  }

  private CompletableFuture<ResponseEntity<Object>> updateTenant(final TenantDTO tenantDTO) {
    return RequestMapper.executeServiceMethod(
        () ->
            tenantServices
                .withAuthentication(RequestMapper.getAuthentication())
                .updateTenant(tenantDTO),
        ResponseMapper::toTenantUpdateResponse);
  }
}
