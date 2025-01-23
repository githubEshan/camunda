/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.report.interpreter.util;

import io.camunda.optimize.dto.optimize.query.report.single.group.AggregateByDateUnit;
import java.time.temporal.ChronoUnit;

public final class AggregateByDateUnitMapper {

  private static final String UNSUPPORTED_UNIT_STRING = "Unsupported unit: ";

  private AggregateByDateUnitMapper() {}

  public static ChronoUnit mapToChronoUnit(final AggregateByDateUnit unit) {
    switch (unit) {
      case YEAR:
        return ChronoUnit.YEARS;
      case MONTH:
        return ChronoUnit.MONTHS;
      case WEEK:
        return ChronoUnit.WEEKS;
      case DAY:
        return ChronoUnit.DAYS;
      case HOUR:
        return ChronoUnit.HOURS;
      case MINUTE:
        return ChronoUnit.MINUTES;
      default:
      case AUTOMATIC:
        throw new IllegalArgumentException(UNSUPPORTED_UNIT_STRING + unit);
    }
  }

  public static AggregateByDateUnit mapToAggregateByDateUnit(final ChronoUnit unit) {
    switch (unit) {
      case YEARS:
        return AggregateByDateUnit.YEAR;
      case MONTHS:
        return AggregateByDateUnit.MONTH;
      case WEEKS:
        return AggregateByDateUnit.WEEK;
      case DAYS:
        return AggregateByDateUnit.DAY;
      case HOURS:
        return AggregateByDateUnit.HOUR;
      case MINUTES:
        return AggregateByDateUnit.MINUTE;
      default:
        throw new IllegalArgumentException(UNSUPPORTED_UNIT_STRING + unit);
    }
  }
}
