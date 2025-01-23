/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.camunda.client.impl.search.response;

import io.camunda.client.api.search.response.DecisionDefinition;
import io.camunda.client.protocol.rest.DecisionDefinitionResult;
import java.util.Objects;

public final class DecisionDefinitionImpl implements DecisionDefinition {

  private final String dmnDecisionId;
  private final String dmnDecisionName;
  private final int version;
  private final long decisionKey;
  private final String dmnDecisionRequirementsId;
  private final long decisionRequirementsKey;
  private final String tenantId;

  public DecisionDefinitionImpl(final DecisionDefinitionResult item) {
    this(
        item.getDecisionDefinitionId(),
        item.getName(),
        item.getVersion(),
        Long.parseLong(item.getDecisionDefinitionKey()),
        item.getDecisionRequirementsId(),
        Long.parseLong(item.getDecisionRequirementsKey()),
        item.getTenantId());
  }

  public DecisionDefinitionImpl(
      final String dmnDecisionId,
      final String dmnDecisionName,
      final int version,
      final long decisionKey,
      final String dmnDecisionRequirementsId,
      final long decisionRequirementsKey,
      final String tenantId) {
    this.dmnDecisionId = dmnDecisionId;
    this.dmnDecisionName = dmnDecisionName;
    this.version = version;
    this.decisionKey = decisionKey;
    this.dmnDecisionRequirementsId = dmnDecisionRequirementsId;
    this.decisionRequirementsKey = decisionRequirementsKey;
    this.tenantId = tenantId;
  }

  @Override
  public String getDmnDecisionId() {
    return dmnDecisionId;
  }

  @Override
  public String getDmnDecisionName() {
    return dmnDecisionName;
  }

  @Override
  public int getVersion() {
    return version;
  }

  @Override
  public long getDecisionKey() {
    return decisionKey;
  }

  @Override
  public String getDmnDecisionRequirementsId() {
    return dmnDecisionRequirementsId;
  }

  @Override
  public long getDecisionRequirementsKey() {
    return decisionRequirementsKey;
  }

  @Override
  public String getTenantId() {
    return tenantId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        dmnDecisionId,
        dmnDecisionName,
        version,
        decisionKey,
        dmnDecisionRequirementsId,
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
    final DecisionDefinitionImpl decisionDefinition = (DecisionDefinitionImpl) o;
    return version == decisionDefinition.version
        && decisionKey == decisionDefinition.decisionKey
        && decisionRequirementsKey == decisionDefinition.decisionRequirementsKey
        && Objects.equals(dmnDecisionId, decisionDefinition.dmnDecisionId)
        && Objects.equals(dmnDecisionName, decisionDefinition.dmnDecisionName)
        && Objects.equals(dmnDecisionRequirementsId, decisionDefinition.dmnDecisionRequirementsId)
        && Objects.equals(tenantId, decisionDefinition.tenantId);
  }

  @Override
  public String toString() {
    return "DecisionDefinitionImpl{"
        + "dmnDecisionId='"
        + dmnDecisionId
        + '\''
        + ", dmnDecisionName='"
        + dmnDecisionName
        + '\''
        + ", version="
        + version
        + ", decisionKey="
        + decisionKey
        + ", dmnDecisionRequirementsId='"
        + dmnDecisionRequirementsId
        + '\''
        + ", decisionRequirementsKey="
        + decisionRequirementsKey
        + ", tenantId='"
        + tenantId
        + '\''
        + '}';
  }
}
