/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.os.report.interpreter.plan.process;

import io.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import io.camunda.optimize.service.db.report.ExecutionContext;
import io.camunda.optimize.service.db.report.interpreter.plan.process.ProcessExecutionPlanInterpreter;
import io.camunda.optimize.service.db.report.plan.process.ProcessExecutionPlan;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;

public interface ProcessExecutionPlanInterpreterOS extends ProcessExecutionPlanInterpreter {
  BoolQuery.Builder baseQueryBuilder(
      final ExecutionContext<ProcessReportDataDto, ProcessExecutionPlan> context);
}
