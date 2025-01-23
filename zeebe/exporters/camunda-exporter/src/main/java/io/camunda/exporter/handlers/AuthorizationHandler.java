/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.exporter.handlers;

import io.camunda.exporter.exceptions.PersistenceException;
import io.camunda.exporter.store.BatchRequest;
import io.camunda.security.entity.Permission;
import io.camunda.webapps.schema.entities.usermanagement.AuthorizationEntity;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.value.AuthorizationRecordValue;
import io.camunda.zeebe.protocol.record.value.AuthorizationRecordValue.PermissionValue;
import java.util.List;
import java.util.stream.Collectors;

public class AuthorizationHandler
    implements ExportHandler<AuthorizationEntity, AuthorizationRecordValue> {
  private final String indexName;

  public AuthorizationHandler(final String indexName) {
    this.indexName = indexName;
  }

  @Override
  public ValueType getHandledValueType() {
    return ValueType.AUTHORIZATION;
  }

  @Override
  public Class<AuthorizationEntity> getEntityType() {
    return AuthorizationEntity.class;
  }

  @Override
  public boolean handlesRecord(final Record<AuthorizationRecordValue> record) {
    return getHandledValueType().equals(record.getValueType());
  }

  @Override
  public List<String> generateIds(final Record<AuthorizationRecordValue> record) {
    return List.of(String.valueOf(record.getKey()));
  }

  @Override
  public AuthorizationEntity createNewEntity(final String id) {
    return new AuthorizationEntity().setId(id);
  }

  @Override
  public void updateEntity(
      final Record<AuthorizationRecordValue> record, final AuthorizationEntity entity) {
    final AuthorizationRecordValue value = record.getValue();
    entity
        .setOwnerKey(value.getOwnerKey())
        .setOwnerType(value.getOwnerType().name())
        .setResourceType(value.getResourceType().name())
        .setPermissions(getPermissions(value.getPermissions()));
  }

  @Override
  public void flush(final AuthorizationEntity entity, final BatchRequest batchRequest)
      throws PersistenceException {
    batchRequest.add(indexName, entity);
  }

  @Override
  public String getIndexName() {
    return indexName;
  }

  private List<Permission> getPermissions(final List<PermissionValue> permissionValues) {
    return permissionValues.stream()
        .map(
            permissionValue ->
                new Permission(
                    permissionValue.getPermissionType(), permissionValue.getResourceIds()))
        .collect(Collectors.toList());
  }
}
