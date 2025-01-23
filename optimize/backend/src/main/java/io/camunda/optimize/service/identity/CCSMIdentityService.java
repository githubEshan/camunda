/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.identity;

import static io.camunda.optimize.rest.constants.RestConstants.OPTIMIZE_AUTHORIZATION;

import io.camunda.optimize.dto.optimize.GroupDto;
import io.camunda.optimize.dto.optimize.IdentityDto;
import io.camunda.optimize.dto.optimize.IdentityWithMetadataResponseDto;
import io.camunda.optimize.dto.optimize.UserDto;
import io.camunda.optimize.dto.optimize.query.IdentitySearchResultResponseDto;
import io.camunda.optimize.service.security.CCSMTokenService;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import io.camunda.optimize.service.util.configuration.condition.CCSMCondition;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Component
@Conditional(CCSMCondition.class)
public class CCSMIdentityService extends AbstractIdentityService {

  private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(CCSMIdentityService.class);
  private final CCSMTokenService ccsmTokenService;
  private final CCSMUserCache userCache;

  public CCSMIdentityService(
      final ConfigurationService configurationService,
      final CCSMTokenService ccsmTokenService,
      final CCSMUserCache userCache) {
    super(configurationService);
    this.ccsmTokenService = ccsmTokenService;
    this.userCache = userCache;
  }

  @Override
  public Optional<UserDto> getUserById(final String userId) {
    return userCache.getUserById(userId);
  }

  @Override
  public Optional<UserDto> getCurrentUserById(
      final String userId, final HttpServletRequest request) {
    return Optional.ofNullable(request.getCookies())
        .flatMap(
            cookies -> {
              final Cookie authorizationCookie =
                  Arrays.stream(request.getCookies())
                      .filter(cookie -> OPTIMIZE_AUTHORIZATION.equals(cookie.getName()))
                      .findAny()
                      .orElse(null);
              return Optional.ofNullable(authorizationCookie)
                  .map(
                      cookie ->
                          ccsmTokenService.getUserInfoFromToken(
                              userId, authorizationCookie.getValue()));
            });
  }

  @Override
  public Optional<GroupDto> getGroupById(final String groupId) {
    // Groups do not exist in SaaS
    return Optional.empty();
  }

  @Override
  public List<GroupDto> getAllGroupsOfUser(final String userId) {
    return Collections.emptyList();
  }

  @Override
  public boolean isUserAuthorizedToAccessIdentity(final String userId, final IdentityDto identity) {
    // Identity permissions are handled by identity on each users() request where we supply the
    // access token of the current user.
    // Note "accessing identity" here means "accessing info about the other user/group"
    return true;
  }

  @Override
  public IdentitySearchResultResponseDto searchForIdentitiesAsUser(
      final String userId,
      final String searchString,
      final int maxResults,
      final boolean excludeUserGroups) {
    return new IdentitySearchResultResponseDto(
        userCache.searchForIdentityUsingSearchTerm(searchString, maxResults).stream()
            .map(IdentityWithMetadataResponseDto.class::cast)
            .toList());
  }

  @Override
  public List<IdentityWithMetadataResponseDto> getUsersById(final Set<String> userIds) {
    return userCache.getUsersById(userIds).stream()
        .map(IdentityWithMetadataResponseDto.class::cast)
        .toList();
  }

  @Override
  public List<IdentityWithMetadataResponseDto> getGroupsById(final Set<String> groupIds) {
    // Groups do not exist in CCSM
    return Collections.emptyList();
  }

  public List<UserDto> getUsersByEmail(final Set<String> emails) {
    return userCache.searchForUsersUsingEmails(emails).stream().map(UserDto.class::cast).toList();
  }
}
