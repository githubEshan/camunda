/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import React, {runAllEffects} from 'react';
import {Button} from '@carbon/react';
import {shallow} from 'enzyme';
import update from 'immutability-helper';

import RawDataModal from './RawDataModal';
import DiagramModal from './DiagramModal';
import {loadTenants} from './service';

import {SingleReportDetails} from './SingleReportDetails';

jest.mock('config', () => ({getOptimizeProfile: jest.fn().mockReturnValue('ccsm')}));

jest.mock('./service', () => ({
  loadTenants: jest.fn().mockReturnValue([
    {
      key: 'aKey',
      versions: ['2', '1'],
      tenants: [
        {id: 'sales', name: 'Sales'},
        {id: 'consulting', name: 'Consulting'},
        {id: 'snacks', name: 'Snacks'},
      ],
    },
  ]),
}));

const props = {
  report: {
    data: {
      definitions: [
        {
          key: 'aKey',
          name: 'aName',
          displayName: 'aName',
          versions: ['2', '1'],
          tenantIds: ['sales', 'consulting'],
        },
      ],
      view: {properties: ['frequency'], entity: 'processInstance'},
      groupBy: {type: 'startDate', value: {unit: 'automatic'}},
    },
    name: 'Report Name',
    reportType: 'process',
    owner: 'Test Person',
    lastModified: '2020-06-23T09:32:48.938+0200',
    lastModifier: 'Test Person',
  },
  mightFail: jest.fn().mockImplementation((data, cb) => cb(data)),
  location: {pathname: '/report/1'},
};

beforeEach(() => {
  loadTenants.mockClear();
});

it('should show relevant information', () => {
  const node = shallow(<SingleReportDetails {...props} />);
  runAllEffects();

  expect(node).toIncludeText('aName');
  expect(node).toIncludeText('2, 1');
  expect(node).toIncludeText('Sales, Consulting');
  expect(node).toIncludeText('Process instance Count by Start date');
  expect(node).not.toIncludeText('Report Name');
});

it('should show the report name if it has showReportName prop', () => {
  const node = shallow(<SingleReportDetails {...props} showReportName />);
  runAllEffects();

  expect(node).toIncludeText('Report Name');
});

it('should not show tenant section if there is only one tenant in C7', () => {
  const oneTenant = update(props.report, {data: {tenantIds: {$set: ['sales']}}});
  loadTenants.mockReturnValueOnce([{id: 'sales', name: 'Sales'}]);

  const node = shallow(<SingleReportDetails {...props} report={oneTenant} />);
  runAllEffects();

  expect(node).not.toIncludeText('Sales');
  expect(node).not.toIncludeText('Tenant');
});

it('should show the only tenant in self managed', async () => {
  const oneTenant = update(props.report, {data: {tenantIds: {$set: ['sales']}}});
  loadTenants.mockReturnValueOnce([
    {
      key: 'aKey',
      versions: ['2', '1'],
      tenants: [{id: 'sales', name: 'Sales'}],
    },
  ]);
  const node = shallow(<SingleReportDetails {...props} report={oneTenant} />);

  await runAllEffects();

  expect(node.find('.info').at(1).text()).toBe('Tenant: Sales');
});

it('should not show tenant section and process model/raw data buttons on share pages', () => {
  const node = shallow(
    <SingleReportDetails {...props} location={{pathname: '/share/report/abc'}} />
  );
  runAllEffects();

  expect(loadTenants).not.toHaveBeenCalled();
  expect(node).not.toIncludeText('Sales');
  expect(node).not.toIncludeText('Tenant');
  expect(node.find('.modalButton')).not.toExist();
});

it('should have special handling for variable views', () => {
  const variableReport = update(props.report, {
    data: {
      view: {$set: {entity: 'variable', properties: [{name: 'x', type: 'Integer'}]}},
      groupBy: {$set: {type: 'none', value: null}},
    },
  });

  const node = shallow(<SingleReportDetails {...props} report={variableReport} />);
  runAllEffects();

  expect(node).toIncludeText('Variable x');
  expect(node.find('h4.nowrap')).toExist();
});

it('should open raw data modal when button is clicked', () => {
  const node = shallow(<SingleReportDetails {...props} />);
  runAllEffects();

  node.find('.rawDataButton').simulate('click');

  expect(node.find(RawDataModal)).toExist();
});

it('should open diagram modal when button is clicked', () => {
  const node = shallow(<SingleReportDetails {...props} />);
  runAllEffects();

  node.find('.definition').find(Button).simulate('click');

  expect(node.find(DiagramModal)).toExist();
});
