/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.webapps.schema.entities.usermanagement;

import io.camunda.webapps.schema.entities.AbstractExporterEntity;

public class RoleEntity extends AbstractExporterEntity<RoleEntity> {

  private Long key;
  private String name;
  private Long memberKey;
  private EntityJoinRelation join;

  public Long getKey() {
    return key;
  }

  public RoleEntity setKey(final Long key) {
    this.key = key;
    return this;
  }

  public String getName() {
    return name;
  }

  public RoleEntity setName(final String name) {
    this.name = name;
    return this;
  }

  public Long getMemberKey() {
    return memberKey;
  }

  public RoleEntity setMemberKey(final Long memberKey) {
    this.memberKey = memberKey;
    return this;
  }

  public EntityJoinRelation getJoin() {
    return join;
  }

  public RoleEntity setJoin(final EntityJoinRelation join) {
    this.join = join;
    return this;
  }

  public static String getChildKey(final long roleKey, final long memberKey) {
    return "%d-%d".formatted(roleKey, memberKey);
  }
}
