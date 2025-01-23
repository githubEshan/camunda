/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.db.rdbms.write.domain;

import io.camunda.util.ObjectBuilder;
import java.util.List;
import java.util.function.Function;

public class AuthorizationDbModel implements DbModel<AuthorizationDbModel> {

  private Long ownerKey;
  private String ownerType;
  private String resourceType;
  private List<AuthorizationPermissionDbModel> permissions;

  public Long ownerKey() {
    return ownerKey;
  }

  public void ownerKey(final Long ownerKey) {
    this.ownerKey = ownerKey;
  }

  public String ownerType() {
    return ownerType;
  }

  public void ownerType(final String ownerType) {
    this.ownerType = ownerType;
  }

  public String resourceType() {
    return resourceType;
  }

  public void resourceType(final String resourceType) {
    this.resourceType = resourceType;
  }

  public List<AuthorizationPermissionDbModel> permissions() {
    return permissions;
  }

  public void permissions(final List<AuthorizationPermissionDbModel> permissions) {
    this.permissions = permissions;
  }

  @Override
  public AuthorizationDbModel copy(
      final Function<ObjectBuilder<AuthorizationDbModel>, ObjectBuilder<AuthorizationDbModel>>
          copyFunction) {
    return copyFunction
        .apply(
            new Builder()
                .ownerKey(ownerKey)
                .ownerType(ownerType)
                .resourceType(resourceType)
                .permissions(permissions))
        .build();
  }

  public static class Builder implements ObjectBuilder<AuthorizationDbModel> {

    private Long ownerKey;
    private String ownerType;
    private String resourceType;
    private List<AuthorizationPermissionDbModel> permissions;

    public Builder() {}

    public Builder ownerKey(final Long ownerKey) {
      this.ownerKey = ownerKey;
      return this;
    }

    public Builder ownerType(final String ownerType) {
      this.ownerType = ownerType;
      return this;
    }

    public Builder resourceType(final String resourceType) {
      this.resourceType = resourceType;
      return this;
    }

    public Builder permissions(final List<AuthorizationPermissionDbModel> permissions) {
      this.permissions = permissions;
      return this;
    }

    @Override
    public AuthorizationDbModel build() {
      final AuthorizationDbModel model = new AuthorizationDbModel();
      model.ownerKey(ownerKey);
      model.ownerType(ownerType);
      model.resourceType(resourceType);
      model.permissions(permissions);
      return model;
    }
  }
}
