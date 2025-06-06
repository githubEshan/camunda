/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.os.report.interpreter.distributedby.decision;

import io.camunda.optimize.dto.optimize.query.report.single.decision.DecisionReportDataDto;
import io.camunda.optimize.service.db.os.report.interpreter.RawResult;
import io.camunda.optimize.service.db.os.report.interpreter.distributedby.AbstractDistributedByInterpreterOS;
import io.camunda.optimize.service.db.os.report.interpreter.view.decision.DecisionViewInterpreterFacadeOS;
import io.camunda.optimize.service.db.report.ExecutionContext;
import io.camunda.optimize.service.db.report.plan.decision.DecisionExecutionPlan;
import io.camunda.optimize.service.db.report.result.CompositeCommandResult.DistributedByResult;
import io.camunda.optimize.service.db.report.result.CompositeCommandResult.ViewResult;
import io.camunda.optimize.service.util.configuration.condition.OpenSearchCondition;
import java.util.List;
import java.util.Map;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Component
@Conditional(OpenSearchCondition.class)
public class DecisionDistributedByNoneInterpreterOS
    extends AbstractDistributedByInterpreterOS<DecisionReportDataDto, DecisionExecutionPlan> {

  private final DecisionViewInterpreterFacadeOS viewInterpreter;

  public DecisionDistributedByNoneInterpreterOS(
      final DecisionViewInterpreterFacadeOS viewInterpreter) {
    this.viewInterpreter = viewInterpreter;
  }

  @Override
  public Map<String, Aggregation> createAggregations(
      final ExecutionContext<DecisionReportDataDto, DecisionExecutionPlan> context,
      final Query baseQuery) {
    return viewInterpreter.createAggregations(context);
  }

  @Override
  public List<DistributedByResult> retrieveResult(
      final SearchResponse<RawResult> response,
      final Map<String, Aggregate> aggregations,
      final ExecutionContext<DecisionReportDataDto, DecisionExecutionPlan> context) {
    final ViewResult viewResult = viewInterpreter.retrieveResult(response, aggregations, context);
    return List.of(DistributedByResult.createDistributedByNoneResult(viewResult));
  }

  @Override
  public List<DistributedByResult> createEmptyResult(
      final ExecutionContext<DecisionReportDataDto, DecisionExecutionPlan> context) {
    return List.of(
        DistributedByResult.createDistributedByNoneResult(
            viewInterpreter.createEmptyResult(context)));
  }

  public DecisionViewInterpreterFacadeOS getViewInterpreter() {
    return this.viewInterpreter;
  }
}
