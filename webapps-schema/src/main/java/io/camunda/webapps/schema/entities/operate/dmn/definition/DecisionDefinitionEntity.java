/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.webapps.schema.entities.operate.dmn.definition;

import io.camunda.webapps.schema.entities.operate.OperateZeebeEntity;
import java.util.Objects;

public class DecisionDefinitionEntity extends OperateZeebeEntity<DecisionDefinitionEntity> {

  private String decisionId;
  private String name;
  private int version;
  private String decisionRequirementsId;
  private long decisionRequirementsKey;
  private String tenantId = DEFAULT_TENANT_ID;

  public String getDecisionId() {
    return decisionId;
  }

  public DecisionDefinitionEntity setDecisionId(final String decisionId) {
    this.decisionId = decisionId;
    return this;
  }

  public String getName() {
    return name;
  }

  public DecisionDefinitionEntity setName(final String name) {
    this.name = name;
    return this;
  }

  public int getVersion() {
    return version;
  }

  public DecisionDefinitionEntity setVersion(final int version) {
    this.version = version;
    return this;
  }

  public String getDecisionRequirementsId() {
    return decisionRequirementsId;
  }

  public DecisionDefinitionEntity setDecisionRequirementsId(final String decisionRequirementsId) {
    this.decisionRequirementsId = decisionRequirementsId;
    return this;
  }

  public long getDecisionRequirementsKey() {
    return decisionRequirementsKey;
  }

  public DecisionDefinitionEntity setDecisionRequirementsKey(final long decisionRequirementsKey) {
    this.decisionRequirementsKey = decisionRequirementsKey;
    return this;
  }

  public String getTenantId() {
    return tenantId;
  }

  public DecisionDefinitionEntity setTenantId(final String tenantId) {
    this.tenantId = tenantId;
    return this;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        super.hashCode(),
        decisionId,
        name,
        version,
        decisionRequirementsId,
        decisionRequirementsKey,
        tenantId);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    final DecisionDefinitionEntity that = (DecisionDefinitionEntity) o;
    return version == that.version
        && decisionRequirementsKey == that.decisionRequirementsKey
        && Objects.equals(decisionId, that.decisionId)
        && Objects.equals(name, that.name)
        && Objects.equals(decisionRequirementsId, that.decisionRequirementsId)
        && Objects.equals(tenantId, that.tenantId);
  }
}
