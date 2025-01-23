/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.it.utils;

import io.camunda.client.CamundaClient;
import io.camunda.qa.util.cluster.TestSimpleCamundaApplication;
import io.camunda.zeebe.qa.util.cluster.TestStandaloneApplication;
import io.camunda.zeebe.test.util.record.RecordingExporter;
import io.camunda.zeebe.test.util.testcontainers.TestSearchContainers;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.agrona.CloseHelper;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.ModifierSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

/**
 * An extension that is able to detect databases setups, configure {@link TestStandaloneApplication}
 * and run test against such them accordingly.
 *
 * <p>Databases can be set up externally. The detection works based on {@link
 * CamundaMultiDBExtension#PROP_CAMUNDA_IT_DATABASE_TYPE} property, which specifies the type of
 * database. Supported types can be found as part of {@link DatabaseType}.
 *
 * <p>Per default, for example if no property is set, local environment is expected. In a local
 * environment case the extension will bootstrap a database via test containers.
 *
 * <p>For simplicity tests can be annotated with {@link MultiDbTest}, and all the magic happens inside
 * the extension. It will fallback to {@link TestSimpleCamundaApplication}, to bootstrap a single
 * camunda application, configure it accordingly to the detected database.
 *
 * <pre>{@code
 * @MultiDbTest
 * final class MyMultiDbTest {
 *
 *   private CamundaClient client;
 *
 *   @Test
 *   void shouldMakeUseOfClient() {
 *     // given
 *     // ... set up
 *
 *     // when
 *     topology = c.newTopologyRequest().send().join();
 *
 *     // then
 *     assertThat(topology.getClusterSize()).isEqualTo(1);
 *   }
 * }</pre>
 *
 * <p>There are more complex scenarios that might need to start respective TestApplication externally.
 * For such cases the extension can be used via:
 * <pre>{@code
 * @RegisterExtension
 * public final CamundaMultiDBExtension extension =
 *    new CamundaMultiDBExtension(new TestStandaloneBroker());
 * }</pre>
 *
 *<p>The extension will take care of the life cycle of the {@link TestStandaloneApplication}, which
 * means configuring the detected database (this includes Operate, Tasklist, Broker properties and
 * exporter), starting the application, and tearing down at the end.
 *
 * <p>See {@link TestStandaloneApplication} for more details.
 */
public class CamundaMultiDBExtension
    implements AfterAllCallback, BeforeAllCallback, ParameterResolver {

  public static final String PROP_CAMUNDA_IT_DATABASE_TYPE =
      "test.integration.camunda.database.type";
  public static final String DEFAULT_ES_URL = "http://localhost:9200";
  public static final String DEFAULT_OS_URL = "http://localhost:9200";
  public static final String DEFAULT_OS_ADMIN_USER = "admin";
  public static final String DEFAULT_OS_ADMIN_PW = "yourStrongPassword123!";

  private static final Logger LOGGER = LoggerFactory.getLogger(CamundaMultiDBExtension.class);
  private final DatabaseType databaseType;
  private final List<AutoCloseable> closeables = new ArrayList<>();
  private final TestStandaloneApplication<?> testApplication;
  private String testPrefix;
  private final MultiDbConfigurator multiDbConfigurator;

  public CamundaMultiDBExtension() {
    this(new TestSimpleCamundaApplication());
    closeables.add(testApplication);
    testApplication
        .withBrokerConfig(cfg -> cfg.getGateway().setEnable(true))
        .withExporter(
            "recordingExporter", cfg -> cfg.setClassName(RecordingExporter.class.getName()));
  }

  public CamundaMultiDBExtension(final TestStandaloneApplication testApplication) {
    this.testApplication = testApplication;
    multiDbConfigurator = new MultiDbConfigurator(testApplication);
    // resolve active database and exporter type
    final String property = System.getProperty(PROP_CAMUNDA_IT_DATABASE_TYPE);
    databaseType =
        property == null ? DatabaseType.LOCAL : DatabaseType.valueOf(property.toUpperCase());
  }

  @Override
  public void beforeAll(final ExtensionContext context) {
    LOGGER.info("Starting up Camunda instance, with {}", databaseType);
    final Class<?> testClass = context.getRequiredTestClass();
    testPrefix = testClass.getSimpleName().toLowerCase();

    switch (databaseType) {
      case LOCAL -> {
        final ElasticsearchContainer elasticsearchContainer = setupElasticsearch();
        final String elasticSearchUrl = "http://" + elasticsearchContainer.getHttpHostAddress();
        validateESConnection(elasticSearchUrl);
        multiDbConfigurator.configureElasticsearchSupport(elasticSearchUrl, testPrefix);
      }
      case ES -> {
        validateESConnection(DEFAULT_ES_URL);
        multiDbConfigurator.configureElasticsearchSupport(DEFAULT_ES_URL, testPrefix);
      }
      case OS ->
          multiDbConfigurator.configureOpenSearchSupport(
              DEFAULT_OS_URL, testPrefix, DEFAULT_OS_ADMIN_USER, DEFAULT_OS_ADMIN_PW);
      case RDBMS -> multiDbConfigurator.configureRDBMSSupport();
      default -> throw new RuntimeException("Unknown exporter type");
    }
    testApplication.start();
    testApplication.awaitCompleteTopology();

    injectFields(testClass, null, ModifierSupport::isStatic);
  }

  private ElasticsearchContainer setupElasticsearch() {
    final ElasticsearchContainer elasticsearchContainer =
        TestSearchContainers.createDefeaultElasticsearchContainer();
    elasticsearchContainer.start();
    closeables.add(elasticsearchContainer);
    return elasticsearchContainer;
  }

  private static void validateESConnection(final String url) {
    final HttpRequest httpRequest =
        HttpRequest.newBuilder().GET().uri(URI.create(String.format("%s/", url))).build();
    try (final HttpClient httpClient = HttpClient.newHttpClient()) {
      final HttpResponse<String> response = httpClient.send(httpRequest, BodyHandlers.ofString());
      final int statusCode = response.statusCode();
      assert statusCode / 100 == 2
          : "Expected to have a running ES service available under: " + url;
    } catch (final IOException | InterruptedException e) {
      assert false
          : "Expected no exception on validating connection under: "
              + url
              + ", failed with: "
              + e
              + ": "
              + e.getMessage();
    }
  }

  private void injectFields(
      final Class<?> testClass, final Object testInstance, Predicate<Field> predicate) {
    predicate = predicate.and(field -> field.getType() == CamundaClient.class);
    for (final Field field : testClass.getDeclaredFields()) {
      try {
        if (predicate.test(field)) {
          field.setAccessible(true);
          field.set(testInstance, createCamundaClient());
        }
      } catch (final Exception ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  @Override
  public void afterAll(final ExtensionContext context) {
    if (databaseType == DatabaseType.ES || databaseType == DatabaseType.OS) {
      final URI deleteEndpoint = URI.create(String.format("%s/%s*", DEFAULT_ES_URL, testPrefix));
      final HttpRequest httpRequest = HttpRequest.newBuilder().DELETE().uri(deleteEndpoint).build();
      try (final HttpClient httpClient = HttpClient.newHttpClient()) {
        final HttpResponse<String> response = httpClient.send(httpRequest, BodyHandlers.ofString());
        final int statusCode = response.statusCode();
        if (statusCode / 100 == 2) {
          LOGGER.info("Test data deleted.");
        } else {
          LOGGER.warn("Failure on deleting test data. Status code: {}", statusCode);
        }
      } catch (final IOException | InterruptedException e) {
        LOGGER.warn("Failure on deleting test data.", e);
      }
    }

    CloseHelper.quietCloseAll(closeables);
  }

  @Override
  public boolean supportsParameter(
      final ParameterContext parameterContext, final ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return parameterContext.getParameter().getType() == CamundaClient.class;
  }

  @Override
  public Object resolveParameter(
      final ParameterContext parameterContext, final ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return createCamundaClient();
  }

  private CamundaClient createCamundaClient() {
    final CamundaClient camundaClient = testApplication.newClientBuilder().build();
    closeables.add(camundaClient);
    return camundaClient;
  }

  public enum DatabaseType {
    LOCAL,
    ES,
    OS,
    RDBMS
  }
}
