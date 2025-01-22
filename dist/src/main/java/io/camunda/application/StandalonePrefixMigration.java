/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.application;

import io.camunda.application.StandaloneSchemaManager.SchemaManagerConnectConfiguration;
import io.camunda.application.commons.migration.PrefixMigrationConfig;
import io.camunda.application.commons.migration.PrefixMigrationHelper;
import io.camunda.application.commons.migration.SchemaManagerHelper;
import io.camunda.exporter.adapters.ClientAdapter;
import io.camunda.exporter.config.ExporterConfiguration;
import io.camunda.operate.property.OperateProperties;
import io.camunda.search.connect.configuration.DatabaseType;
import io.camunda.tasklist.property.TasklistProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootConfiguration(proxyBeanMethods = false)
public class StandalonePrefixMigration {
  private static final Logger LOG = LoggerFactory.getLogger(StandalonePrefixMigration.class);

  public static void main(final String[] args) {
    // To ensure that debug logging performed using java.util.logging is routed into Log4j 2
    MainSupport.putSystemPropertyIfAbsent(
        "java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");

    final SpringApplication application =
        new SpringApplicationBuilder()
            .logStartupInfo(true)
            .web(WebApplicationType.NONE)
            .sources(
                StandaloneSchemaManager.class,
                SchemaManagerConnectConfiguration.class,
                TasklistProperties.class,
                OperateProperties.class,
                PrefixMigrationConfig.class)
            .addCommandLineProperties(true)
            .build(args);

    final ConfigurableApplicationContext applicationContext = application.run(args);

    final var operateProperties = applicationContext.getBean(OperateProperties.class);
    final var tasklistProperties = applicationContext.getBean(TasklistProperties.class);
    final SchemaManagerConnectConfiguration connectConfiguration =
        applicationContext.getBean(SchemaManagerConnectConfiguration.class);
    final var isElasticsearch = connectConfiguration.getTypeEnum() == DatabaseType.ELASTICSEARCH;

    LOG.info("Creating/updating Elasticsearch schema for Camunda ...");

    SchemaManagerHelper.createSchema(connectConfiguration);

    LOG.info("... finished creating/updating schema for Camunda");

    LOG.info("Migrating runtime indices");

    final var exporterConfig = new ExporterConfiguration();
    exporterConfig.setConnect(connectConfiguration);

    final var searchEngineClient = ClientAdapter.of(exporterConfig).getSearchEngineClient();

    final var operatePrefix =
        isElasticsearch
            ? operateProperties.getElasticsearch().getIndexPrefix()
            : operateProperties.getOpensearch().getIndexPrefix();
    final var tasklistPrefix =
        isElasticsearch
            ? tasklistProperties.getElasticsearch().getIndexPrefix()
            : tasklistProperties.getOpenSearch().getIndexPrefix();

    final var executor = applicationContext.getBean(PrefixMigrationConfig.class).getTaskExecutor();

    PrefixMigrationHelper.migrateRuntimeIndices(
        operatePrefix, tasklistPrefix, connectConfiguration, searchEngineClient, executor);

    LOG.info("... finished migrating runtime indices");

    LOG.info("Migrating historic indices");

    searchEngineClient.cloneArchivedIndices(
        operatePrefix, tasklistPrefix, connectConfiguration.getIndexPrefix());

    LOG.info("... finished migrating historic indices");

    System.exit(0);
  }
}
