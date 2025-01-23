/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import React from 'react';
import {shallow} from 'enzyme';
import update from 'immutability-helper';

import {DownloadButton, HeatmapOverlay} from 'components';
import {formatters, loadRawData, getTooltipText, processResult} from 'services';

import Heatmap from './Heatmap';
import {calculateTargetValueHeat} from './service';

const {convertToMilliseconds} = formatters;

jest.mock('./service', () => {
  return {
    calculateTargetValueHeat: jest.fn(),
    getConfig: () => 'config',
  };
});

jest.mock('services', () => {
  const durationFct = jest.fn();
  const rest = jest.requireActual('services');
  return {
    ...rest,
    formatters: {
      duration: durationFct,
      convertToMilliseconds: jest.fn(),
      objectifyResult: jest.fn().mockReturnValue({a: 1, b: 2}),
      formatFileName: (name) => name,
    },
    loadRawData: jest.fn().mockReturnValue({result: {data: [{processInstanceId: 'test'}]}}),
    isDurationReport: jest.fn().mockReturnValue(false),
    getTooltipText: jest.fn(),
    processResult: jest.fn().mockReturnValue({data: {}}),
  };
});

const report = {
  name: 'test',
  reportType: 'process',
  data: {
    configuration: {
      xml: 'some diagram XML',
      aggregationTypes: [{type: 'avg', value: null}],
    },
    view: {
      properties: ['frequency'],
    },
    visualization: 'heat',
  },
  result: {
    measures: [
      {
        property: 'frequency',
        aggregationType: null,
        data: [
          {key: 'a', value: 1},
          {key: 'b', value: 2},
        ],
      },
    ],
    instanceCount: 5,
  },
};

const props = {
  report,
};

beforeEach(() => {
  processResult.mockClear();
});

it('should load the process definition xml', () => {
  const node = shallow(<Heatmap {...props} />);

  expect(node.find({xml: 'some diagram XML'})).toExist();
});

it('should load an updated process definition xml', () => {
  const node = shallow(<Heatmap {...props} />);

  node.setProps({report: {...report, data: {...report.data, configuration: {xml: 'another xml'}}}});

  expect(node.find({xml: 'another xml'})).toExist();
});

it('should display a loading indication while loading', () => {
  const node = shallow(
    <Heatmap {...props} report={{...report, data: {...report.data, configuration: {xml: null}}}} />
  );

  expect(node.find('Loading')).toExist();
});

it('should display an error message if visualization is incompatible with data', () => {
  const node = shallow(
    <Heatmap
      {...props}
      report={{
        ...report,
        result: {
          measures: [
            {
              property: 'frequency',
              data: 1234,
            },
          ],
        },
      }}
      errorMessage="Error"
    />
  );

  expect(node).toIncludeText('Error');
});

it('should display a diagram', () => {
  const node = shallow(<Heatmap {...props} />);

  expect(node).toIncludeText('Diagram');
});

it('should display a heatmap overlay', () => {
  const node = shallow(<Heatmap {...props} />);

  expect(node.find('HeatmapOverlay')).toExist();
});

it('should convert the data to target value heat when target value mode is active', () => {
  const heatmapTargetValue = {
    active: true,
    values: 'some values',
  };

  shallow(
    <Heatmap
      {...props}
      report={update(report, {
        data: {
          view: {properties: {$set: ['duration']}},
          configuration: {heatmapTargetValue: {$set: heatmapTargetValue}},
        },
        result: {measures: {0: {aggregationType: {$set: {type: 'avg', value: null}}}}},
      })}
    />
  );

  expect(calculateTargetValueHeat).toHaveBeenCalledWith(
    formatters.objectifyResult(report.result.data),
    heatmapTargetValue.values
  );
});

it('should show a tooltip with information about actual and target value', () => {
  const heatmapTargetValue = {
    active: true,
    values: {
      b: {value: 1, unit: 'millis'},
    },
  };

  calculateTargetValueHeat.mockReturnValue({b: 1});
  formatters.duration.mockReturnValueOnce('1ms').mockReturnValueOnce('2ms');
  convertToMilliseconds.mockReturnValue(1);

  const node = shallow(
    <Heatmap
      {...props}
      report={update(report, {
        data: {
          view: {properties: {$set: ['duration']}},
          configuration: {heatmapTargetValue: {$set: heatmapTargetValue}},
        },
        result: {measures: {0: {aggregationType: {$set: {type: 'avg', value: null}}}}},
      })}
    />
  );

  const tooltip = node.find('HeatmapOverlay').renderProp('formatter')('', 'b');

  expect(tooltip).toIncludeText('Target duration: 1ms');
  expect(tooltip).toIncludeText('Average duration: 2ms');
  expect(tooltip).toIncludeText('200% of the target value');
});

it('should inform if the actual value is less than 1% of the target value', () => {
  const heatmapTargetValue = {
    active: true,
    values: {
      b: {value: 10000, unit: 'millis'},
    },
  };

  calculateTargetValueHeat.mockReturnValue({b: 10000});
  formatters.duration.mockReturnValueOnce('10000ms').mockReturnValueOnce('1ms');
  convertToMilliseconds.mockReturnValue(10000);

  const node = shallow(
    <Heatmap
      {...props}
      report={update(report, {
        data: {
          view: {properties: {$set: ['duration']}},
          configuration: {heatmapTargetValue: {$set: heatmapTargetValue}},
        },
        result: {measures: {0: {aggregationType: {$set: {type: 'avg', value: null}}}}},
      })}
    />
  );

  const tooltip = node.find(HeatmapOverlay).renderProp('formatter')('', 'b');

  expect(tooltip).toIncludeText('< 1% of the target value');
});

it('should show a tooltip with information if no actual value is available', () => {
  const heatmapTargetValue = {
    active: true,
    values: {
      b: {value: 1, unit: 'millis'},
    },
  };

  calculateTargetValueHeat.mockReturnValue({b: undefined});
  formatters.duration.mockReturnValueOnce('1ms');
  convertToMilliseconds.mockReturnValue(1);
  formatters.objectifyResult.mockReturnValueOnce({});

  const node = shallow(
    <Heatmap
      {...props}
      report={{
        ...report,
        result: {
          measures: [
            {
              property: 'duration',
              data: [],
            },
          ],
          instanceCount: 5,
        },
        data: {
          ...report.data,
          configuration: {
            xml: 'test',
            heatmapTargetValue,
            aggregationTypes: [{type: 'avg', value: null}],
          },
        },
      }}
    />
  );

  const tooltip = node.find(HeatmapOverlay).renderProp('formatter')('', 'b');

  expect(tooltip).toIncludeText('No actual value available');
});

it('should invoke report evaluation when clicking the download instances button', async () => {
  const heatmapTargetValue = {
    active: true,
    values: {
      b: {value: 1, unit: 'millis'},
    },
  };

  formatters.duration.mockReturnValueOnce('1ms').mockReturnValueOnce('2ms');
  const node = shallow(
    <Heatmap
      {...props}
      report={update(report, {
        data: {
          view: {properties: {$set: ['duration']}},
          configuration: {heatmapTargetValue: {$set: heatmapTargetValue}},
        },
        result: {measures: {0: {aggregationType: {$set: {type: 'avg', value: null}}}}},
      })}
    />
  );

  const tooltip = node.find('HeatmapOverlay').renderProp('formatter')('', 'b');

  await tooltip.find(DownloadButton).prop('retriever')();

  expect(loadRawData).toHaveBeenCalledWith('config');
});

it('should hide to the download csv button in share mode', async () => {
  const heatmapTargetValue = {
    active: true,
    values: {
      b: {value: 1, unit: 'millis'},
    },
  };

  formatters.duration.mockReturnValueOnce('1ms').mockReturnValueOnce('2ms');
  const node = shallow(
    <Heatmap
      {...props}
      context="shared"
      report={update(report, {
        data: {
          view: {properties: {$set: ['duration']}},
          configuration: {heatmapTargetValue: {$set: heatmapTargetValue}},
        },
        result: {measures: {0: {aggregationType: {$set: {type: 'avg', value: null}}}}},
      })}
    />
  );

  const tooltip = node.find('HeatmapOverlay').renderProp('formatter')('', 'b');
  expect(tooltip.find(DownloadButton)).not.toExist();
});

describe('multi-measure reports', () => {
  const multiMeasureReport = update(report, {
    data: {view: {properties: {$set: ['frequency', 'duration']}}},
    result: {
      measures: {
        $push: [
          {
            property: 'duration',
            data: [
              {key: 'a', value: 1234},
              {key: 'b', value: 5678},
            ],
          },
        ],
      },
    },
  });

  it('should show a tooltip with information about multi-measure reports', () => {
    const node = shallow(<Heatmap {...props} report={multiMeasureReport} />);

    getTooltipText.mockReturnValueOnce('12');
    getTooltipText.mockReturnValueOnce('2d 15s');

    const tooltip = node.find('HeatmapOverlay').renderProp('formatter')('', 'b');

    expect(tooltip).toIncludeText('Count:12');
    expect(tooltip).toIncludeText('Duration:2d 15s');
  });

  it('should show tooltips in the short notation', () => {
    const multiMeasureReportPersistedTooltips = update(report, {
      data: {
        view: {properties: {$set: ['frequency', 'duration']}},
        configuration: {$merge: {alwaysShowAbsolute: true, alwaysShowRelative: true}},
      },
      result: {
        measures: {
          $push: [
            {
              property: 'duration',
              data: [
                {key: 'a', value: 1234},
                {key: 'b', value: 5678},
              ],
              aggregationType: {
                type: 'avg',
              },
            },
          ],
        },
      },
    });

    const node = shallow(<Heatmap {...props} report={multiMeasureReportPersistedTooltips} />);

    getTooltipText.mockReturnValueOnce('12');
    getTooltipText.mockReturnValueOnce('2d 15s');

    const tooltip = node.find('HeatmapOverlay').renderProp('formatter')('', 'b');

    expect(tooltip).toIncludeText('Count:12');
    expect(tooltip).toIncludeText('Avg:2d 15s');
  });

  it('should allow switching between heat visualizations for multi-measure reports', () => {
    const node = shallow(<Heatmap {...props} report={multiMeasureReport} />);

    expect(processResult).toHaveBeenCalledWith(
      update(multiMeasureReport, {result: {$set: multiMeasureReport.result.measures[0]}})
    );
    expect(processResult).not.toHaveBeenCalledWith(
      update(multiMeasureReport, {result: {$set: multiMeasureReport.result.measures[1]}})
    );

    node.find('Select').simulate('change', 1);

    expect(processResult).toHaveBeenCalledWith(
      update(multiMeasureReport, {result: {$set: multiMeasureReport.result.measures[1]}})
    );
  });
});
