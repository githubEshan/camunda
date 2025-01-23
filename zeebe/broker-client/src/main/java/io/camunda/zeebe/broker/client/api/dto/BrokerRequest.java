/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.broker.client.api.dto;

import io.camunda.zeebe.broker.client.api.RequestDispatchStrategy;
import io.camunda.zeebe.broker.client.api.UnsupportedBrokerResponseException;
import io.camunda.zeebe.protocol.impl.encoding.ErrorResponse;
import io.camunda.zeebe.protocol.record.ErrorResponseDecoder;
import io.camunda.zeebe.protocol.record.ErrorResponseEncoder;
import io.camunda.zeebe.protocol.record.MessageHeaderDecoder;
import io.camunda.zeebe.transport.ClientRequest;
import io.camunda.zeebe.util.buffer.BufferUtil;
import io.camunda.zeebe.util.buffer.BufferWriter;
import java.util.Map;
import java.util.Optional;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BrokerRequest<T> implements ClientRequest {
  private static final Logger LOGGER = LoggerFactory.getLogger(BrokerRequest.class);

  protected final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
  protected final ErrorResponse errorResponse = new ErrorResponse();

  protected final int schemaId;
  protected final int templateId;

  public BrokerRequest(final int schemaId, final int templateId) {
    this.schemaId = schemaId;
    this.templateId = templateId;
  }

  public Optional<Integer> getBrokerId() {
    return Optional.empty();
  }

  public abstract void setPartitionId(int partitionId);

  public void setAuthorization(final Map<String, Object> claims) {
    // Unsupported by default
  }

  public abstract boolean addressesSpecificPartition();

  public abstract boolean requiresPartitionId();

  // public so we can do assertions in tests
  public abstract BufferWriter getRequestWriter();

  public void serializeValue() {
    final BufferWriter valueWriter = getRequestWriter();
    if (valueWriter != null) {
      final int valueLength = valueWriter.getLength();
      final MutableDirectBuffer buffer = new UnsafeBuffer(new byte[valueLength]);
      valueWriter.write(buffer, 0);
      setSerializedValue(buffer);
    }
  }

  protected abstract void setSerializedValue(DirectBuffer buffer);

  protected abstract void wrapResponse(DirectBuffer buffer);

  protected abstract BrokerResponse<T> readResponse();

  protected abstract T toResponseDto(DirectBuffer buffer);

  public abstract String getType();

  public Optional<RequestDispatchStrategy> requestDispatchStrategy() {
    return Optional.empty();
  }

  public BrokerResponse<T> getResponse(final DirectBuffer responseBuffer) {
    try {
      if (isValidResponse(responseBuffer)) {
        wrapResponse(responseBuffer);
        return readResponse();
      } else if (isErrorResponse(responseBuffer)) {
        wrapErrorResponse(responseBuffer);
        final var error = new BrokerError(errorResponse);
        return new BrokerErrorResponse<>(error);
      } else {
        throw new UnsupportedBrokerResponseException(
            headerDecoder.schemaId(), headerDecoder.templateId(), schemaId, templateId);
      }
    } catch (final Exception e) {
      // Log response buffer for debugging purpose
      LOGGER.error(
          "Failed to read response: {}{}{}",
          e.getMessage(),
          System.lineSeparator(),
          BufferUtil.bufferAsHexString(responseBuffer));
      throw e;
    }
  }

  protected void wrapResponseHeader(final DirectBuffer buffer) {
    headerDecoder.wrap(buffer, 0);
  }

  protected boolean isErrorResponse(final DirectBuffer buffer) {
    wrapResponseHeader(buffer);

    return headerDecoder.schemaId() == ErrorResponseEncoder.SCHEMA_ID
        && headerDecoder.templateId() == ErrorResponseDecoder.TEMPLATE_ID;
  }

  protected void wrapErrorResponse(final DirectBuffer buffer) {
    errorResponse.wrap(buffer, 0, buffer.capacity());
  }

  protected boolean isValidResponse(final DirectBuffer buffer) {
    wrapResponseHeader(buffer);

    return headerDecoder.schemaId() == schemaId && headerDecoder.templateId() == templateId;
  }
}
