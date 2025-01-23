/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.util.mapper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;

public class CustomOffsetDateTimeDeserializer extends JsonDeserializer<OffsetDateTime> {

  public static final String OFFSET_X_DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSX";

  private final DateTimeFormatter formatter;

  public CustomOffsetDateTimeDeserializer(final DateTimeFormatter formatter) {
    this.formatter = formatter;
  }

  @Override
  public OffsetDateTime deserialize(final JsonParser parser, final DeserializationContext context)
      throws IOException {
    try {
      final TemporalAccessor parsedTemporal =
          this.formatter.parseBest(
              // having LocalDateTime here as fallback in case the pattern is not including a time
              // zone
              parser.getText(), OffsetDateTime::from, LocalDateTime::from);
      if (parsedTemporal instanceof OffsetDateTime) {
        return (OffsetDateTime) parsedTemporal;
      } else {
        return ((LocalDateTime) parsedTemporal).atZone(ZoneId.systemDefault()).toOffsetDateTime();
      }
    } catch (final DateTimeParseException exception) {
      // If the offset is a 'Z', we can handle it using a backup `X` pattern rather than failing
      return ZonedDateTime.parse(
              parser.getText(),
              DateTimeFormatter.ofPattern(OFFSET_X_DATE_TIME_PATTERN)
                  .withZone(ZoneId.systemDefault()))
          .toOffsetDateTime();
    }
  }
}
