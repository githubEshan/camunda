/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.operate.webapp.security.identity;

import static io.camunda.operate.OperateProfileService.IDENTITY_AUTH_PROFILE;
import static io.camunda.operate.webapp.security.OperateURIs.*;
import static io.camunda.webapps.util.HttpUtils.REQUESTED_URL;

import io.camunda.identity.sdk.authentication.dto.AuthCodeDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@Profile(IDENTITY_AUTH_PROFILE)
public class IdentityController {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final IdentityService identityService;

  private final SecurityContextRepository securityContextRepository =
      new HttpSessionSecurityContextRepository();

  private final SecurityContextHolderStrategy securityContextHolderStrategy =
      SecurityContextHolder.getContextHolderStrategy();

  @Autowired
  public IdentityController(IdentityService identityService) {
    this.identityService = identityService;
  }

  /**
   * Initiates user login - the user will be redirected to Camunda Account
   *
   * @param req request
   * @return a redirect command to Camunda Account authorize url
   */
  @RequestMapping(
      value = LOGIN_RESOURCE,
      method = {RequestMethod.GET, RequestMethod.POST})
  public String login(final HttpServletRequest req) {
    final String authorizeUrl = identityService.getRedirectUrl(req);
    logger.debug("Redirect Login to {}", authorizeUrl);
    return "redirect:" + authorizeUrl;
  }

  /**
   * Logged in callback - Is called by Camunda Account with results of user authentication (GET)
   * <br>
   * Redirects to root url if successful, otherwise it will be redirected to an error url.
   *
   * @param req request
   * @param res response
   * @param authCodeDto authorize response
   * @throws IOException IO Exception
   */
  @GetMapping(value = IDENTITY_CALLBACK_URI)
  public void loggedInCallback(
      final HttpServletRequest req,
      final HttpServletResponse res,
      @RequestParam(required = false, name = "code") String code,
      @RequestParam(required = false, name = "state") String state,
      @RequestParam(required = false, name = "error") String error)
      throws IOException {
    final AuthCodeDto authCodeDto = new AuthCodeDto(code, state, error);
    logger.debug(
        "Called back by identity with {} {}, SessionId: {} and AuthCode {}",
        req.getRequestURI(),
        req.getQueryString(),
        req.getSession().getId(),
        authCodeDto.getCode());
    try {
      final IdentityAuthentication authentication =
          identityService.getAuthenticationFor(req, authCodeDto);

      final var context = securityContextHolderStrategy.createEmptyContext();
      context.setAuthentication(authentication);
      securityContextHolderStrategy.setContext(context);
      securityContextRepository.saveContext(context, req, res);

      redirectToPage(req, res);
    } catch (Exception iae) {
      clearContextAndRedirectToNoPermission(req, res, iae);
    }
  }

  private void redirectToPage(final HttpServletRequest req, final HttpServletResponse res)
      throws IOException {
    final Object originalRequestUrl = req.getSession().getAttribute(REQUESTED_URL);
    if (originalRequestUrl != null) {
      res.sendRedirect(req.getContextPath() + originalRequestUrl);
    } else {
      res.sendRedirect(req.getContextPath() + ROOT);
    }
  }

  /**
   * Is called when there was an in authentication or authorization
   *
   * @return no permissions error message
   */
  @RequestMapping(value = NO_PERMISSION)
  @ResponseBody
  public String noPermissions() {
    return "No permission for Operate - Please check your operate configuration or cloud configuration.";
  }

  /**
   * Logout - Invalidates session and logout from auth0, after that redirects to root url.
   *
   * @param req request
   * @param res response
   * @throws IOException exception
   */
  @RequestMapping(value = LOGOUT_RESOURCE)
  public void logout(HttpServletRequest req, HttpServletResponse res) throws IOException {
    logger.debug("logout user");
    try {
      identityService.logout();
    } catch (Exception e) {
      logger.error("An error occurred in logout process", e);
    }
    cleanup(req);
  }

  protected void clearContextAndRedirectToNoPermission(
      HttpServletRequest req, HttpServletResponse res, Throwable t) throws IOException {
    logger.error("Error in authentication callback: ", t);
    cleanup(req);
    res.sendRedirect(NO_PERMISSION);
  }

  protected void cleanup(HttpServletRequest req) {
    req.getSession().invalidate();

    final var context = securityContextHolderStrategy.getContext();
    if (context != null) {
      context.setAuthentication(null);
      securityContextHolderStrategy.clearContext();
    }
  }
}
