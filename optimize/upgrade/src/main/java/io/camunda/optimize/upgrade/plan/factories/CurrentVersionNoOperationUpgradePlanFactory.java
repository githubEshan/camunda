/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.upgrade.plan.factories;

import io.camunda.optimize.service.metadata.PreviousVersion;
import io.camunda.optimize.service.metadata.Version;
import io.camunda.optimize.upgrade.plan.UpgradeExecutionDependencies;
import io.camunda.optimize.upgrade.plan.UpgradePlan;
import io.camunda.optimize.upgrade.plan.UpgradePlanBuilder;
import org.slf4j.Logger;

public class CurrentVersionNoOperationUpgradePlanFactory implements UpgradePlanFactory {

  private static final Logger LOG =
      org.slf4j.LoggerFactory.getLogger(CurrentVersionNoOperationUpgradePlanFactory.class);

  public UpgradePlan createUpgradePlan() {
    return UpgradePlanBuilder.createUpgradePlan()
        .fromVersion(PreviousVersion.PREVIOUS_VERSION_MAJOR_MINOR)
        .toVersion(Version.VERSION)
        .build();
  }

  @Override
  public UpgradePlan createUpgradePlan(final UpgradeExecutionDependencies dependencies) {
    return createUpgradePlan();
  }

  @Override
  public void logErrorMessage(final String message) {
    LOG.error(message);
  }
}
