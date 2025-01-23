/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import React, {runLastEffect, runAllEffects} from 'react';
import {shallow} from 'enzyme';
import {MenuItem} from '@carbon/react';

import {showPrompt} from 'prompt';
import {useUiConfig} from 'hooks';

import {getVariableNames} from './service';

import {AddFiltersButton} from './AddFiltersButton';

jest.mock('hooks', () => ({
  useUiConfig: jest.fn().mockReturnValue({userTaskAssigneeAnalyticsEnabled: true}),
  useErrorHandling: () => ({
    mightFail: jest.fn().mockImplementation((data, cb) => cb(data)),
  }),
}));

const props = {
  availableFilters: [],
  setAvailableFilters: jest.fn(),
  mightFail: jest.fn().mockImplementation((data, cb) => cb(data)),
  reports: [{id: 'reportId'}],
  persistReports: jest.fn(),
};

jest.mock('prompt', () => ({
  showPrompt: jest.fn().mockImplementation(async (_config, cb) => await cb()),
}));

jest.mock('./service', () => ({
  getVariableNames: jest.fn(),
}));

beforeEach(() => {
  props.setAvailableFilters.mockClear();
  props.persistReports.mockClear();
  getVariableNames.mockClear();
});

it('should not allow adding the same filter twice', () => {
  const node = shallow(
    <AddFiltersButton {...props} availableFilters={[{type: 'instanceStartDate'}]} />
  );

  expect(node.find(MenuItem).at(0)).toBeDisabled();
});

it('should disable options that rely on process data if there are no reports', () => {
  const node = shallow(<AddFiltersButton {...props} />);

  expect(node.find(MenuItem).last()).not.toBeDisabled();
  node.setProps({reports: []});
  expect(node.find(MenuItem).last()).toBeDisabled();
});

it('should show a prompt to save the dashboard when adding filters that rely on processes on a dashboard with unsaved reports', async () => {
  const node = shallow(
    <AddFiltersButton {...props} reports={[{id: 'reportId'}, {report: {name: 'unsaved report'}}]} />
  );

  node.find(MenuItem).last().simulate('click');

  await flushPromises();

  expect(showPrompt).toHaveBeenCalledTimes(1);
  expect(props.persistReports).toHaveBeenCalledTimes(1);
});

it('should fetch variable names', () => {
  shallow(<AddFiltersButton {...props} />);

  runLastEffect();

  expect(getVariableNames).toHaveBeenCalledWith(['reportId']);
});

it('should remove filters that are no longer valid', () => {
  getVariableNames.mockReturnValue([{type: 'String', name: 'a'}]);

  shallow(
    <AddFiltersButton
      {...props}
      availableFilters={[
        {type: 'variable', data: {name: 'a', type: 'String'}},
        {type: 'variable', data: {name: 'b', type: 'Boolean'}},
      ]}
    />
  );

  runLastEffect();

  expect(props.setAvailableFilters).toHaveBeenCalledWith([
    {type: 'variable', data: {name: 'a', type: 'String'}},
  ]);
});

it('should include the allowed values for string and number variables', () => {
  const node = shallow(<AddFiltersButton {...props} />);

  node
    .find(MenuItem)
    .findWhere((n) => n.prop('label') === 'Variable')
    .first()
    .simulate('click');

  const modal = node.find('.dashboardVariableFilter');

  expect(modal).toExist();

  const newFilter = {
    type: 'variable',
    data: {
      type: 'String',
      name: 'stringVar',
      data: {operator: 'in', values: ['aStringValue'], allowCustomValues: false},
    },
  };

  modal.prop('addFilter')(newFilter);
  expect(props.setAvailableFilters).toHaveBeenCalledWith([newFilter]);
});

it('should not include a data field for boolean and date variables', () => {
  const node = shallow(<AddFiltersButton {...props} />);

  node
    .find(MenuItem)
    .findWhere((n) => n.prop('label') === 'Variable')
    .first()
    .simulate('click');

  const modal = node.find('.dashboardVariableFilter');

  modal.prop('addFilter')({
    type: 'variable',
    data: {type: 'Boolean', name: 'newVar', data: {value: true}},
  });
  expect(props.setAvailableFilters).toHaveBeenCalledWith([
    {
      type: 'variable',
      data: {
        type: 'Boolean',
        name: 'newVar',
      },
    },
  ]);

  modal.prop('addFilter')({
    type: 'variable',
    data: {
      type: 'Date',
      name: 'newVar',
      data: {type: 'relative', start: {value: 1, unit: 'years'}, end: null},
    },
  });
  expect(props.setAvailableFilters).toHaveBeenCalledWith([
    {
      type: 'variable',
      data: {
        type: 'Date',
        name: 'newVar',
      },
    },
  ]);
});

it('should include a checkbox to allow custom values', () => {
  const node = shallow(<AddFiltersButton {...props} />);

  node
    .find(MenuItem)
    .findWhere((n) => n.prop('label') === 'Variable')
    .first()
    .simulate('click');

  const postText = shallow(
    node.find('.dashboardVariableFilter').prop('getPosttext')({type: 'String'})
  );

  expect(postText.find('[type="checkbox"]')).toExist();
});

it('should show an assignee filter modal with additional content', async () => {
  const node = shallow(<AddFiltersButton {...props} />);

  await runAllEffects();

  node
    .find(MenuItem)
    .findWhere((n) => n.prop('label') === 'Assignee')
    .first()
    .simulate('click');

  expect(node.find('.dashboardAssigneeFilter')).toExist();

  const postText = shallow(
    node.find('.dashboardAssigneeFilter').prop('getPosttext')({type: 'String'})
  );
  expect(postText.find('[type="checkbox"]')).toExist();
});

it('should not show assignee options when assignee analytics are disabled', async () => {
  useUiConfig.mockImplementation(() => ({
    userTaskAssigneeAnalyticsEnabled: false,
  }));
  const node = shallow(<AddFiltersButton {...props} />);

  await runAllEffects();

  expect(node.find(MenuItem).findWhere((n) => n.prop('label') === 'Assignee').length).toBe(0);
});
