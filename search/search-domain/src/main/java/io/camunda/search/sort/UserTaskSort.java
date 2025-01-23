/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.search.sort;

import io.camunda.util.ObjectBuilder;
import java.util.List;
import java.util.function.Function;

public final record UserTaskSort(List<FieldSorting> orderings) implements SortOption {

  @Override
  public List<FieldSorting> getFieldSortings() {
    return orderings;
  }

  public static UserTaskSort of(final Function<Builder, ObjectBuilder<UserTaskSort>> fn) {
    return SortOptionBuilders.userTask(fn);
  }

  public static final class Builder extends AbstractBuilder<Builder>
      implements ObjectBuilder<UserTaskSort> {

    public Builder creationDate() {
      currentOrdering = new FieldSorting("creationDate", null);
      return this;
    }

    public Builder completionDate() {
      currentOrdering = new FieldSorting("completionDate", null);
      return this;
    }

    public Builder priority() {
      currentOrdering = new FieldSorting("priority", null);
      return this;
    }

    public Builder dueDate() {
      currentOrdering = new FieldSorting("dueDate", null);
      return this;
    }

    public Builder followUpDate() {
      currentOrdering = new FieldSorting("followUpDate", null);
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }

    @Override
    public UserTaskSort build() {
      return new UserTaskSort(orderings);
    }
  }
}
