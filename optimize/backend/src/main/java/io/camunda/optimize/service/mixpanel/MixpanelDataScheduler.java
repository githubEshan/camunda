/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.mixpanel;

import io.camunda.optimize.service.AbstractScheduledService;
import io.camunda.optimize.service.exceptions.OptimizeRuntimeException;
import io.camunda.optimize.service.util.configuration.ConfigurationReloadable;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import io.camunda.optimize.service.util.configuration.TelemetryConfiguration;
import io.camunda.optimize.service.util.configuration.condition.CCSaaSCondition;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Component;

@Component
@Conditional(CCSaaSCondition.class)
public class MixpanelDataScheduler extends AbstractScheduledService
    implements ConfigurationReloadable {

  private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(MixpanelDataScheduler.class);
  private final ConfigurationService configurationService;
  private final MixpanelReportingService mixpanelReportingService;

  public MixpanelDataScheduler(
      final ConfigurationService configurationService,
      final MixpanelReportingService mixpanelReportingService) {
    this.configurationService = configurationService;
    this.mixpanelReportingService = mixpanelReportingService;
  }

  @PostConstruct
  public void init() {
    LOG.info("Initializing MixpanelDataScheduler");
    getTelemetryConfiguration().validate();
    startMixpanelTelemetryScheduling();
  }

  @Override
  protected void run() {
    LOG.info("Checking whether Mixpanel telemetry data can be sent.");
    if (configurationService.getTelemetryConfiguration().isInitializeTelemetry()) {
      try {
        mixpanelReportingService.sendHeartbeatData();
        LOG.info("Mixpanel telemetry data was sent.");
      } catch (final OptimizeRuntimeException e) {
        LOG.error("Failed to send Mixpanel telemetry.", e);
      }
    } else {
      LOG.info("Mixpanel telemetry disabled.");
    }
  }

  @Override
  protected Trigger createScheduleTrigger() {
    return new PeriodicTrigger(
        Duration.ofHours(getTelemetryConfiguration().getReportingIntervalInHours()));
  }

  @Override
  public void reloadConfiguration(final ApplicationContext context) {
    init();
  }

  public synchronized boolean startMixpanelTelemetryScheduling() {
    LOG.info("Starting mixpanel scheduling");
    return startScheduling();
  }

  @PreDestroy
  public synchronized void stopMixpanelTelemetryScheduling() {
    LOG.info("Stopping mixpanel scheduling");
    stopScheduling();
  }

  protected TelemetryConfiguration getTelemetryConfiguration() {
    return configurationService.getTelemetryConfiguration();
  }
}
