/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import React from 'react';
import {shallow} from 'enzyme';

import {Report} from './Report';
import ReportEdit from './ReportEdit';
import ReportView from './ReportView';

import {loadEntity, evaluateReport} from 'services';
import {track} from 'tracking';

jest.mock('config', () => ({
  newReport: {new: {data: {configuration: {data: 'rest of configuration'}}}},
}));

jest.mock('services', () => {
  const rest = jest.requireActual('services');
  return {
    ...rest,
    evaluateReport: jest.fn(),
    loadEntity: jest.fn(),
  };
});

jest.mock('tracking', () => ({track: jest.fn()}));

const props = {
  match: {params: {id: '1'}},
  location: {},
  mightFail: (promise, cb) => cb(promise),
  getUser: () => ({id: 'demo'}),
};

const report = {
  id: 'reportID',
  name: 'name',
  description: 'description',
  lastModifier: 'lastModifier',
  lastModified: '2017-11-11T11:11:11.1111+0200',
  reportType: 'process',
  data: {
    processDefinitionKey: null,
    configuration: {},
    visualization: 'table',
  },
  result: {data: [1, 2, 3]},
};

loadEntity.mockReturnValue(report);
evaluateReport.mockReturnValue(report);

beforeEach(() => {
  jest.clearAllMocks();
});

it('should display a loading indicator', () => {
  const node = shallow(<Report {...props} mightFail={() => {}} />);

  expect(node.find('Loading')).toExist();
});

it("should show an error page if report doesn't exist", () => {
  const node = shallow(<Report {...props} />);
  node.setState({
    report: undefined,
    serverError: {status: 404, data: {errorMessage: 'testError'}},
  });

  expect(node.find('ErrorPage')).toExist();
});

it('should pass the error to report view and edit mode if evaluation fails', async () => {
  const testError = {status: 400, message: 'testError', reportDefinition: report};
  const mightFail = (_promise, _cb, err) => err(testError);

  const node = shallow(<Report {...props} mightFail={mightFail} />);
  await node.instance().loadReport();

  expect(node.find(ReportView).prop('error')).toEqual(testError);
});

it('should initially evaluate the report', () => {
  shallow(<Report {...props} />);

  expect(evaluateReport).toHaveBeenCalled();
  expect(track).toHaveBeenCalledWith('viewReport', {entityId: 'reportID'});
});

it('should not evaluate the report if it is new', () => {
  evaluateReport.mockClear();
  shallow(<Report {...props} match={{params: {id: 'new'}}} />);

  expect(evaluateReport).not.toHaveBeenCalled();
});

it('should apply templates from the location state', async () => {
  const node = await shallow(
    <Report
      {...props}
      match={{params: {id: 'new', viewMode: 'edit'}}}
      location={{
        state: {
          name: 'Template Report',
          description: 'description',
          data: {
            configuration: {xml: 'processXML'},
            processDefinitionKey: 'key',
            processDefinitionName: 'Definition Name',
            processDefinitionVersions: ['all'],
            tenantIds: [null, 'a'],
            view: {entity: 'flowNode', properties: ['frequency']},
            groupBy: {type: 'flowNodes', value: null},
            visualization: 'heat',
          },
        },
      }}
    />
  );

  const report = node.find(ReportEdit).prop('report');

  expect(report.name).toBe('Template Report');
  expect(report.description).toBe('description');
  expect(report.data).toMatchSnapshot();
});

it('should render ReportEdit component if viewMode is edit', () => {
  props.match.params.viewMode = 'edit';

  const node = shallow(<Report {...props} />);
  node.setState({loaded: true, report});

  expect(node.find(ReportEdit)).toExist();
});

it('should render ReportView component if viewMode is view', () => {
  props.match.params.viewMode = 'view';

  const node = shallow(<Report {...props} />);
  node.setState({loaded: true, report});

  expect(node.find(ReportView)).toExist();
});

it('should use the passed evaluation payload to evaluate the report if it exists', () => {
  const node = shallow(<Report {...props} />);

  expect(evaluateReport).toHaveBeenCalledWith('1', [], undefined);

  const passedReport = {id: '2'};
  node.find(ReportView).prop('loadReport')(undefined, passedReport);
  expect(evaluateReport).toHaveBeenCalledWith(passedReport, [], undefined);
});

it('should use the existing report in state to evaluate the report after loading it', () => {
  const node = shallow(<Report {...props} />);

  expect(evaluateReport).toHaveBeenCalledWith('1', [], undefined);

  node.find(ReportView).prop('loadReport')();
  expect(evaluateReport).toHaveBeenCalledWith(report, [], undefined);
});
