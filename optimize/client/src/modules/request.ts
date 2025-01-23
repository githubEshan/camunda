/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {ReactNode} from 'react';
import {getLanguage, t} from './translation/translation';

type Handler = {
  fct: (response: Response, payload: RequestPayload) => Promise<Response>;
  priority: number;
};

export type RequestPayload = {
  url: string;
  method: string;
  body?: unknown;
  query?: Record<string, unknown>;
  headers?: Record<string, unknown>;
};

export interface ErrorResponse extends Response {
  status: number;
  message: string;
}

const handlers: Handler[] = [];

export function put(
  url: string,
  body: unknown,
  options: Record<string, unknown> = {}
): Promise<Response> {
  return request({
    url,
    body,
    method: 'PUT',
    ...options,
  });
}

export function post(
  url: string,
  body?: unknown,
  options: Record<string, unknown> = {}
): Promise<Response> {
  return request({
    url,
    body,
    method: 'POST',
    ...options,
  });
}

export function get(
  url: string,
  query?: Record<string, unknown>,
  options: Record<string, unknown> = {}
): Promise<Response> {
  return request({
    url,
    query,
    method: 'GET',
    ...options,
  });
}

export function del(
  url: string,
  query?: Record<string, unknown>,
  options: Record<string, unknown> = {}
): Promise<Response> {
  return request({
    url,
    query,
    method: 'DELETE',
    ...options,
  });
}

export function addHandler(fct: Handler['fct'], priority = 0) {
  handlers.push({fct, priority});
  handlers.sort((a, b) => b.priority - a.priority);
}

export function removeHandler(fct: Handler['fct']) {
  const handlerToRemove = handlers.find((entry) => entry.fct === fct);
  if (handlerToRemove) {
    handlers.splice(handlers.indexOf(handlerToRemove), 1);
  }
}

export async function request(payload: RequestPayload): Promise<Response> {
  const {url, method, body, query, headers} = payload;
  const resourceUrl = query ? `${url}?${formatQuery(query)}` : url;

  let response = await fetch(resourceUrl, {
    method,
    body: processBody(body),
    headers: {
      'Content-Type': 'application/json',
      'X-Optimize-Client-Timezone': Intl.DateTimeFormat().resolvedOptions().timeZone,
      'X-Optimize-Client-Locale': getLanguage(),
      ...headers,
    },
    mode: 'cors',
    credentials: 'same-origin',
  });

  for (const handlerToCall of handlers) {
    if (handlerToCall) {
      response = await handlerToCall.fct(response, payload);
    }
  }

  if (response.status >= 200 && response.status < 300) {
    return response;
  } else {
    throw await parseError(response as ErrorResponse);
  }
}

export function formatQuery(query: Record<string, unknown>): string {
  return Object.keys(query)
    .reduce<string[]>((queryStr, key) => {
      const value = query[key] as string | string[];

      if (Array.isArray(value)) {
        const str = value.map((val) => `${key}=${val}`).join('&');
        if (!str) {
          return queryStr;
        }
        return queryStr.concat(str);
      }

      if (queryStr.length === 0) {
        return [`${key}=${encodeURIComponent(value)}`];
      }

      return queryStr.concat(`${key}=${encodeURIComponent(value)}`);
    }, [])
    .join('&');
}

function processBody(body: unknown): string {
  if (typeof body === 'string') {
    return body;
  }

  return JSON.stringify(body);
}

async function parseError(error: ErrorResponse): Promise<ErrorResponse | Record<string, unknown>> {
  let message: ReactNode = error.message || 'Unknown error';

  if (error.status === 413) {
    // This error is thrown by the nginx and it is a HTML response.
    // We need to handle it based on the status code.
    return {status: error.status, message: t('apiErrors.payloadTooLarge')};
  }

  if (typeof error.json !== 'function') {
    return {status: error.status, message};
  }

  try {
    const {errorCode, errorMessage, ...errorProps} = await error.json();
    if (errorMessage) {
      message = errorMessage;
    }

    if (errorCode) {
      try {
        message = t('apiErrors.' + errorCode, {...errorProps});
      } catch (e) {
        console.error('Tried to parse error message, but failed: ', error, e);
      }
    }

    return {status: error.status, message, ...errorProps};
  } catch (e) {
    // We should show an error, but cannot parse the error
    // e.g. the server did not return the expected error object
    console.error('Tried to parse error object, but failed', error, e);
  }

  return {status: error.status, message};
}
