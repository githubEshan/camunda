/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {Component} from 'react';
import Viewer from 'bpmn-js/lib/NavigatedViewer';
import {Button, InlineNotification} from '@carbon/react';

import {
  Modal,
  BPMNDiagram,
  TargetValueBadge,
  Loading,
  ClickBehavior,
  RegistryElement,
} from 'components';
import {t} from 'translation';
import {loadProcessDefinitionXml} from 'services';
import {WithErrorHandlingProps, withErrorHandling} from 'HOC';
import {showError} from 'notifications';
import {Definition, FilterData} from 'types';

import FilterSingleDefinitionSelection from '../FilterSingleDefinitionSelection';
import {FilterProps} from '../types';

import {isValidInput} from './service';
import NodesTable from './NodesTable';

import './NodeDuration.scss';

interface NodeDurationState {
  focus: string | null;
  values: Record<string, FilterData>;
  nodeNames: Record<string, string>;
  loading: boolean;
  applyTo?: Definition | null;
  xml: string | null;
}

export class NodeDuration extends Component<
  FilterProps<'flowNodeDuration'> & WithErrorHandlingProps,
  NodeDurationState
> {
  state: NodeDurationState = {
    focus: null,
    values: {},
    nodeNames: {},
    loading: false,
    applyTo: null,
    xml: null,
  };

  async componentDidMount() {
    this.setState({loading: true});

    const validDefinitions = this.props.definitions.filter(
      (definition) => definition.versions?.length && definition.tenantIds?.length
    );

    const applyTo =
      validDefinitions.find(({identifier}) => this.props.filterData?.appliedTo[0] === identifier) ||
      validDefinitions[0];

    const {
      values = {},
      nodeNames = {},
      xml = null,
    } = (await this.constructValues(applyTo, this.props.filterData?.data)) || {};
    this.setState({
      focus: null,
      values,
      nodeNames,
      loading: false,
      applyTo,
      xml,
    });
  }

  async componentDidUpdate(
    _prevProps: FilterProps<'flowNodeDuration'>,
    prevState: NodeDurationState
  ) {
    if (prevState.applyTo && prevState.applyTo !== this.state.applyTo) {
      this.setState({loading: true});

      const {
        values = {},
        nodeNames = {},
        xml = null,
      } = (await this.constructValues(this.state.applyTo)) || {};
      this.setState({
        focus: null,
        values,
        nodeNames,
        loading: false,
        xml,
      });
    }
  }

  constructValues = (
    definition: Definition | null = {identifier: ''},
    predefinedValues: Record<string, FilterData> = {}
  ) => {
    const {key, tenantIds, versions} = definition || {};
    return this.props.mightFail(
      loadProcessDefinitionXml(key, versions?.[0], tenantIds?.[0]),
      async (xml) => {
        if (!xml) {
          return;
        }

        const viewer = new Viewer();
        await viewer.importXML(xml);
        const values: Record<string, FilterData> = {};
        const nodeNames: Record<string, string> = {};

        const set = new Set<RegistryElement>();
        viewer
          .get<RegistryElement[]>('elementRegistry')
          .filter((element) => element.businessObject.$instanceOf('bpmn:FlowNode'))
          .map((element) => element.businessObject)
          .forEach((element) => set.add(element));

        set.forEach((element) => {
          values[element.id] = copyObjectIfExistsAndStringifyValue(
            predefinedValues[element.id]
          ) as FilterData;
          nodeNames[element.id] = element.name || element.id;
        });

        return {values, nodeNames, xml};
      },
      showError
    );
  };

  cleanUpValues = () => {
    // this function removes all entries without value and converts values into numbers
    const values: Record<string, FilterData> = {};

    Object.keys(this.state.values).forEach((key) => {
      const entry = this.state.values[key];
      if (entry && entry.value.toString().trim()) {
        values[key] = {
          operator: entry.operator,
          value: parseFloat(entry.value.toString()),
          unit: entry.unit,
        };
      }
    });

    return values;
  };

  validChanges = () => {
    return this.hasSomethingChanged() && this.areAllFieldsNumbers();
  };

  hasSomethingChanged = () => {
    const prev = this.props.filterData?.data || {};
    const now = this.cleanUpValues();

    return JSON.stringify(prev) !== JSON.stringify(now);
  };

  areAllFieldsNumbers = () => {
    return Object.keys(this.state.values)
      .filter((key) => this.state.values[key])
      .every((key) => {
        const entry = this.state.values[key];
        const value = entry && entry.value;
        return isValidInput(value);
      });
  };

  updateFocus = (focus: string | null) => this.setState({focus});

  confirmModal = () => {
    const data = this.cleanUpValues();
    this.props.addFilter({
      type: 'flowNodeDuration',
      data,
      appliedTo: this.state.applyTo ? [this.state.applyTo.identifier] : [],
    });
  };

  render() {
    const {close, filterData, definitions} = this.props;
    const {loading, focus, nodeNames, values, applyTo, xml} = this.state;

    const activeNodes = Object.keys(this.cleanUpValues());
    const empty = activeNodes.length === 0;
    if (focus) {
      activeNodes.push(focus);
    }

    return (
      <Modal size="lg" open onClose={close} className="NodeDuration">
        <Modal.Header title={t('common.filter.types.flowNodeDuration')} />
        <Modal.Content className="contentContainer">
          <FilterSingleDefinitionSelection
            availableDefinitions={definitions}
            applyTo={applyTo}
            setApplyTo={(applyTo) => this.setState({applyTo})}
          />
          {loading ? (
            <Loading />
          ) : (
            <>
              <BPMNDiagram xml={xml}>
                <ClickBehavior
                  onClick={({id}) => this.updateFocus(id)}
                  selectedNodes={activeNodes}
                />
                <TargetValueBadge values={values} />
              </BPMNDiagram>
              <NodesTable
                focus={focus}
                updateFocus={this.updateFocus}
                nodeNames={nodeNames}
                values={values}
                onChange={(values) => this.setState({values})}
              />
              {!this.areAllFieldsNumbers() && (
                <InlineNotification
                  kind="error"
                  hideCloseButton
                  subtitle={t('report.heatTarget.invalidValue').toString()}
                />
              )}
            </>
          )}
        </Modal.Content>
        <Modal.Footer>
          <Button kind="secondary" className="cancel" onClick={close}>
            {t('common.cancel')}
          </Button>
          <Button
            onClick={this.confirmModal}
            className="confirm"
            disabled={empty || !this.validChanges()}
          >
            {filterData ? t('common.filter.updateFilter') : t('common.filter.addFilter')}
          </Button>
        </Modal.Footer>
      </Modal>
    );
  }
}

function copyObjectIfExistsAndStringifyValue<T extends {value: unknown}>(obj?: T) {
  if (obj) {
    return {
      ...obj,
      value: '' + obj.value,
    };
  }
  return obj;
}

export default withErrorHandling(NodeDuration);
