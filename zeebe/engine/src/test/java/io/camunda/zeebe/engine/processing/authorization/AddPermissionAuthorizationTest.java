/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.processing.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.camunda.zeebe.engine.util.EngineRule;
import io.camunda.zeebe.protocol.record.Assertions;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.value.AuthorizationOwnerType;
import io.camunda.zeebe.protocol.record.value.AuthorizationRecordValue;
import io.camunda.zeebe.protocol.record.value.AuthorizationRecordValue.PermissionValue;
import io.camunda.zeebe.protocol.record.value.AuthorizationResourceType;
import io.camunda.zeebe.protocol.record.value.EntityType;
import io.camunda.zeebe.protocol.record.value.PermissionType;
import io.camunda.zeebe.test.util.record.RecordingExporterTestWatcher;
import java.util.List;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;

public class AddPermissionAuthorizationTest {

  @Rule public final EngineRule engine = EngineRule.singlePartition();
  @Rule public final TestWatcher recordingExporterTestWatcher = new RecordingExporterTestWatcher();

  @Test
  public void shouldAddPermission() {
    // given no user
    final var owner =
        engine
            .user()
            .newUser("foo")
            .withEmail("foo@bar")
            .withName("Foo Bar")
            .withPassword("zabraboof")
            .create();

    // when
    final var response =
        engine
            .authorization()
            .permission()
            .withOwnerKey(owner.getKey())
            .withOwnerId(owner.getValue().getUsername())
            .withOwnerType(AuthorizationOwnerType.USER)
            .withResourceType(AuthorizationResourceType.RESOURCE)
            .withPermission(PermissionType.CREATE, "foo")
            .withPermission(PermissionType.DELETE_PROCESS, "bar")
            .add()
            .getValue();

    // then
    assertThat(response)
        .extracting(
            AuthorizationRecordValue::getOwnerKey,
            AuthorizationRecordValue::getOwnerType,
            AuthorizationRecordValue::getResourceType)
        .containsExactly(
            owner.getKey(), AuthorizationOwnerType.USER, AuthorizationResourceType.RESOURCE);
    assertThat(response.getPermissions())
        .extracting(PermissionValue::getPermissionType, PermissionValue::getResourceIds)
        .containsExactly(
            tuple(PermissionType.CREATE, Set.of("foo")),
            tuple(PermissionType.DELETE_PROCESS, Set.of("bar")));
  }

  @Test
  public void shouldRejectIfNoOwnerExists() {
    // given no user
    final var ownerKey = 1L;

    // when
    final var rejection =
        engine
            .authorization()
            .permission()
            .withOwnerKey(ownerKey)
            .withOwnerId("bar")
            .withOwnerType(AuthorizationOwnerType.USER)
            .withResourceType(AuthorizationResourceType.RESOURCE)
            .withPermission(PermissionType.CREATE, "foo")
            .expectRejection()
            .add();

    // then
    Assertions.assertThat(rejection)
        .describedAs("Owner is not found")
        .hasRejectionType(RejectionType.NOT_FOUND)
        .hasRejectionReason(
            "Expected to find owner with key: '%d', but none was found".formatted(ownerKey));
  }

  @Test
  public void shouldRejectIfPermissionAlreadyExistsDirectly() {
    // given
    final var owner =
        engine
            .user()
            .newUser("foo")
            .withEmail("foo@bar")
            .withName("Foo Bar")
            .withPassword("zabraboof")
            .create();
    final var ownerKey = owner.getKey();

    engine
        .authorization()
        .permission()
        .withOwnerKey(ownerKey)
        .withOwnerId(owner.getValue().getUsername())
        .withOwnerType(AuthorizationOwnerType.USER)
        .withResourceType(AuthorizationResourceType.RESOURCE)
        .withPermission(PermissionType.CREATE, "foo")
        .withPermission(PermissionType.DELETE_PROCESS, "bar", "baz")
        .add()
        .getValue();

    // when
    final var rejection =
        engine
            .authorization()
            .permission()
            .withOwnerKey(ownerKey)
            .withOwnerId(owner.getValue().getUsername())
            .withOwnerType(AuthorizationOwnerType.USER)
            .withResourceType(AuthorizationResourceType.RESOURCE)
            .withPermission(PermissionType.DELETE_PROCESS, "foo", "bar")
            .expectRejection()
            .add();

    // then
    Assertions.assertThat(rejection)
        .describedAs("Permission already exists")
        .hasRejectionType(RejectionType.ALREADY_EXISTS)
        .hasRejectionReason(
            "Expected to add '%s' permission for resource '%s' and resource identifiers '%s' for owner '%s', but this permission for resource identifiers '%s' already exist. Existing resource ids are: '%s'"
                .formatted(
                    PermissionType.DELETE_PROCESS,
                    AuthorizationResourceType.RESOURCE,
                    "[bar, foo]",
                    ownerKey,
                    "[bar]",
                    "[bar, baz]"));
  }

  @Test
  public void shouldNotRejectIfPermissionAlreadyExistsOnRole() {
    // given -- user is assigned a role that has the permission
    final var owner =
        engine
            .user()
            .newUser("foo")
            .withEmail("foo@bar")
            .withName("Foo Bar")
            .withPassword("zabraboof")
            .create();
    final var ownerKey = owner.getKey();
    final var ownerId = owner.getValue().getUsername();
    final var roleKey = engine.role().newRole("role").create().getKey();
    engine.role().addEntity(roleKey).withEntityKey(ownerKey).withEntityType(EntityType.USER).add();
    engine
        .authorization()
        .permission()
        .withOwnerKey(roleKey)
        .withOwnerId(String.valueOf(roleKey))
        .withOwnerType(AuthorizationOwnerType.ROLE)
        .withResourceType(AuthorizationResourceType.RESOURCE)
        .withPermission(PermissionType.CREATE, "foo")
        .withPermission(PermissionType.DELETE_PROCESS, "bar", "baz")
        .add()
        .getValue();

    // when -- assigning the permission directly to the user
    final var response =
        engine
            .authorization()
            .permission()
            .withOwnerKey(ownerKey)
            .withOwnerId(ownerId)
            .withOwnerType(AuthorizationOwnerType.USER)
            .withResourceType(AuthorizationResourceType.RESOURCE)
            .withPermission(PermissionType.DELETE_PROCESS, "foo", "bar")
            .add();

    // then
    assertThat(response.getValue())
        .extracting(
            AuthorizationRecordValue::getOwnerKey,
            AuthorizationRecordValue::getOwnerType,
            AuthorizationRecordValue::getResourceType)
        .containsExactly(ownerKey, AuthorizationOwnerType.USER, AuthorizationResourceType.RESOURCE);
  }

  @Test
  public void shouldNotRejectIfPermissionAlreadyExistsOnGroup() {
    // given -- user is assigned a group that has the permission
    final var owner =
        engine
            .user()
            .newUser("foo")
            .withEmail("foo@bar")
            .withName("Foo Bar")
            .withPassword("zabraboof")
            .create();
    final var ownerKey = owner.getKey();
    final var ownerId = owner.getValue().getUsername();
    final var groupKey = engine.group().newGroup("group").create().getKey();
    engine
        .group()
        .addEntity(groupKey)
        .withEntityKey(ownerKey)
        .withEntityType(EntityType.USER)
        .add();
    engine
        .authorization()
        .permission()
        .withOwnerKey(groupKey)
        .withOwnerId(String.valueOf(groupKey))
        .withOwnerType(AuthorizationOwnerType.GROUP)
        .withResourceType(AuthorizationResourceType.RESOURCE)
        .withPermission(PermissionType.CREATE, "foo")
        .withPermission(PermissionType.DELETE_PROCESS, "bar", "baz")
        .add()
        .getValue();

    // when -- assigning the permission directly to the user
    final var response =
        engine
            .authorization()
            .permission()
            .withOwnerKey(ownerKey)
            .withOwnerId(ownerId)
            .withOwnerType(AuthorizationOwnerType.USER)
            .withResourceType(AuthorizationResourceType.RESOURCE)
            .withPermission(PermissionType.DELETE_PROCESS, "foo", "bar")
            .add();

    // then
    assertThat(response.getValue())
        .extracting(
            AuthorizationRecordValue::getOwnerKey,
            AuthorizationRecordValue::getOwnerType,
            AuthorizationRecordValue::getResourceType)
        .containsExactly(ownerKey, AuthorizationOwnerType.USER, AuthorizationResourceType.RESOURCE);
  }

  @Test
  public void shouldRejectAddingUnsupportedPermission() {
    // given
    final var owner =
        engine
            .user()
            .newUser("foo")
            .withEmail("foo@bar")
            .withName("Foo Bar")
            .withPassword("zabraboof")
            .create();
    final var ownerKey = owner.getKey();
    final var ownerId = owner.getValue().getUsername();
    final var resourceType = AuthorizationResourceType.RESOURCE;

    // when
    final var rejection =
        engine
            .authorization()
            .permission()
            .withOwnerKey(ownerKey)
            .withOwnerId(ownerId)
            .withOwnerType(AuthorizationOwnerType.USER)
            .withResourceType(resourceType)
            .withPermission(PermissionType.CREATE, "foo")
            .withPermission(PermissionType.DELETE_PROCESS, "foo")
            .withPermission(PermissionType.ACCESS, "foo")
            .withPermission(PermissionType.READ_PROCESS_INSTANCE, "foo")
            .expectRejection()
            .add();

    // then
    Assertions.assertThat(rejection)
        .hasRejectionType(RejectionType.INVALID_ARGUMENT)
        .hasRejectionReason(
            "Expected to add permission types '%s' for resource type '%s', but these permissions are not supported. Supported permission types are: '%s'"
                .formatted(
                    List.of(PermissionType.ACCESS, PermissionType.READ_PROCESS_INSTANCE),
                    resourceType,
                    resourceType.getSupportedPermissionTypes()));
  }
}
