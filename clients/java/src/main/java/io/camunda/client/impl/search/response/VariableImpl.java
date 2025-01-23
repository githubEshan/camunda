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

import io.camunda.client.api.search.response.Variable;
import io.camunda.client.impl.util.ParseUtil;
import io.camunda.client.protocol.rest.VariableResult;

public class VariableImpl implements Variable {

  private final Long variableKey;
  private final String name;
  private final String value;
  private final String fullValue;
  private final Long scopeKey;
  private final Long processInstanceKey;
  private final String tenantId;
  private final Boolean isTruncated;

  public VariableImpl(final VariableResult item) {
    variableKey = ParseUtil.parseLongOrNull(item.getVariableKey());
    name = item.getName();
    value = item.getValue();
    fullValue = item.getFullValue();
    scopeKey = ParseUtil.parseLongOrNull(item.getScopeKey());
    processInstanceKey = ParseUtil.parseLongOrNull(item.getProcessInstanceKey());
    tenantId = item.getTenantId();
    isTruncated = item.getIsTruncated();
  }

  @Override
  public Long getVariableKey() {
    return variableKey;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getValue() {
    return value;
  }

  @Override
  public String getFullValue() {
    return fullValue;
  }

  @Override
  public Long getScopeKey() {
    return scopeKey;
  }

  @Override
  public Long getProcessInstanceKey() {
    return processInstanceKey;
  }

  @Override
  public String getTenantId() {
    return tenantId;
  }

  @Override
  public Boolean isTruncated() {
    return isTruncated;
  }
}
