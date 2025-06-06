/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {
  render,
  screen,
  waitForElementToBeRemoved,
  within,
} from 'common/testing/testing-library';
import {AvailableTasks} from './index';
import {Link, MemoryRouter} from 'react-router-dom';
import {nodeMockServer} from 'common/testing/nodeMockServer';
import {http, HttpResponse} from 'msw';
import * as tasksMocks from 'v2/mocks/tasks';
import * as userMocks from 'common/mocks/current-user';
import {QueryClientProvider} from '@tanstack/react-query';
import {getMockQueryClient} from 'common/testing/getMockQueryClient';

function noop() {
  return Promise.resolve([]);
}

const getWrapper = (
  initialEntries: React.ComponentProps<
    typeof MemoryRouter
  >['initialEntries'] = ['/'],
) => {
  const mockClient = getMockQueryClient();

  const Wrapper: React.FC<{
    children?: React.ReactNode;
  }> = ({children}) => (
    <QueryClientProvider client={mockClient}>
      <MemoryRouter initialEntries={initialEntries}>
        {children}
        <Link to="/">go home</Link>
      </MemoryRouter>
    </QueryClientProvider>
  );

  return Wrapper;
};

describe('<AvailableTasks />', () => {
  beforeEach(() => {
    nodeMockServer.use(
      http.get(
        '/v2/authentication/me',
        () => {
          return HttpResponse.json(userMocks.currentUser);
        },
        {once: true},
      ),
    );
  });

  it('should not render when loading', async () => {
    const {rerender} = render(
      <AvailableTasks
        loading
        onScrollDown={noop}
        onScrollUp={noop}
        hasNextPage={false}
        hasPreviousPage={false}
        tasks={[]}
      />,
      {
        wrapper: getWrapper(),
      },
    );

    expect(screen.queryByTestId('task-0')).not.toBeInTheDocument();
    expect(screen.getByTestId('tasks-skeleton')).toBeInTheDocument();

    rerender(
      <AvailableTasks
        loading={false}
        onScrollDown={noop}
        onScrollUp={noop}
        hasNextPage={false}
        hasPreviousPage={false}
        tasks={tasksMocks.tasks}
      />,
    );

    await waitForElementToBeRemoved(screen.queryByTestId('tasks-skeleton'));

    expect(screen.queryByTestId('tasks-skeleton')).not.toBeInTheDocument();
    expect(screen.getByTestId('task-0')).toBeInTheDocument();
  });

  it('should render tasks', async () => {
    render(
      <AvailableTasks
        loading={false}
        onScrollDown={noop}
        onScrollUp={noop}
        hasNextPage={false}
        hasPreviousPage={false}
        tasks={tasksMocks.tasks}
      />,
      {wrapper: getWrapper()},
    );

    await waitForElementToBeRemoved(screen.queryByTestId('tasks-skeleton'));

    const [firstTask, secondTask] = tasksMocks.tasks;

    const withinFirstTask = within(screen.getByTestId('task-0'));
    const withinSecondTask = within(screen.getByTestId('task-1'));

    expect(withinFirstTask.getByText(firstTask.name!)).toBeInTheDocument();
    expect(
      withinFirstTask.getByText(firstTask.processName!),
    ).toBeInTheDocument();
    expect(
      withinFirstTask.getByTitle('Created on 1st of January, 2024 at 12:00 AM'),
    ).toBeInTheDocument();
    expect(await withinFirstTask.findByText('Me')).toBeInTheDocument();

    expect(withinSecondTask.getByText(secondTask.name!)).toBeInTheDocument();
    expect(
      withinSecondTask.getByText(secondTask.processName!),
    ).toBeInTheDocument();
    expect(
      withinSecondTask.getByTitle('Created on 1st of January, 2024 at 1:00 AM'),
    ).toBeInTheDocument();
    expect(withinSecondTask.getByText('mustermann')).toBeInTheDocument();
  });

  it('should render empty message when there are no tasks', async () => {
    render(
      <AvailableTasks
        loading={false}
        onScrollDown={noop}
        onScrollUp={noop}
        hasNextPage={false}
        hasPreviousPage={false}
        tasks={[]}
      />,
      {wrapper: getWrapper()},
    );

    await waitForElementToBeRemoved(screen.queryByTestId('tasks-skeleton'));

    expect(screen.getByText('No tasks found')).toBeInTheDocument();
    expect(
      screen.getByText('There are no tasks matching your filter criteria.'),
    ).toBeInTheDocument();
  });
});
