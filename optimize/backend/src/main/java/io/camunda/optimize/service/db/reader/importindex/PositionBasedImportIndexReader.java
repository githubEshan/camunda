/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.reader.importindex;

import static io.camunda.optimize.service.db.DatabaseConstants.POSITION_BASED_IMPORT_INDEX_NAME;

import io.camunda.optimize.dto.optimize.datasource.ZeebeDataSourceDto;
import io.camunda.optimize.dto.optimize.index.PositionBasedImportIndexDto;
import io.camunda.optimize.service.db.repository.ImportRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class PositionBasedImportIndexReader
    implements ImportIndexReader<PositionBasedImportIndexDto, ZeebeDataSourceDto> {

  private static final Logger LOG =
      org.slf4j.LoggerFactory.getLogger(PositionBasedImportIndexReader.class);
  private final ImportRepository importRepository;

  public PositionBasedImportIndexReader(final ImportRepository importRepository) {
    this.importRepository = importRepository;
  }

  @Override
  public Optional<PositionBasedImportIndexDto> getImportIndex(
      final String typeIndexComesFrom, final ZeebeDataSourceDto dataSourceDto) {
    return importRepository.getImportIndex(
        POSITION_BASED_IMPORT_INDEX_NAME,
        "position based",
        PositionBasedImportIndexDto.class,
        typeIndexComesFrom,
        dataSourceDto);
  }
}
