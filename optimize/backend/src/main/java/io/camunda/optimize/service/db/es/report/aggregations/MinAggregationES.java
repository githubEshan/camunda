/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.report.aggregations;

import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation.Builder.ContainerBuilder;
import co.elastic.clients.elasticsearch._types.aggregations.MinAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.MinAggregation.Builder;
import co.elastic.clients.util.Pair;
import io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationDto;
import io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationType;
import io.camunda.optimize.service.db.report.interpreter.util.AggregationResultMappingUtil;
import java.util.Map;

public class MinAggregationES extends AggregationStrategyES<Builder> {
  private static final String MIN_AGGREGATION = "minAggregation";

  @Override
  public Double getValueForAggregation(
      final String customIdentifier, final Map<String, Aggregate> aggs) {
    final MinAggregate aggregate =
        aggs.get(createAggregationName(customIdentifier, MIN_AGGREGATION)).min();
    return AggregationResultMappingUtil.mapToDoubleOrNull(aggregate.value());
  }

  @Override
  public Pair<String, ContainerBuilder> createAggregationBuilderForAggregation(
      final String customIdentifier, final Script script, final String... field) {
    final Aggregation.Builder builder = new Aggregation.Builder();
    return Pair.of(
        createAggregationName(customIdentifier, MIN_AGGREGATION),
        builder.min(
            a -> {
              a.script(script);
              if (field != null && field.length != 0) {
                a.field(field[0]);
              }
              return a;
            }));
  }

  @Override
  public AggregationDto getAggregationType() {
    return new AggregationDto(AggregationType.MIN);
  }
}
