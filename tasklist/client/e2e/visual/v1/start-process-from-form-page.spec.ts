/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {expect} from '@playwright/test';
import subscribeFormSchema from '@/resources/subscribeForm.json' assert {type: 'json'};
import {test} from '@/fixtures/v1-visual';
import {URL_API_V1_PATTERN} from '@/constants';
import {mockFormResponses} from '@/mocks/v1/forms';

test.describe('start process from form page', () => {
  test('initial page', async ({page}) => {
    await page.route(URL_API_V1_PATTERN, mockFormResponses());

    await page.goto('/new/foo', {
      waitUntil: 'networkidle',
    });

    await expect(
      page.getByRole('heading', {
        name: 'Subscribe to newsletter',
      }),
    ).toBeVisible();
    await expect(page.getByLabel('Name')).toBeVisible();

    await expect(page).toHaveScreenshot();
  });

  test('process not found', async ({page}) => {
    await page.route(
      URL_API_V1_PATTERN,
      mockFormResponses({
        formStatus: 404,
      }),
    );

    await page.goto('/new/foo', {
      waitUntil: 'networkidle',
    });

    await expect(page.getByTestId('public-form-skeleton')).not.toBeVisible({
      timeout: 10000,
    });
    await expect(
      page.getByRole('heading', {
        name: '404 - Page not found',
      }),
    ).toBeVisible();

    await expect(page).toHaveScreenshot();
  });

  test('invalid schema', async ({page}) => {
    await page.route(
      URL_API_V1_PATTERN,
      mockFormResponses({
        schema: `${JSON.stringify(subscribeFormSchema)}invalidschema`,
      }),
    );

    await page.goto('/new/foo', {
      waitUntil: 'networkidle',
    });

    await expect(
      page.getByRole('heading', {
        name: 'Invalid form',
      }),
    ).toBeVisible();

    await expect(page).toHaveScreenshot();
  });

  test('successful submission', async ({page}) => {
    await page.route(URL_API_V1_PATTERN, mockFormResponses());

    await page.goto('/new/foo', {
      waitUntil: 'networkidle',
    });

    await page.getByLabel('Name').fill('Joe Doe');
    await page.getByLabel('Email').fill('joe@doe.com');
    await page.getByRole('button', {name: 'Submit'}).click();

    await expect(
      page.getByRole('heading', {
        name: 'Success!',
      }),
    ).toBeVisible();

    await expect(page).toHaveScreenshot();
  });

  test('failed submission', async ({page}) => {
    await page.route(
      URL_API_V1_PATTERN,
      mockFormResponses({
        startStatus: 500,
      }),
    );

    await page.goto('/new/foo', {
      waitUntil: 'networkidle',
    });

    await page.getByLabel('Name').fill('Joe Doe');
    await page.getByLabel('Email').fill('joe@doe.com');
    await page.getByRole('button', {name: 'Submit'}).click();

    await expect(
      page.getByRole('heading', {
        name: 'Something went wrong',
      }),
    ).toBeVisible();

    await expect(page).toHaveScreenshot();
  });
});
