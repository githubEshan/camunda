/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.schema.index;

import co.elastic.clients.elasticsearch.indices.IndexSettings.Builder;
import io.camunda.optimize.service.db.es.schema.index.DecisionInstanceIndexES;
import io.camunda.optimize.service.db.es.schema.index.ProcessInstanceIndexES;
import io.camunda.optimize.service.db.os.schema.index.DecisionInstanceIndexOS;
import io.camunda.optimize.service.db.os.schema.index.ProcessInstanceIndexOS;
import io.camunda.optimize.service.db.schema.IndexMappingCreator;
import java.util.function.Function;
import org.opensearch.client.opensearch.indices.IndexSettings;

public enum IndexMappingCreatorBuilder {
  DECISION_INSTANCE_INDEX(DecisionInstanceIndexES::new, DecisionInstanceIndexOS::new),
  PROCESS_INSTANCE_INDEX(ProcessInstanceIndexES::new, ProcessInstanceIndexOS::new);

  private final Function<
          String,
          IndexMappingCreator<co.elastic.clients.elasticsearch.indices.IndexSettings.Builder>>
      elasticsearch;
  private final Function<String, IndexMappingCreator<IndexSettings.Builder>> opensearch;

  private IndexMappingCreatorBuilder(
      final Function<String, IndexMappingCreator<Builder>> elasticsearch,
      final Function<String, IndexMappingCreator<IndexSettings.Builder>> opensearch) {
    this.elasticsearch = elasticsearch;
    this.opensearch = opensearch;
  }

  public Function<String, IndexMappingCreator<Builder>> getElasticsearch() {
    return elasticsearch;
  }

  public Function<String, IndexMappingCreator<IndexSettings.Builder>> getOpensearch() {
    return opensearch;
  }
}
