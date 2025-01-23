/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {shallow} from 'enzyme';

import {ErrorResponse} from 'request';

import {default as Notifications, addNotification, showError} from './Notifications';
import {Config} from './Notification';

it('should expose a global addNotifications method', () => {
  const node = shallow(<Notifications />);

  addNotification('Sample Notification');

  expect(node.find('Notification')).toExist();
  expect(node.find('Notification').prop<Config>('config').text).toBe('Sample Notification');
});

it('should process and show an error notification', async () => {
  const node = shallow(<Notifications />);

  await showError({message: 'Error content'} as ErrorResponse);

  expect(node.find('Notification')).toExist();
  expect(node.find('Notification').prop<Config>('config').text).toBe('Error content');
});

it('should accept string error', async () => {
  const node = shallow(<Notifications />);

  await showError('Error content');

  expect(node.find('Notification').prop<Config>('config').text).toBe('Error content');
});

it('should accept React element error', async () => {
  const node = shallow(<Notifications />);

  await showError(<h1>test</h1>);

  expect(node.find('Notification').dive().find('h1')).toExist();
});
