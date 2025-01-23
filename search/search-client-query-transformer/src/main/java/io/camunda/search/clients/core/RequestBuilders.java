/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.search.clients.core;

import io.camunda.util.ObjectBuilder;
import java.util.function.Function;

public final class RequestBuilders {

  private RequestBuilders() {}

  public static SearchQueryRequest.Builder searchRequest() {
    return new SearchQueryRequest.Builder();
  }

  public static SearchQueryRequest searchRequest(
      final Function<SearchQueryRequest.Builder, ObjectBuilder<SearchQueryRequest>> fn) {
    return fn.apply(searchRequest()).build();
  }

  public static SearchGetRequest.Builder getRequest() {
    return new SearchGetRequest.Builder();
  }

  public static SearchGetRequest getRequest(
      final Function<SearchGetRequest.Builder, ObjectBuilder<SearchGetRequest>> fn) {
    return fn.apply(getRequest()).build();
  }

  public static <T> SearchIndexRequest.Builder<T> indexRequest() {
    return new SearchIndexRequest.Builder<T>();
  }

  public static <T> SearchIndexRequest<T> indexRequest(
      final Function<SearchIndexRequest.Builder<T>, ObjectBuilder<SearchIndexRequest<T>>> fn) {
    return fn.apply(indexRequest()).build();
  }

  public static SearchDeleteRequest.Builder deleteRequest() {
    return new SearchDeleteRequest.Builder();
  }

  public static SearchDeleteRequest deleteRequest(
      final Function<SearchDeleteRequest.Builder, ObjectBuilder<SearchDeleteRequest>> fn) {
    return fn.apply(deleteRequest()).build();
  }
}
