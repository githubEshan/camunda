/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {Select} from 'components';
import {shallow} from 'enzyme';

import {createReportUpdate} from 'services';

import Sorting from './Sorting';

jest.mock('services', () => {
  const rest = jest.requireActual('services');

  return {
    ...rest,
    createReportUpdate: jest.fn((_b, type, value) => ({[type]: value})),
  };
});

const config = {
  report: {
    view: {},
    visualization: 'bar',
    groupBy: {type: 'startDate'},
    distributedBy: {type: 'none'},
    configuration: {
      sorting: null,
    },
  },
  onChange: jest.fn(),
};

beforeEach(() => {
  createReportUpdate.mockClear();
});

it('should show soritng options for bar/line/barLine chart grouped by date', () => {
  const node = shallow(<Sorting {...config} />);

  expect(node.find('.sortingOrder')).toExist();
});

it('should not show soritng options for other chart type', () => {
  const node = shallow(<Sorting {...config} report={{...config.report, visualization: 'table'}} />);

  expect(node.find('.sortingOrder')).not.toExist();
});

it('should not show soritng options when there is second sorting', () => {
  const node = shallow(
    <Sorting {...config} report={{...config.report, distributedBy: {type: 'flowNode'}}} />
  );

  expect(node.find('.sortingOrder')).not.toExist();
});

it('should invoke onChange when sorting order is changed', () => {
  const node = shallow(<Sorting {...config} />);

  node.find(Select).simulate('change', 'asc');

  expect(config.onChange).toHaveBeenCalledWith({sortingOrder: 'asc'});
});
