/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.spring.client.configuration;

import static io.camunda.spring.client.configuration.CamundaClientConfigurationImpl.DEFAULT;
import static io.camunda.spring.client.configuration.PropertyUtil.getProperty;

import io.camunda.client.api.JsonMapper;
import io.camunda.client.api.worker.BackoffSupplier;
import io.camunda.client.impl.worker.ExponentialBackoffBuilderImpl;
import io.camunda.spring.client.annotation.customizer.JobWorkerValueCustomizer;
import io.camunda.spring.client.jobhandling.CamundaClientExecutorService;
import io.camunda.spring.client.jobhandling.CommandExceptionHandlingStrategy;
import io.camunda.spring.client.jobhandling.DefaultCommandExceptionHandlingStrategy;
import io.camunda.spring.client.jobhandling.JobWorkerManager;
import io.camunda.spring.client.jobhandling.parameter.DefaultParameterResolverStrategy;
import io.camunda.spring.client.jobhandling.parameter.ParameterResolverStrategy;
import io.camunda.spring.client.jobhandling.result.DefaultResultProcessorStrategy;
import io.camunda.spring.client.jobhandling.result.ResultProcessorStrategy;
import io.camunda.spring.client.metrics.MetricsRecorder;
import io.camunda.spring.client.properties.CamundaClientConfigurationProperties;
import io.camunda.spring.client.properties.CamundaClientProperties;
import io.camunda.spring.client.properties.PropertyBasedJobWorkerValueCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@ConditionalOnProperty(
    prefix = "zeebe.client",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@Import({AnnotationProcessorConfiguration.class, JsonMapperConfiguration.class})
@EnableConfigurationProperties({
  CamundaClientConfigurationProperties.class,
  CamundaClientProperties.class
})
public class CamundaClientAllAutoConfiguration {

  private final CamundaClientConfigurationProperties configurationProperties;
  private final CamundaClientProperties camundaClientProperties;

  public CamundaClientAllAutoConfiguration(
      final CamundaClientConfigurationProperties configurationProperties,
      final CamundaClientProperties camundaClientProperties) {
    this.configurationProperties = configurationProperties;
    this.camundaClientProperties = camundaClientProperties;
  }

  @Bean
  @ConditionalOnMissingBean
  public CamundaClientExecutorService zeebeClientExecutorService() {
    return CamundaClientExecutorService.createDefault(
        getProperty(
            "NumJobWorkerExecutionThreads",
            null,
            DEFAULT.getNumJobWorkerExecutionThreads(),
            camundaClientProperties::getExecutionThreads,
            () -> camundaClientProperties.getZeebe().getExecutionThreads(),
            configurationProperties::getNumJobWorkerExecutionThreads));
  }

  @Bean
  @ConditionalOnMissingBean
  public CommandExceptionHandlingStrategy commandExceptionHandlingStrategy(
      final CamundaClientExecutorService scheduledExecutorService) {
    return new DefaultCommandExceptionHandlingStrategy(
        backoffSupplier(), scheduledExecutorService.get());
  }

  @Bean
  @ConditionalOnMissingBean
  public ParameterResolverStrategy parameterResolverStrategy(final JsonMapper jsonMapper) {
    return new DefaultParameterResolverStrategy(jsonMapper);
  }

  @Bean
  @ConditionalOnMissingBean
  public ResultProcessorStrategy resultProcessorStrategy() {
    return new DefaultResultProcessorStrategy();
  }

  @Bean
  public JobWorkerManager jobWorkerManager(
      final CommandExceptionHandlingStrategy commandExceptionHandlingStrategy,
      final MetricsRecorder metricsRecorder,
      final ParameterResolverStrategy parameterResolverStrategy,
      final ResultProcessorStrategy resultProcessorStrategy) {
    return new JobWorkerManager(
        commandExceptionHandlingStrategy,
        metricsRecorder,
        parameterResolverStrategy,
        resultProcessorStrategy);
  }

  @Bean
  public BackoffSupplier backoffSupplier() {
    return new ExponentialBackoffBuilderImpl()
        .maxDelay(1000L)
        .minDelay(50L)
        .backoffFactor(1.5)
        .jitterFactor(0.2)
        .build();
  }

  @Bean("propertyBasedJobWorkerValueCustomizer")
  @ConditionalOnMissingBean(name = "propertyBasedJobWorkerValueCustomizer")
  public JobWorkerValueCustomizer propertyBasedJobWorkerValueCustomizer() {
    return new PropertyBasedJobWorkerValueCustomizer(
        configurationProperties, camundaClientProperties);
  }
}
