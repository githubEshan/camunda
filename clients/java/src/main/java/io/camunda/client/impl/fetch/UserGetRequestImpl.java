/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.camunda.client.impl.fetch;

import io.camunda.client.api.CamundaFuture;
import io.camunda.client.api.command.FinalCommandStep;
import io.camunda.client.api.fetch.UserGetRequest;
import io.camunda.client.api.search.response.User;
import io.camunda.client.impl.command.ArgumentUtil;
import io.camunda.client.impl.http.HttpCamundaFuture;
import io.camunda.client.impl.http.HttpClient;
import io.camunda.client.impl.search.response.SearchResponseMapper;
import io.camunda.client.protocol.rest.UserResult;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.apache.hc.client5.http.config.RequestConfig;

public class UserGetRequestImpl implements UserGetRequest {

  private final HttpClient httpClient;
  private final RequestConfig.Builder httpRequestConfig;
  private final String username;

  public UserGetRequestImpl(final HttpClient httpClient, final String username) {
    this.httpClient = httpClient;
    httpRequestConfig = httpClient.newRequestConfig();
    this.username = username;
  }

  @Override
  public FinalCommandStep<User> requestTimeout(final Duration requestTimeout) {
    httpRequestConfig.setResponseTimeout(requestTimeout.toMillis(), TimeUnit.MILLISECONDS);
    return this;
  }

  @Override
  public CamundaFuture<User> send() {
    ArgumentUtil.ensureNotNullNorEmpty("username", username);
    final HttpCamundaFuture<User> result = new HttpCamundaFuture<>();
    httpClient.get(
        String.format("/users/%s", username),
        httpRequestConfig.build(),
        UserResult.class,
        SearchResponseMapper::toUserResponse,
        result);
    return result;
  }
}
