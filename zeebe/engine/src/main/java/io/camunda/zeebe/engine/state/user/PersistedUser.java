/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.state.user;

import io.camunda.zeebe.db.DbValue;
import io.camunda.zeebe.msgpack.UnpackedObject;
import io.camunda.zeebe.msgpack.property.ArrayProperty;
import io.camunda.zeebe.msgpack.property.ObjectProperty;
import io.camunda.zeebe.msgpack.value.LongValue;
import io.camunda.zeebe.msgpack.value.StringValue;
import io.camunda.zeebe.protocol.impl.record.value.user.UserRecord;
import io.camunda.zeebe.util.buffer.BufferUtil;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class PersistedUser extends UnpackedObject implements DbValue {

  private final ObjectProperty<UserRecord> userProp =
      new ObjectProperty<>("user", new UserRecord());
  private final ArrayProperty<LongValue> roleKeysProp =
      new ArrayProperty<>("roleKeys", LongValue::new);
  private final ArrayProperty<StringValue> tenantIdsProp =
      new ArrayProperty<>("tenantIds", StringValue::new);
  private final ArrayProperty<LongValue> groupKeysProp =
      new ArrayProperty<>("groupKeys", LongValue::new);

  public PersistedUser() {
    super(4);
    declareProperty(userProp)
        .declareProperty(roleKeysProp)
        .declareProperty(tenantIdsProp)
        .declareProperty(groupKeysProp);
  }

  public PersistedUser copy() {
    final var copy = new PersistedUser();
    copy.copyFrom(this);
    return copy;
  }

  public UserRecord getUser() {
    return userProp.getValue();
  }

  public void setUser(final UserRecord record) {
    userProp.getValue().copyFrom(record);
  }

  public long getUserKey() {
    return getUser().getUserKey();
  }

  public String getUsername() {
    return getUser().getUsername();
  }

  public String getName() {
    return getUser().getName();
  }

  public String getEmail() {
    return getUser().getEmail();
  }

  public String getPassword() {
    return getUser().getPassword();
  }

  public List<Long> getRoleKeysList() {
    return StreamSupport.stream(roleKeysProp.spliterator(), false)
        .map(LongValue::getValue)
        .collect(Collectors.toList());
  }

  public PersistedUser setRoleKeysList(final List<Long> roleKeys) {
    roleKeysProp.reset();
    roleKeys.forEach(roleKey -> roleKeysProp.add().setValue(roleKey));
    return this;
  }

  public PersistedUser addRoleKey(final long roleKey) {
    roleKeysProp.add().setValue(roleKey);
    return this;
  }

  public List<String> getTenantIdsList() {
    return StreamSupport.stream(tenantIdsProp.spliterator(), false)
        .map(StringValue::getValue)
        .map(BufferUtil::bufferAsString)
        .collect(Collectors.toList());
  }

  public PersistedUser setTenantIdsList(final List<String> tenantIds) {
    tenantIdsProp.reset();
    tenantIds.forEach(tenantId -> tenantIdsProp.add().wrap(BufferUtil.wrapString(tenantId)));
    return this;
  }

  public PersistedUser addTenantId(final String tenantId) {
    tenantIdsProp.add().wrap(BufferUtil.wrapString(tenantId));
    return this;
  }

  public List<Long> getGroupKeysList() {
    return StreamSupport.stream(groupKeysProp.spliterator(), false)
        .map(LongValue::getValue)
        .collect(Collectors.toList());
  }

  public PersistedUser setGroupKeysList(final List<Long> groupKeys) {
    groupKeysProp.reset();
    groupKeys.forEach(groupKey -> groupKeysProp.add().setValue(groupKey));
    return this;
  }

  public PersistedUser addGroupKey(final long groupKey) {
    groupKeysProp.add().setValue(groupKey);
    return this;
  }
}
