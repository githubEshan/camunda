/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

type Variable = {
  id: string;
  name: string;
  previewValue: string;
  value: string;
  isValueTruncated: boolean;
};

const emptyVariables: Variable[] = [];

const variables: Variable[] = [
  {
    id: '2251799813686711-small',
    name: 'small',
    previewValue: '"Hello World"',
    value: '"Hello World"',
    isValueTruncated: false,
  },
];

export {emptyVariables, variables};
export type {Variable};
