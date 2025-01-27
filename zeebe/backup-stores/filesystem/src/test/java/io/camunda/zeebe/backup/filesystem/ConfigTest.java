/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.backup.filesystem;

import java.nio.file.Path;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ConfigTest {

  @TempDir private Path path;

  @Test
  void shouldSuccessfullyValidateConfig() {
    final FilesystemBackupConfig backupConfig =
        new FilesystemBackupConfig.Builder().withBasePath(path.toString()).build();

    Assertions.assertThatCode(() -> new FilesystemBackupStore(backupConfig))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldFailDueToMissingBaseDir() {
    final FilesystemBackupConfig backupConfig = new FilesystemBackupConfig.Builder().build();

    Assertions.assertThatCode(() -> new FilesystemBackupStore(backupConfig))
        .hasMessage("Base directory is required");
  }

  @Test
  void shouldFailDueToEmptyBaseDir() {
    final FilesystemBackupConfig backupConfig =
        new FilesystemBackupConfig.Builder().withBasePath("").build();

    Assertions.assertThatCode(() -> new FilesystemBackupStore(backupConfig))
        .hasMessage("Base directory is required");
  }
}
