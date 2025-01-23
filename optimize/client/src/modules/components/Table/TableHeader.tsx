/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {MouseEvent} from 'react';
import {TableHeader as CarbonTableHeader, DataTableSortState, TableSelectAll} from '@carbon/react';
import classNames from 'classnames';

import {isReactElement} from 'services';

import {Header} from './Table';
import {rewriteHeaderStyles} from './service';

interface TableHeaderProps {
  header: Header;
  allowLocalSorting?: boolean;
  updateSorting?: (columneName: string | undefined, sorting: 'asc' | 'desc') => void;
  sorting?: {by: string; order: string};
  isSortable: boolean;
  resultType?: string;
  sortByLabel?: boolean;
  firstColumnId?: string;
}

export default function TableHeader({
  header,
  isSortable,
  allowLocalSorting,
  firstColumnId,
  resultType,
  sortByLabel,
  sorting,
  updateSorting,
}: TableHeaderProps) {
  if (
    typeof header.Header === 'object' &&
    isReactElement(header.Header) &&
    header.Header?.type === TableSelectAll
  ) {
    return header.render('Header');
  }

  function getSortingProps(header: Header): Record<string, unknown> {
    if (!updateSorting && !allowLocalSorting) {
      return {};
    }
    const props = header.getSortByToggleProps();
    return {
      ...props,
      isSortable: isSortable && header.canSort,
      isSortHeader: header.isSorted,
      sortDirection: getSortingDirection(header, sorting),
      onClick: (evt: MouseEvent) => {
        // dont do anything when clicker on column rearrangement or resizing
        if (
          evt?.target instanceof HTMLElement &&
          (evt.target.classList.contains('::after') || evt.target.classList.contains('resizer'))
        ) {
          return;
        }

        if (props.onClick) {
          props.onClick(evt);
          let sortColumn = header.id;
          if (resultType === 'map') {
            if (sortColumn === firstColumnId) {
              sortColumn = sortByLabel ? 'label' : 'key';
            } else {
              sortColumn = 'value';
            }
          }
          updateSorting?.(sortColumn, sorting?.order === 'asc' ? 'desc' : 'asc');
        }
      },
    };
  }

  function getSortingDirection(
    header: Header,
    sorting?: {by: string; order: string}
  ): DataTableSortState {
    if (sorting?.order) {
      return sorting.order === 'desc' ? 'DESC' : 'ASC';
    }
    if (!header.isSorted) {
      return 'NONE';
    }
    if (header.isSortedDesc) {
      return 'DESC';
    }
    return 'ASC';
  }

  const {role, ...reactTableHeaderProps} = header.getHeaderProps();

  const {key, ...headerProps} = {
    ...getSortingProps(header),
    ...reactTableHeaderProps,
    className: classNames('tableHeader', {placeholder: header.placeholderOf}),
    title: getTableHeaderTitle(header),
    'data-group': header.group,
  };

  return (
    <CarbonTableHeader key={key} {...headerProps} ref={rewriteHeaderStyles(headerProps.style)}>
      <span className="text">{header.render('Header')}</span>
      <div {...header.getResizerProps()} className="resizer" />
    </CarbonTableHeader>
  );
}

function getTableHeaderTitle(header: Header): string {
  let title = header.title || header.id;
  if (typeof header.Header === 'string') {
    title = header.Header;
  }
  return title;
}
