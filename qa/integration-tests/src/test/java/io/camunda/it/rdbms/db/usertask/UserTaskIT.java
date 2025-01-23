/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.it.rdbms.db.usertask;

import static io.camunda.it.rdbms.db.fixtures.CommonFixtures.NOW;
import static io.camunda.it.rdbms.db.fixtures.UserTaskFixtures.createAndSaveRandomUserTasks;
import static io.camunda.it.rdbms.db.fixtures.UserTaskFixtures.createAndSaveUserTask;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.db.rdbms.RdbmsService;
import io.camunda.db.rdbms.read.domain.UserTaskDbQuery;
import io.camunda.db.rdbms.read.service.UserTaskReader;
import io.camunda.db.rdbms.write.RdbmsWriter;
import io.camunda.db.rdbms.write.domain.UserTaskDbModel;
import io.camunda.db.rdbms.write.domain.UserTaskDbModel.UserTaskState;
import io.camunda.db.rdbms.write.domain.VariableDbModel;
import io.camunda.it.rdbms.db.fixtures.UserTaskFixtures;
import io.camunda.it.rdbms.db.fixtures.VariableFixtures;
import io.camunda.it.rdbms.db.util.CamundaRdbmsInvocationContextProviderExtension;
import io.camunda.it.rdbms.db.util.CamundaRdbmsTestApplication;
import io.camunda.search.entities.UserTaskEntity;
import io.camunda.search.filter.Operation;
import io.camunda.search.filter.UntypedOperation;
import io.camunda.search.filter.UserTaskFilter;
import io.camunda.search.filter.VariableValueFilter;
import io.camunda.search.page.SearchQueryPage;
import io.camunda.search.sort.UserTaskSort;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.assertj.core.data.TemporalUnitWithinOffset;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

@Tag("rdbms")
@ExtendWith(CamundaRdbmsInvocationContextProviderExtension.class)
public class UserTaskIT {

  @TestTemplate
  public void shouldCreateAndFindUserTaskByKey(final CamundaRdbmsTestApplication testApplication) {
    final RdbmsService rdbmsService = testApplication.getRdbmsService();

    final UserTaskDbModel randomizedUserTask = UserTaskFixtures.createRandomized();
    createAndSaveUserTask(rdbmsService, randomizedUserTask);

    final var instance = rdbmsService.getUserTaskReader().findOne(randomizedUserTask.userTaskKey());
    assertUserTaskEntity(instance, randomizedUserTask);
  }

  @TestTemplate
  public void shouldCreateAndFindUserTaskWithoutCandidatesByKey(
      final CamundaRdbmsTestApplication testApplication) {
    final RdbmsService rdbmsService = testApplication.getRdbmsService();

    final UserTaskDbModel randomizedUserTask =
        UserTaskFixtures.createRandomized(b -> b.candidateGroups(null).candidateUsers(null));
    createAndSaveUserTask(rdbmsService, randomizedUserTask);

    final var instance = rdbmsService.getUserTaskReader().findOne(randomizedUserTask.userTaskKey());
    assertUserTaskEntity(instance, randomizedUserTask);
  }

  @TestTemplate
  public void shouldUpdateAndFindUserTaskByKey(final CamundaRdbmsTestApplication testApplication) {
    final RdbmsService rdbmsService = testApplication.getRdbmsService();

    final UserTaskDbModel randomizedUserTask = UserTaskFixtures.createRandomized();
    createAndSaveUserTask(rdbmsService, randomizedUserTask);

    final UserTaskDbModel updatedModel =
        randomizedUserTask.toBuilder()
            .state(UserTaskState.COMPLETED)
            .completionDate(NOW.plusDays(2))
            .build();
    final RdbmsWriter writer = rdbmsService.createWriter(1L);
    writer.getUserTaskWriter().update(updatedModel);
    writer.flush();

    final var instance = rdbmsService.getUserTaskReader().findOne(randomizedUserTask.userTaskKey());
    assertUserTaskEntity(instance, updatedModel);
  }

  @TestTemplate
  public void shouldUpdateAndFindUserTaskWithoutCandidatesByKey(
      final CamundaRdbmsTestApplication testApplication) {
    final RdbmsService rdbmsService = testApplication.getRdbmsService();

    final UserTaskDbModel randomizedUserTask =
        UserTaskFixtures.createRandomized(b -> b.candidateGroups(null).candidateUsers(null));
    createAndSaveUserTask(rdbmsService, randomizedUserTask);

    final UserTaskDbModel updatedModel =
        randomizedUserTask.toBuilder()
            .state(UserTaskState.COMPLETED)
            .completionDate(NOW.plusDays(2))
            .assignee("newAssignee")
            .priority(2)
            .dueDate(NOW.plusDays(3))
            .followUpDate(NOW.plusDays(4))
            .build();
    final RdbmsWriter writer = rdbmsService.createWriter(1L);
    writer.getUserTaskWriter().update(updatedModel);
    writer.flush();

    final var instance = rdbmsService.getUserTaskReader().findOne(randomizedUserTask.userTaskKey());
    assertUserTaskEntity(instance, updatedModel);
  }

  @TestTemplate
  public void shouldDeletingExistingCandidates(final CamundaRdbmsTestApplication testApplication) {
    final RdbmsService rdbmsService = testApplication.getRdbmsService();

    final UserTaskDbModel randomizedUserTask = UserTaskFixtures.createRandomized();
    createAndSaveUserTask(rdbmsService, randomizedUserTask);

    final UserTaskDbModel updatedModel =
        randomizedUserTask.toBuilder().candidateGroups(null).candidateUsers(null).build();
    final RdbmsWriter writer = rdbmsService.createWriter(1L);
    writer.getUserTaskWriter().update(updatedModel);
    writer.flush();

    final var instance = rdbmsService.getUserTaskReader().findOne(randomizedUserTask.userTaskKey());
    assertUserTaskEntity(instance, updatedModel);
  }

  @TestTemplate
  public void shouldUpdateCandidateUserAndGroupAndFindByKey(
      final CamundaRdbmsTestApplication testApplication) {
    final RdbmsService rdbmsService = testApplication.getRdbmsService();
    final RdbmsWriter rdbmsWriter = rdbmsService.createWriter(1L);

    final UserTaskDbModel randomizedUserTask = UserTaskFixtures.createRandomized();
    createAndSaveUserTask(rdbmsWriter, randomizedUserTask);

    final UserTaskDbModel updatedModel =
        randomizedUserTask.toBuilder()
            .candidateUsers(List.of("user1", "user2", "user3"))
            .candidateGroups(List.of("group1", "group2"))
            .build();
    rdbmsWriter.getUserTaskWriter().update(updatedModel);
    rdbmsWriter.flush();

    final var instance = rdbmsService.getUserTaskReader().findOne(randomizedUserTask.userTaskKey());
    assertUserTaskEntity(instance, updatedModel);
  }

  @TestTemplate
  public void shouldFindUserTaskByProcessInstanceKey(
      final CamundaRdbmsTestApplication testApplication) {
    final RdbmsService rdbmsService = testApplication.getRdbmsService();

    final UserTaskDbModel randomizedUserTask = UserTaskFixtures.createRandomized();
    createAndSaveUserTask(rdbmsService, randomizedUserTask);

    final var searchResult =
        rdbmsService
            .getUserTaskReader()
            .search(
                new UserTaskDbQuery(
                    new UserTaskFilter.Builder()
                        .processInstanceKeys(randomizedUserTask.processInstanceKey())
                        .build(),
                    UserTaskSort.of(b -> b),
                    SearchQueryPage.of(b -> b.from(0).size(10))));

    assertThat(searchResult).isNotNull();
    assertThat(searchResult.total()).isEqualTo(1);
    assertThat(searchResult.hits()).hasSize(1);
    assertUserTaskEntity(searchResult.hits().getFirst(), randomizedUserTask);
  }

  @TestTemplate
  public void shouldFindUserTaskByProcessInstanceVariableName(
      final CamundaRdbmsTestApplication testApplication) {
    final RdbmsService rdbmsService = testApplication.getRdbmsService();

    final UserTaskDbModel randomizedUserTask = UserTaskFixtures.createRandomized();
    createAndSaveUserTask(rdbmsService, randomizedUserTask);

    final VariableDbModel randomizedVariable =
        VariableFixtures.createRandomized(
            b -> b.processInstanceKey(randomizedUserTask.processInstanceKey()));
    final RdbmsWriter writer = rdbmsService.createWriter(1L);
    writer.getVariableWriter().create(randomizedVariable);
    writer.flush();

    final var searchResult =
        rdbmsService
            .getUserTaskReader()
            .search(
                new UserTaskDbQuery(
                    new UserTaskFilter.Builder()
                        .processInstanceVariables(
                            List.of(
                                new VariableValueFilter.Builder()
                                    .name(randomizedVariable.name())
                                    .build()))
                        .build(),
                    UserTaskSort.of(b -> b),
                    SearchQueryPage.of(b -> b.from(0).size(10))));

    assertThat(searchResult).isNotNull();
    assertThat(searchResult.total()).isEqualTo(1);
    assertThat(searchResult.hits()).hasSize(1);
    assertUserTaskEntity(searchResult.hits().getFirst(), randomizedUserTask);
  }

  @TestTemplate
  public void shouldFindUserTaskByProcessInstanceVariableNameAndValue(
      final CamundaRdbmsTestApplication testApplication) {
    final RdbmsService rdbmsService = testApplication.getRdbmsService();

    final UserTaskDbModel randomizedUserTask = UserTaskFixtures.createRandomized();
    createAndSaveUserTask(rdbmsService, randomizedUserTask);

    final VariableDbModel randomizedVariable =
        VariableFixtures.createRandomized(
            b -> b.processInstanceKey(randomizedUserTask.processInstanceKey()));
    final RdbmsWriter writer = rdbmsService.createWriter(1L);
    writer.getVariableWriter().create(randomizedVariable);
    writer.flush();

    final var searchResult =
        rdbmsService
            .getUserTaskReader()
            .search(
                new UserTaskDbQuery(
                    new UserTaskFilter.Builder()
                        .processInstanceVariables(
                            List.of(
                                new VariableValueFilter.Builder()
                                    .name(randomizedVariable.name())
                                    .valueOperation(
                                        UntypedOperation.of(
                                            Operation.eq(randomizedVariable.value())))
                                    .build()))
                        .build(),
                    UserTaskSort.of(b -> b),
                    SearchQueryPage.of(b -> b.from(0).size(10))));

    assertThat(searchResult).isNotNull();
    assertThat(searchResult.total()).isEqualTo(1);
    assertThat(searchResult.hits()).hasSize(1);
    assertUserTaskEntity(searchResult.hits().getFirst(), randomizedUserTask);
  }

  @TestTemplate
  public void shouldFindUserTaskByLocalVariableName(
      final CamundaRdbmsTestApplication testApplication) {
    final RdbmsService rdbmsService = testApplication.getRdbmsService();

    final UserTaskDbModel randomizedUserTask = UserTaskFixtures.createRandomized();
    createAndSaveUserTask(rdbmsService, randomizedUserTask);

    final VariableDbModel randomizedVariable =
        VariableFixtures.createRandomized(
            b -> b.scopeKey(randomizedUserTask.userTaskKey()).name("localVariable"));
    final RdbmsWriter writer = rdbmsService.createWriter(1L);
    writer.getVariableWriter().create(randomizedVariable);
    writer.flush();

    final var searchResult =
        rdbmsService
            .getUserTaskReader()
            .search(
                new UserTaskDbQuery(
                    new UserTaskFilter.Builder()
                        .localVariables(
                            List.of(
                                new VariableValueFilter.Builder().name("localVariable").build()))
                        .build(),
                    UserTaskSort.of(b -> b),
                    SearchQueryPage.of(b -> b.from(0).size(10))));

    assertThat(searchResult).isNotNull();
    assertThat(searchResult.total()).isEqualTo(1);
    assertThat(searchResult.hits()).hasSize(1);
    assertUserTaskEntity(searchResult.hits().getFirst(), randomizedUserTask);
  }

  @TestTemplate
  public void shouldFindAllUserTasksPaged(final CamundaRdbmsTestApplication testApplication) {
    final RdbmsService rdbmsService = testApplication.getRdbmsService();

    final String processDefinitionId = UserTaskFixtures.nextStringId();
    createAndSaveRandomUserTasks(rdbmsService, processDefinitionId);

    final var searchResult =
        rdbmsService
            .getUserTaskReader()
            .search(
                new UserTaskDbQuery(
                    new UserTaskFilter.Builder().bpmnProcessIds(processDefinitionId).build(),
                    UserTaskSort.of(b -> b),
                    SearchQueryPage.of(b -> b.from(0).size(5))));

    assertThat(searchResult).isNotNull();
    assertThat(searchResult.total()).isEqualTo(20);
    assertThat(searchResult.hits()).hasSize(5);
  }

  @TestTemplate
  public void shouldFindAllUserTasksPageValuesAreNull(
      final CamundaRdbmsTestApplication testApplication) {
    final RdbmsService rdbmsService = testApplication.getRdbmsService();

    final String processDefinitionId = UserTaskFixtures.nextStringId();
    createAndSaveRandomUserTasks(rdbmsService, processDefinitionId);

    final var searchResult =
        rdbmsService
            .getUserTaskReader()
            .search(
                new UserTaskDbQuery(
                    new UserTaskFilter.Builder().bpmnProcessIds(processDefinitionId).build(),
                    UserTaskSort.of(b -> b),
                    SearchQueryPage.of(b -> b.from(null).size(null))));

    assertThat(searchResult).isNotNull();
    assertThat(searchResult.total()).isEqualTo(20);
    assertThat(searchResult.hits()).hasSize(20);
  }

  @TestTemplate
  public void shouldFindUserTaskByCreationDateGt(
      final CamundaRdbmsTestApplication testApplication) {
    final RdbmsService rdbmsService = testApplication.getRdbmsService();

    final var creationDate = NOW.plusDays(15).truncatedTo(ChronoUnit.SECONDS);

    final UserTaskDbModel randomizedUserTask =
        UserTaskFixtures.createRandomized(b -> b.creationDate(creationDate));
    createAndSaveUserTask(rdbmsService, randomizedUserTask);

    final var searchResult =
        rdbmsService
            .getUserTaskReader()
            .search(
                new UserTaskDbQuery(
                    new UserTaskFilter.Builder()
                        .creationDateOperations(Operation.gt(creationDate.minusDays(1)))
                        .build(),
                    UserTaskSort.of(b -> b),
                    SearchQueryPage.of(b -> b.from(0).size(10))));

    assertThat(searchResult.total()).isEqualTo(1);
    assertThat(searchResult.hits()).hasSize(1);
    assertUserTaskEntity(searchResult.hits().getFirst(), randomizedUserTask);
  }

  @TestTemplate
  public void shouldFindUserTaskByCompletionDateGte(
      final CamundaRdbmsTestApplication testApplication) {
    final RdbmsService rdbmsService = testApplication.getRdbmsService();

    final var completionDate = NOW.plusDays(15).truncatedTo(ChronoUnit.SECONDS);

    final UserTaskDbModel randomizedUserTask =
        UserTaskFixtures.createRandomized(b -> b.completionDate(completionDate));
    createAndSaveUserTask(rdbmsService, randomizedUserTask);

    final var searchResult =
        rdbmsService
            .getUserTaskReader()
            .search(
                new UserTaskDbQuery(
                    new UserTaskFilter.Builder()
                        .completionDateOperations(Operation.gte(completionDate))
                        .build(),
                    UserTaskSort.of(b -> b),
                    SearchQueryPage.of(b -> b.from(0).size(10))));

    assertThat(searchResult.total()).isEqualTo(1);
    assertThat(searchResult.hits()).hasSize(1);
    assertUserTaskEntity(searchResult.hits().getFirst(), randomizedUserTask);
  }

  @TestTemplate
  public void shouldFindUserTaskByCreationDateLte(
      final CamundaRdbmsTestApplication testApplication) {
    final RdbmsService rdbmsService = testApplication.getRdbmsService();

    final var creationDate = NOW.minusDays(20).truncatedTo(ChronoUnit.SECONDS);

    final UserTaskDbModel randomizedUserTask =
        UserTaskFixtures.createRandomized(b -> b.creationDate(creationDate));
    createAndSaveUserTask(rdbmsService, randomizedUserTask);

    final var searchResult =
        rdbmsService
            .getUserTaskReader()
            .search(
                new UserTaskDbQuery(
                    new UserTaskFilter.Builder()
                        .creationDateOperations(Operation.lte(creationDate))
                        .build(),
                    UserTaskSort.of(b -> b),
                    SearchQueryPage.of(b -> b.from(0).size(10))));

    assertThat(searchResult.total()).isEqualTo(1);
    assertThat(searchResult.hits()).hasSize(1);
    assertUserTaskEntity(searchResult.hits().getFirst(), randomizedUserTask);
  }

  @TestTemplate
  public void shouldFindUserTaskByCompletionDateLt(
      final CamundaRdbmsTestApplication testApplication) {
    final RdbmsService rdbmsService = testApplication.getRdbmsService();

    final var completionDate = NOW.minusDays(15).truncatedTo(ChronoUnit.SECONDS);

    final UserTaskDbModel randomizedUserTask =
        UserTaskFixtures.createRandomized(b -> b.completionDate(completionDate));
    createAndSaveUserTask(rdbmsService, randomizedUserTask);

    final var searchResultGte =
        rdbmsService
            .getUserTaskReader()
            .search(
                new UserTaskDbQuery(
                    new UserTaskFilter.Builder()
                        .completionDateOperations(Operation.lt(completionDate.plusDays(1)))
                        .build(),
                    UserTaskSort.of(b -> b),
                    SearchQueryPage.of(b -> b.from(0).size(10))));

    assertThat(searchResultGte.total()).isEqualTo(1);
    assertThat(searchResultGte.hits()).hasSize(1);
    assertUserTaskEntity(searchResultGte.hits().getFirst(), randomizedUserTask);
  }

  @TestTemplate
  public void shouldFindUserTaskByDueDateGt(final CamundaRdbmsTestApplication testApplication) {
    final RdbmsService rdbmsService = testApplication.getRdbmsService();

    final var dueDate = NOW.plusDays(15).truncatedTo(ChronoUnit.SECONDS);

    final UserTaskDbModel randomizedUserTask =
        UserTaskFixtures.createRandomized(b -> b.dueDate(dueDate));
    createAndSaveUserTask(rdbmsService, randomizedUserTask);

    final var searchResult =
        rdbmsService
            .getUserTaskReader()
            .search(
                new UserTaskDbQuery(
                    new UserTaskFilter.Builder()
                        .dueDateOperations(Operation.gt(dueDate.minusDays(1)))
                        .build(),
                    UserTaskSort.of(b -> b),
                    SearchQueryPage.of(b -> b.from(0).size(10))));

    assertThat(searchResult.total()).isEqualTo(1);
    assertThat(searchResult.hits()).hasSize(1);
    assertUserTaskEntity(searchResult.hits().getFirst(), randomizedUserTask);
  }

  @TestTemplate
  public void shouldFindUserTaskByDueDateLte(final CamundaRdbmsTestApplication testApplication) {
    final RdbmsService rdbmsService = testApplication.getRdbmsService();

    final var dueDate = NOW.minusDays(20).truncatedTo(ChronoUnit.SECONDS);

    final UserTaskDbModel randomizedUserTask =
        UserTaskFixtures.createRandomized(b -> b.dueDate(dueDate));
    createAndSaveUserTask(rdbmsService, randomizedUserTask);

    final var searchResult =
        rdbmsService
            .getUserTaskReader()
            .search(
                new UserTaskDbQuery(
                    new UserTaskFilter.Builder().dueDateOperations(Operation.lte(dueDate)).build(),
                    UserTaskSort.of(b -> b),
                    SearchQueryPage.of(b -> b.from(0).size(10))));

    assertThat(searchResult.total()).isEqualTo(1);
    assertThat(searchResult.hits()).hasSize(1);
    assertUserTaskEntity(searchResult.hits().getFirst(), randomizedUserTask);
  }

  @TestTemplate
  public void shouldFindUserTaskByFollowUpDateGt(
      final CamundaRdbmsTestApplication testApplication) {
    final RdbmsService rdbmsService = testApplication.getRdbmsService();

    final var followUpDate = NOW.plusDays(10).truncatedTo(ChronoUnit.SECONDS);

    final UserTaskDbModel randomizedUserTask =
        UserTaskFixtures.createRandomized(b -> b.followUpDate(followUpDate));
    createAndSaveUserTask(rdbmsService, randomizedUserTask);

    final var searchResult =
        rdbmsService
            .getUserTaskReader()
            .search(
                new UserTaskDbQuery(
                    new UserTaskFilter.Builder()
                        .followUpDateOperations(Operation.gt(followUpDate.minusDays(1)))
                        .build(),
                    UserTaskSort.of(b -> b),
                    SearchQueryPage.of(b -> b.from(0).size(10))));

    assertThat(searchResult.total()).isEqualTo(1);
    assertThat(searchResult.hits()).hasSize(1);
    assertUserTaskEntity(searchResult.hits().getFirst(), randomizedUserTask);
  }

  @TestTemplate
  public void shouldFindUserTaskByFollowUpDateLte(
      final CamundaRdbmsTestApplication testApplication) {
    final RdbmsService rdbmsService = testApplication.getRdbmsService();

    final var followUpDate = NOW.minusDays(12).truncatedTo(ChronoUnit.SECONDS);

    final UserTaskDbModel randomizedUserTask =
        UserTaskFixtures.createRandomized(b -> b.followUpDate(followUpDate));
    createAndSaveUserTask(rdbmsService, randomizedUserTask);

    final var searchResult =
        rdbmsService
            .getUserTaskReader()
            .search(
                new UserTaskDbQuery(
                    new UserTaskFilter.Builder()
                        .followUpDateOperations(Operation.lte(followUpDate))
                        .build(),
                    UserTaskSort.of(b -> b),
                    SearchQueryPage.of(b -> b.from(0).size(10))));

    assertThat(searchResult.total()).isEqualTo(1);
    assertThat(searchResult.hits()).hasSize(1);
    assertUserTaskEntity(searchResult.hits().getFirst(), randomizedUserTask);
  }

  @TestTemplate
  public void shouldFindUserTaskByCompletionDateEquals(
      final CamundaRdbmsTestApplication testApplication) {
    final RdbmsService rdbmsService = testApplication.getRdbmsService();

    final var completionDate = NOW.minusDays(16).truncatedTo(ChronoUnit.SECONDS);

    final UserTaskDbModel randomizedUserTask =
        UserTaskFixtures.createRandomized(b -> b.completionDate(completionDate));
    createAndSaveUserTask(rdbmsService, randomizedUserTask);

    final var searchResultGte =
        rdbmsService
            .getUserTaskReader()
            .search(
                new UserTaskDbQuery(
                    new UserTaskFilter.Builder()
                        .completionDateOperations(Operation.eq(completionDate))
                        .build(),
                    UserTaskSort.of(b -> b),
                    SearchQueryPage.of(b -> b.from(0).size(10))));

    assertThat(searchResultGte.total()).isEqualTo(1);
    assertThat(searchResultGte.hits()).hasSize(1);
    assertUserTaskEntity(searchResultGte.hits().getFirst(), randomizedUserTask);
  }

  @TestTemplate
  public void shouldFindUserTaskWithFullFilter(final CamundaRdbmsTestApplication testApplication) {
    final RdbmsService rdbmsService = testApplication.getRdbmsService();
    final UserTaskReader processInstanceReader = rdbmsService.getUserTaskReader();
    final OffsetDateTime completionDate = NOW.plusDays(1).truncatedTo(ChronoUnit.SECONDS);
    final OffsetDateTime dueDate = NOW.plusDays(5).truncatedTo(ChronoUnit.SECONDS);
    final OffsetDateTime followUpDate = NOW.plusDays(3).truncatedTo(ChronoUnit.SECONDS);

    final String processDefinitionId = UserTaskFixtures.nextStringId();
    createAndSaveRandomUserTasks(rdbmsService, processDefinitionId);
    final UserTaskDbModel randomizedUserTask =
        UserTaskFixtures.createRandomized(
            b ->
                b.processDefinitionId(processDefinitionId)
                    .completionDate(completionDate)
                    .dueDate(dueDate)
                    .followUpDate(followUpDate));
    createAndSaveUserTask(rdbmsService, randomizedUserTask);

    final var searchResult =
        processInstanceReader.search(
            new UserTaskDbQuery(
                new UserTaskFilter.Builder()
                    .userTaskKeys(randomizedUserTask.userTaskKey())
                    .elementIds(randomizedUserTask.elementId())
                    .assignees(randomizedUserTask.assignee())
                    .states(randomizedUserTask.state().name())
                    .processInstanceKeys(randomizedUserTask.processInstanceKey())
                    .processDefinitionKeys(randomizedUserTask.processDefinitionKey())
                    .candidateUserOperations(Operation.in(randomizedUserTask.candidateUsers()))
                    .candidateGroupOperations(Operation.in(randomizedUserTask.candidateGroups()))
                    .creationDateOperations(
                        Operation.lt(randomizedUserTask.creationDate().plusDays(1)))
                    .completionDateOperations(
                        Operation.gte(randomizedUserTask.completionDate().minusDays(2)))
                    .dueDateOperations(Operation.eq(randomizedUserTask.dueDate()))
                    .followUpDateOperations(
                        Operation.gte(randomizedUserTask.followUpDate().minusDays(1)))
                    .tenantIds(randomizedUserTask.tenantId())
                    .build(),
                UserTaskSort.of(b -> b),
                SearchQueryPage.of(b -> b.from(0).size(5))));

    assertThat(searchResult.total()).isEqualTo(1);
    assertThat(searchResult.hits()).hasSize(1);
    assertThat(searchResult.hits().getFirst().userTaskKey())
        .isEqualTo(randomizedUserTask.userTaskKey());
  }

  private static void assertUserTaskEntity(
      final UserTaskEntity instance, final UserTaskDbModel randomizedUserTask) {
    assertThat(instance).isNotNull();
    assertThat(instance)
        .usingRecursiveComparison()
        .ignoringFields(
            "customHeaders",
            "creationDate",
            "completionDate",
            "dueDate",
            "followUpDate",
            "candidateUsers",
            "candidateGroups")
        .isEqualTo(randomizedUserTask);
    assertThat(instance.creationDate())
        .isCloseTo(
            randomizedUserTask.creationDate(), new TemporalUnitWithinOffset(1, ChronoUnit.MILLIS));
    assertThat(instance.completionDate())
        .isCloseTo(
            randomizedUserTask.completionDate(),
            new TemporalUnitWithinOffset(1, ChronoUnit.MILLIS));
    assertThat(instance.dueDate())
        .isCloseTo(
            randomizedUserTask.dueDate(), new TemporalUnitWithinOffset(1, ChronoUnit.MILLIS));
    assertThat(instance.followUpDate())
        .isCloseTo(
            randomizedUserTask.followUpDate(), new TemporalUnitWithinOffset(1, ChronoUnit.MILLIS));

    if (randomizedUserTask.candidateUsers() != null) {
      assertThat(instance.candidateUsers())
          .containsExactlyInAnyOrderElementsOf(randomizedUserTask.candidateUsers());
    } else {
      assertThat(instance.candidateUsers()).isEmpty();
    }

    if (randomizedUserTask.candidateGroups() != null) {
      assertThat(instance.candidateGroups())
          .containsExactlyInAnyOrderElementsOf(randomizedUserTask.candidateGroups());
    } else {
      assertThat(instance.candidateGroups()).isEmpty();
    }
  }
}
