/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize.rest.export.report;

import static io.camunda.optimize.dto.optimize.rest.export.ExportEntityType.SINGLE_DECISION_REPORT;

import io.camunda.optimize.dto.optimize.query.report.single.decision.DecisionReportDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.decision.SingleDecisionReportDefinitionRequestDto;
import io.camunda.optimize.dto.optimize.rest.export.ExportEntityType;
import io.camunda.optimize.service.db.schema.index.report.SingleDecisionReportIndex;
import jakarta.validation.constraints.NotNull;

public class SingleDecisionReportDefinitionExportDto extends ReportDefinitionExportDto {

  @NotNull private DecisionReportDataDto data;

  public SingleDecisionReportDefinitionExportDto(
      final SingleDecisionReportDefinitionRequestDto reportDefinition) {
    super(
        reportDefinition.getId(),
        SINGLE_DECISION_REPORT,
        SingleDecisionReportIndex.VERSION,
        reportDefinition.getName(),
        reportDefinition.getDescription(),
        reportDefinition.getCollectionId());
    data = reportDefinition.getData();
  }

  public SingleDecisionReportDefinitionExportDto(@NotNull final DecisionReportDataDto data) {
    this.data = data;
  }

  public SingleDecisionReportDefinitionExportDto() {}

  @Override
  public ExportEntityType getExportEntityType() {
    return SINGLE_DECISION_REPORT;
  }

  public @NotNull DecisionReportDataDto getData() {
    return data;
  }

  public void setData(@NotNull final DecisionReportDataDto data) {
    this.data = data;
  }

  @Override
  protected boolean canEqual(final Object other) {
    return other instanceof SingleDecisionReportDefinitionExportDto;
  }

  @Override
  public int hashCode() {
    return org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode(this);
  }

  @Override
  public boolean equals(final Object o) {
    return org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals(this, o);
  }

  @Override
  public String toString() {
    return "SingleDecisionReportDefinitionExportDto(data=" + getData() + ")";
  }
}
