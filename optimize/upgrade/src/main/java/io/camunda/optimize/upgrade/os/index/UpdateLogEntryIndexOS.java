/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.upgrade.os.index;

import io.camunda.optimize.upgrade.db.index.UpdateLogEntryIndex;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.opensearch.client.opensearch.indices.IndexSettings.Builder;
import org.springframework.stereotype.Component;

@Component
public class UpdateLogEntryIndexOS extends UpdateLogEntryIndex<Builder> {

  @Override
  public IndexSettings.Builder addStaticSetting(
      final String key, final int value, final IndexSettings.Builder contentBuilder) {
    return contentBuilder.numberOfShards(Integer.toString(value));
  }
}
