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
package io.camunda.spring.client.jobhandling.parameter;

import static io.camunda.spring.client.annotation.AnnotationUtil.getVariableValue;
import static io.camunda.spring.client.annotation.AnnotationUtil.isCustomHeaders;
import static io.camunda.spring.client.annotation.AnnotationUtil.isVariable;
import static io.camunda.spring.client.annotation.AnnotationUtil.isVariablesAsType;

import io.camunda.client.api.JsonMapper;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import io.camunda.spring.client.bean.ParameterInfo;

public class DefaultParameterResolverStrategy implements ParameterResolverStrategy {
  protected final JsonMapper jsonMapper;

  public DefaultParameterResolverStrategy(final JsonMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
  }

  @Override
  public ParameterResolver createResolver(final ParameterInfo parameterInfo) {
    final Class<?> parameterType = parameterInfo.getParameterInfo().getType();
    if (JobClient.class.isAssignableFrom(parameterType)) {
      return new JobClientParameterResolver();
    } else if (ActivatedJob.class.isAssignableFrom(parameterType)) {
      return new ActivatedJobParameterResolver();
    } else if (isVariable(parameterInfo)) {
      // get() can be used savely here as isVariable() verifies that an annotation is present
      final String variableName = getVariableValue(parameterInfo).get().getName();
      return new VariableResolver(variableName, parameterType, jsonMapper);
    } else if (isVariablesAsType(parameterInfo)) {
      return new VariablesAsTypeResolver(parameterType);
    } else if (isCustomHeaders(parameterInfo)) {
      return new CustomHeadersResolver();
    }
    throw new IllegalStateException(
        "Could not create parameter resolver for parameter " + parameterInfo);
  }
}
