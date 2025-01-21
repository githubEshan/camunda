/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.application.commons.migration;

import io.camunda.exporter.adapters.ClientAdapter;
import io.camunda.exporter.config.ExporterConfiguration;
import io.camunda.exporter.schema.SchemaManager;
import io.camunda.exporter.schema.SearchEngineClient;
import io.camunda.search.connect.configuration.ConnectConfiguration;
import io.camunda.search.connect.configuration.DatabaseType;
import io.camunda.webapps.schema.descriptors.IndexDescriptors;

public final class SchemaManagerHelper {
  private SchemaManagerHelper() {}

  public static void createSchema(final ConnectConfiguration connectConfig) {
    final var config = new ExporterConfiguration();
    config.setConnect(connectConfig);
    config.getIndex().setPrefix(connectConfig.getIndexPrefix());

    final var isElasticsearch = connectConfig.getTypeEnum() == DatabaseType.ELASTICSEARCH;

    final IndexDescriptors indexDescriptors =
        new IndexDescriptors(connectConfig.getIndexPrefix(), isElasticsearch);

    final SearchEngineClient client = ClientAdapter.of(config).getSearchEngineClient();
    final SchemaManager schemaManager =
        new SchemaManager(client, indexDescriptors.indices(), indexDescriptors.templates(), config);

    schemaManager.startup();
  }
}
