/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.search.clients.transformers.sort;

import static io.camunda.webapps.schema.descriptors.usermanagement.index.AuthorizationIndex.ID;
import static io.camunda.webapps.schema.descriptors.usermanagement.index.AuthorizationIndex.OWNER_KEY;
import static io.camunda.webapps.schema.descriptors.usermanagement.index.AuthorizationIndex.OWNER_TYPE;
import static io.camunda.webapps.schema.descriptors.usermanagement.index.AuthorizationIndex.RESOURCE_TYPE;

public class AuthorizationFieldSortingTransformer implements FieldSortingTransformer {

  @Override
  public String apply(final String domainField) {
    return switch (domainField) {
      case "ownerKey" -> OWNER_KEY;
      case "ownerType" -> OWNER_TYPE;
      case "resourceType" -> RESOURCE_TYPE;
      default -> throw new IllegalArgumentException("Unknown field: " + domainField);
    };
  }

  @Override
  public String defaultSortField() {
    return ID;
  }
}
