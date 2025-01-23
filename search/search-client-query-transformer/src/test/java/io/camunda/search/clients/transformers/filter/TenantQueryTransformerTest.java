/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.search.clients.transformers.filter;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.search.clients.query.SearchQuery;
import io.camunda.search.filter.FilterBuilders;
import java.util.List;
import org.junit.jupiter.api.Test;

public class TenantQueryTransformerTest extends AbstractTransformerTest {

  @Test
  public void shouldQueryByTenantKey() {
    // given
    final var filter = FilterBuilders.tenant((f) -> f.key(12345L));

    // when
    final var searchRequest = transformQuery(filter);

    // then
    assertThat(searchRequest)
        .isEqualTo(
            SearchQuery.of(
                q ->
                    q.bool(
                        b ->
                            b.must(
                                List.of(
                                    SearchQuery.of(
                                        q1 -> q.term(t -> t.field("join").value("tenant"))),
                                    SearchQuery.of(
                                        q1 -> q.term(t -> t.field("key").value(12345L))))))));
  }

  @Test
  public void shouldQueryByTenantId() {
    // given
    final var filter = FilterBuilders.tenant((f) -> f.tenantId("tenant1"));

    // when
    final var searchRequest = transformQuery(filter);

    // then
    assertThat(searchRequest)
        .isEqualTo(
            SearchQuery.of(
                q ->
                    q.bool(
                        b ->
                            b.must(
                                List.of(
                                    SearchQuery.of(
                                        q1 -> q.term(t -> t.field("join").value("tenant"))),
                                    SearchQuery.of(
                                        q1 ->
                                            q.term(t -> t.field("tenantId").value("tenant1"))))))));
  }

  @Test
  public void shouldQueryByTenantName() {
    // given
    final var filter = FilterBuilders.tenant((f) -> f.name("TestTenant"));

    // when
    final var searchRequest = transformQuery(filter);

    // then
    assertThat(searchRequest)
        .isEqualTo(
            SearchQuery.of(
                q ->
                    q.bool(
                        b ->
                            b.must(
                                List.of(
                                    SearchQuery.of(
                                        q1 -> q.term(t -> t.field("join").value("tenant"))),
                                    SearchQuery.of(
                                        q1 ->
                                            q.term(t -> t.field("name").value("TestTenant"))))))));
  }

  @Test
  public void shouldQueryByMultipleTenantFields() {
    // given
    final var filter =
        FilterBuilders.tenant((f) -> f.key(12345L).tenantId("tenant1").name("TestTenant"));

    // when
    final var searchRequest = transformQuery(filter);

    // then
    assertThat(searchRequest)
        .isEqualTo(
            SearchQuery.of(
                builder ->
                    builder.bool(
                        b ->
                            b.must(
                                List.of(
                                    SearchQuery.of(
                                        q -> q.term(t -> t.field("join").value("tenant"))),
                                    SearchQuery.of(q -> q.term(t -> t.field("key").value(12345L))),
                                    SearchQuery.of(
                                        q -> q.term(t -> t.field("tenantId").value("tenant1"))),
                                    SearchQuery.of(
                                        q -> q.term(t -> t.field("name").value("TestTenant"))))))));
  }
}
