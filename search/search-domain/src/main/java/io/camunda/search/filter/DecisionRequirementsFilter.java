/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.search.filter;

import static io.camunda.util.CollectionUtil.addValuesToList;
import static io.camunda.util.CollectionUtil.collectValuesAsList;

import io.camunda.util.ObjectBuilder;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record DecisionRequirementsFilter(
    List<Long> decisionRequirementsKeys,
    List<String> names,
    List<Integer> versions,
    List<String> decisionRequirementsIds,
    List<String> tenantIds,
    List<String> resourceNames)
    implements FilterBase {

  public static final class Builder implements ObjectBuilder<DecisionRequirementsFilter> {

    private List<Long> decisionRequirementsKeys;
    private List<String> names;
    private List<Integer> versions;
    private List<String> decisionRequirementsIds;
    private List<String> tenantIds;
    private List<String> resourceNames;

    public Builder decisionRequirementsKeys(final List<Long> values) {
      decisionRequirementsKeys = addValuesToList(decisionRequirementsKeys, values);
      return this;
    }

    public Builder decisionRequirementsKeys(final Long... values) {
      return decisionRequirementsKeys(collectValuesAsList(values));
    }

    public Builder names(final List<String> values) {
      names = addValuesToList(names, values);
      return this;
    }

    public Builder names(final String... values) {
      return names(collectValuesAsList(values));
    }

    public Builder versions(final List<Integer> values) {
      versions = addValuesToList(versions, values);
      return this;
    }

    public Builder versions(final Integer... values) {
      return versions(collectValuesAsList(values));
    }

    public Builder decisionRequirementsIds(final List<String> values) {
      decisionRequirementsIds = addValuesToList(decisionRequirementsIds, values);
      return this;
    }

    public Builder decisionRequirementsIds(final String... values) {
      return decisionRequirementsIds(collectValuesAsList(values));
    }

    public Builder tenantIds(final List<String> values) {
      tenantIds = addValuesToList(tenantIds, values);
      return this;
    }

    public Builder tenantIds(final String... values) {
      return tenantIds(collectValuesAsList(values));
    }

    public Builder resourceNames(final List<String> values) {
      resourceNames = addValuesToList(resourceNames, values);
      return this;
    }

    public Builder resourceNames(final String... values) {
      return resourceNames(collectValuesAsList(values));
    }

    @Override
    public DecisionRequirementsFilter build() {
      return new DecisionRequirementsFilter(
          Objects.requireNonNullElse(decisionRequirementsKeys, Collections.emptyList()),
          Objects.requireNonNullElse(names, Collections.emptyList()),
          Objects.requireNonNullElse(versions, Collections.emptyList()),
          Objects.requireNonNullElse(decisionRequirementsIds, Collections.emptyList()),
          Objects.requireNonNullElse(tenantIds, Collections.emptyList()),
          Objects.requireNonNullElse(resourceNames, Collections.emptyList()));
    }
  }
}
