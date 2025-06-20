/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.gateway.rest;

import static io.camunda.zeebe.auth.Authorization.USER_TOKEN_CLAIMS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.camunda.authentication.CamundaJwtAuthenticationToken;
import io.camunda.authentication.entity.AuthenticationContext.AuthenticationContextBuilder;
import io.camunda.authentication.entity.CamundaJwtUser;
import io.camunda.authentication.entity.CamundaOidcUser;
import io.camunda.authentication.entity.CamundaUser.CamundaUserBuilder;
import io.camunda.authentication.entity.OAuthContext;
import io.camunda.service.ProcessInstanceServices.ProcessInstanceMigrateBatchOperationRequest;
import io.camunda.service.ProcessInstanceServices.ProcessInstanceModifyBatchOperationRequest;
import io.camunda.service.TenantServices.TenantDTO;
import io.camunda.zeebe.auth.Authorization;
import io.camunda.zeebe.gateway.protocol.rest.MigrateProcessInstanceMappingInstruction;
import io.camunda.zeebe.gateway.protocol.rest.ProcessInstanceFilter;
import io.camunda.zeebe.gateway.protocol.rest.ProcessInstanceMigrationBatchOperationPlan;
import io.camunda.zeebe.gateway.protocol.rest.ProcessInstanceMigrationBatchOperationRequest;
import io.camunda.zeebe.gateway.protocol.rest.ProcessInstanceModificationBatchOperationRequest;
import io.camunda.zeebe.gateway.protocol.rest.ProcessInstanceModificationMoveBatchOperationInstruction;
import io.camunda.zeebe.util.Either;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

class RequestMapperTest {

  @Mock private RequestAttributes requestAttributes;

  @BeforeEach
  void setup() throws Exception {
    MockitoAnnotations.openMocks(this).close();
    RequestContextHolder.setRequestAttributes(requestAttributes);
  }

  @Test
  void tokenContainsUsernameWithBasicAuth() {

    // given
    final var username = "username";
    setUsernamePasswordAuthenticationInContext(username);

    // when
    final var claims = RequestMapper.getAuthentication().claims();

    // then
    assertNotNull(claims);
    assertThat(claims).containsKey(Authorization.AUTHORIZED_USERNAME);
    assertThat(claims.get(Authorization.AUTHORIZED_USERNAME)).isEqualTo(username);
  }

  @Test
  void tokenContainsExtraClaimsWithOidcAuth() {
    // given
    final String usernameClaim = "sub";
    final String usernameValue = "test-user";
    setOidcAuthenticationInContext(usernameClaim, usernameValue, "aud1");

    // when
    final var authenticatedUsername = RequestMapper.getAuthentication().authenticatedUsername();

    // then
    assertThat(authenticatedUsername).isEqualTo(usernameValue);
  }

  @Test
  void tokenContainsExtraClaimsWithJwtAuth() {

    // given
    final String sub1 = "sub1";
    final String aud1 = "aud1";
    setJwtAuthenticationInContext(sub1, aud1);

    // when
    final var claims =
        (Map<String, Object>) RequestMapper.getAuthentication().claims().get(USER_TOKEN_CLAIMS);

    // then
    assertNotNull(claims);
    assertThat(claims).containsKey("sub");
    assertThat(claims).containsKey("aud");
    assertThat(claims).containsKey("groups");
    assertThat(claims.get("sub")).isEqualTo(sub1);
    assertThat(claims.get("aud")).isEqualTo(aud1);
    assertThat(claims.get("groups")).isEqualTo(List.of("g1", "g2"));
  }

  @Test
  void tokenContainsTenantIdsInAuthenticationContext() {
    // given
    final var username = "test-user";
    final var tenants =
        List.of(
            new TenantDTO(1L, "tenant-1", "Tenant One", "First"),
            new TenantDTO(2L, "tenant-2", "Tenant Two", "Second"));
    final var authenticationContext =
        new AuthenticationContextBuilder().withUsername(username).withTenants(tenants).build();

    final var principal =
        new CamundaOidcUser(
            new DefaultOidcUser(
                Collections.emptyList(),
                new OidcIdToken(
                    "tokenValue",
                    Instant.now(),
                    Instant.now().plusSeconds(3600),
                    Map.of("sub", username))),
            Collections.emptySet(),
            authenticationContext);

    final var auth = new OAuth2AuthenticationToken(principal, List.of(), "oidc");
    SecurityContextHolder.getContext().setAuthentication(auth);

    // when
    final var authContext = RequestMapper.getAuthentication();

    // then
    assertThat(authContext.authenticatedTenantIds())
        .containsExactlyInAnyOrder("tenant-1", "tenant-2");
    assertThat(authContext.authenticatedUsername()).isEqualTo(username);
  }

  @Test
  void usernameIsSetInAuthenticationWhenOnAuthenticationContext() {
    // given
    final var username = "test-user";
    final var authenticationContext =
        new AuthenticationContextBuilder().withUsername(username).build();

    final var principal =
        new CamundaOidcUser(
            new DefaultOidcUser(
                Collections.emptyList(),
                new OidcIdToken(
                    "tokenValue",
                    Instant.now(),
                    Instant.now().plusSeconds(3600),
                    Map.of("sub", username))),
            Collections.emptySet(),
            authenticationContext);

    final var auth = new OAuth2AuthenticationToken(principal, List.of(), "oidc");
    SecurityContextHolder.getContext().setAuthentication(auth);

    // when
    final var authentication = RequestMapper.getAuthentication();

    // then
    assertThat(authentication.authenticatedUsername()).isEqualTo(username);
    assertThat(authentication.authenticatedClientId()).isNull();
  }

  @Test
  void clientIdIsSetInAuthenticationWhenOnAuthenticationContext() {
    // given
    final var clientId = "my-application";
    final var authenticationContext =
        new AuthenticationContextBuilder().withClientId(clientId).build();

    final var principal =
        new CamundaOidcUser(
            new DefaultOidcUser(
                Collections.emptyList(),
                new OidcIdToken(
                    "tokenValue",
                    Instant.now(),
                    Instant.now().plusSeconds(3600),
                    Map.of("sub", clientId))),
            Collections.emptySet(),
            authenticationContext);

    final var auth = new OAuth2AuthenticationToken(principal, List.of(), "oidc");
    SecurityContextHolder.getContext().setAuthentication(auth);

    // when
    final var authContext = RequestMapper.getAuthentication();

    // then
    assertThat(authContext.authenticatedUsername()).isNull();
    assertThat(authContext.authenticatedClientId()).isEqualTo(clientId);
  }

  @Test
  void shouldMapToProcessInstanceMigrationBatchOperationRequest() {
    // given
    final var migrationPlan = new ProcessInstanceMigrationBatchOperationPlan();
    migrationPlan.setTargetProcessDefinitionKey("123");
    final var mappingInstruction = new MigrateProcessInstanceMappingInstruction();
    mappingInstruction.setSourceElementId("source1");
    mappingInstruction.setTargetElementId("target1");
    migrationPlan.setMappingInstructions(List.of(mappingInstruction));

    final var batchOperationInstruction = new ProcessInstanceMigrationBatchOperationRequest();
    batchOperationInstruction.setMigrationPlan(migrationPlan);
    final var filter = new ProcessInstanceFilter();
    batchOperationInstruction.setFilter(filter);

    // when
    final Either<ProblemDetail, ProcessInstanceMigrateBatchOperationRequest> result =
        RequestMapper.toProcessInstanceMigrationBatchOperationRequest(batchOperationInstruction);

    // then
    assertTrue(result.isRight());
    final var request = result.get();
    assertThat(request.targetProcessDefinitionKey()).isEqualTo(123L);
    assertThat(request.mappingInstructions())
        .hasSize(1)
        .first()
        .satisfies(
            instruction -> {
              assertThat(instruction.getSourceElementId()).isEqualTo("source1");
              assertThat(instruction.getTargetElementId()).isEqualTo("target1");
            });
  }

  @Test
  void shouldReturnProblemDetailForInvalidInput() {
    // given
    final var migrationPlan = new ProcessInstanceMigrationBatchOperationPlan();
    migrationPlan.setTargetProcessDefinitionKey("123");
    final var mappingInstruction = new MigrateProcessInstanceMappingInstruction();
    mappingInstruction.setSourceElementId(null);
    mappingInstruction.setTargetElementId(null);
    migrationPlan.setMappingInstructions(List.of(mappingInstruction));

    final var batchOperationRequest = new ProcessInstanceMigrationBatchOperationRequest();
    batchOperationRequest.setMigrationPlan(migrationPlan);
    final var filter = new ProcessInstanceFilter();
    batchOperationRequest.setFilter(filter);

    // when
    final Either<ProblemDetail, ProcessInstanceMigrateBatchOperationRequest> result =
        RequestMapper.toProcessInstanceMigrationBatchOperationRequest(batchOperationRequest);

    // then
    assertTrue(result.isLeft());
    final var problemDetail = result.getLeft();
    assertThat(problemDetail.getStatus()).isEqualTo(400); // Bad Request
    assertThat(problemDetail.getDetail()).contains("are required");
  }

  @Test
  void shouldMapProcessInstanceModifyBatchOperationRequest() {
    // given
    final var moveInstruction =
        new ProcessInstanceModificationMoveBatchOperationInstruction()
            .sourceElementId("source1")
            .targetElementId("target1");

    final var modificationRequest =
        new ProcessInstanceModificationBatchOperationRequest()
            .addMoveInstructionsItem(moveInstruction);

    // when
    final Either<ProblemDetail, ProcessInstanceModifyBatchOperationRequest> result =
        RequestMapper.toProcessInstanceModifyBatchOperationRequest(modificationRequest);

    // then
    assertTrue(result.isRight());
    final var request = result.get();
    assertThat(request.moveInstructions())
        .hasSize(1)
        .first()
        .satisfies(
            instruction -> {
              assertThat(instruction.getSourceElementId()).isEqualTo("source1");
              assertThat(instruction.getTargetElementId()).isEqualTo("target1");
            });
  }

  @Test
  void shouldNotMapProcessInstanceModifyBatchOperationRequestWhenInvalid() {
    // given
    final var moveInstruction =
        new ProcessInstanceModificationMoveBatchOperationInstruction().sourceElementId("source1");

    final var modificationRequest =
        new ProcessInstanceModificationBatchOperationRequest()
            .addMoveInstructionsItem(moveInstruction);

    // when
    final Either<ProblemDetail, ProcessInstanceModifyBatchOperationRequest> result =
        RequestMapper.toProcessInstanceModifyBatchOperationRequest(modificationRequest);

    // then
    assertTrue(result.isLeft());
    final var problemDetail = result.getLeft();
    assertThat(problemDetail.getStatus()).isEqualTo(400);
    assertThat(problemDetail.getDetail()).isEqualTo("No targetElementId provided.");
  }

  private void setJwtAuthenticationInContext(final String sub, final String aud) {
    final Jwt jwt =
        new Jwt(
            JWT.create()
                .withIssuer("issuer1")
                .withAudience(aud)
                .withSubject(sub)
                .sign(Algorithm.none()),
            Instant.ofEpochSecond(10),
            Instant.ofEpochSecond(100),
            Map.of("alg", "RSA256"),
            Map.of("sub", sub, "aud", aud, "groups", List.of("g1", "g2")));

    final AbstractAuthenticationToken token =
        new CamundaJwtAuthenticationToken(
            jwt,
            new CamundaJwtUser(
                jwt,
                new OAuthContext(
                    new HashSet<>(),
                    new AuthenticationContextBuilder()
                        .withUsername(sub)
                        .withGroups(List.of("g1", "g2"))
                        .build())),
            null,
            null);
    SecurityContextHolder.getContext().setAuthentication(token);
  }

  private void setOidcAuthenticationInContext(
      final String usernameClaim, final String usernameValue, final String aud) {
    final String tokenValue = "{}";
    final Instant tokenIssuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    final Instant tokenExpiresAt = tokenIssuedAt.plus(1, ChronoUnit.DAYS);

    final var oauth2Authentication =
        new OAuth2AuthenticationToken(
            new CamundaOidcUser(
                new DefaultOidcUser(
                    Collections.emptyList(),
                    new OidcIdToken(
                        tokenValue,
                        tokenIssuedAt,
                        tokenExpiresAt,
                        Map.of("aud", aud, usernameClaim, usernameValue))),
                Collections.emptySet(),
                new AuthenticationContextBuilder().withUsername(usernameValue).build()),
            List.of(),
            "oidc");

    SecurityContextHolder.getContext().setAuthentication(oauth2Authentication);
  }

  private void setUsernamePasswordAuthenticationInContext(final String username) {
    final UsernamePasswordAuthenticationToken authenticationToken =
        new UsernamePasswordAuthenticationToken(
            CamundaUserBuilder.aCamundaUser()
                .withUsername(username)
                .withPassword("admin")
                .withUserKey(1L)
                .build(),
            null);
    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
  }
}
