/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.camunda.client.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.camunda.client.impl.search.SearchQuerySortRequest;
import io.camunda.client.impl.search.SearchQuerySortRequestMapper;
import io.camunda.client.protocol.rest.IncidentFilterRequest;
import io.camunda.client.protocol.rest.IncidentFilterRequest.ErrorTypeEnum;
import io.camunda.client.protocol.rest.IncidentFilterRequest.StateEnum;
import io.camunda.client.protocol.rest.IncidentSearchQueryRequest;
import io.camunda.client.protocol.rest.SearchQueryPageRequest;
import io.camunda.client.protocol.rest.SortOrderEnum;
import io.camunda.client.util.ClientRestTest;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

public class SearchIncidentTest extends ClientRestTest {

  @Test
  void shouldGetIncident() {
    // when
    final long incidentKey = 0xC00L;
    client.newIncidentGetRequest(incidentKey).send().join();

    // then
    final LoggedRequest request = gatewayService.getLastRequest();
    assertThat(request.getUrl()).isEqualTo("/v2/incidents/" + incidentKey);
    assertThat(request.getMethod()).isEqualTo(RequestMethod.GET);
  }

  @Test
  public void shouldSearchIncidentWithEmptyQuery() {
    // when
    client.newIncidentQuery().send().join();

    // then
    final IncidentSearchQueryRequest request =
        gatewayService.getLastRequest(IncidentSearchQueryRequest.class);
    assertThat(request.getFilter()).isNull();
  }

  @Test
  public void shouldSearchIncidentWithFullFilters() {
    // when
    client
        .newIncidentQuery()
        .filter(
            f ->
                f.incidentKey(1L)
                    .processDefinitionKey(2L)
                    .processDefinitionId("complexProcess")
                    .processInstanceKey(3L)
                    .errorType("CALLED_DECISION_ERROR")
                    .errorMessage("Can't decide")
                    .flowNodeId("flowNode")
                    .flowNodeInstanceKey(4L)
                    .creationTime("2024-05-23T23:05:00.000+000")
                    .state("ACTIVE")
                    .jobKey(5L)
                    .tenantId("tenant"))
        .send()
        .join();
    // then
    final IncidentSearchQueryRequest request =
        gatewayService.getLastRequest(IncidentSearchQueryRequest.class);
    final IncidentFilterRequest filter = request.getFilter();
    assertThat(filter.getIncidentKey()).isEqualTo(1L);
    assertThat(filter.getProcessDefinitionKey()).isEqualTo(2L);
    assertThat(filter.getProcessDefinitionId()).isEqualTo("complexProcess");
    assertThat(filter.getProcessInstanceKey()).isEqualTo(3L);
    assertThat(filter.getErrorType()).isEqualTo(ErrorTypeEnum.CALLED_DECISION_ERROR);
    assertThat(filter.getErrorMessage()).isEqualTo("Can't decide");
    assertThat(filter.getFlowNodeId()).isEqualTo("flowNode");
    assertThat(filter.getFlowNodeInstanceKey()).isEqualTo(4L);
    assertThat(filter.getCreationTime()).isEqualTo("2024-05-23T23:05:00.000+000");
    assertThat(filter.getState()).isEqualTo(StateEnum.ACTIVE);
    assertThat(filter.getJobKey()).isEqualTo(5L);
    assertThat(filter.getTenantId()).isEqualTo("tenant");
  }

  @Test
  void shouldSearchIncidentWithFullSorting() {
    // when
    client
        .newIncidentQuery()
        .sort(
            s ->
                s.incidentKey()
                    .asc()
                    .errorType()
                    .asc()
                    .processDefinitionKey()
                    .asc()
                    .processInstanceKey()
                    .asc()
                    .tenantId()
                    .asc()
                    .flowNodeInstanceKey()
                    .asc()
                    .flowNodeId()
                    .asc()
                    .state()
                    .asc()
                    .jobKey()
                    .asc()
                    .creationTime()
                    .desc())
        .send()
        .join();

    // then
    final IncidentSearchQueryRequest request =
        gatewayService.getLastRequest(IncidentSearchQueryRequest.class);
    final List<SearchQuerySortRequest> sorts =
        SearchQuerySortRequestMapper.fromIncidentSearchQuerySortRequest(
            Objects.requireNonNull(request.getSort()));
    assertThat(sorts).hasSize(10);
    assertSort(sorts.get(0), "incidentKey", SortOrderEnum.ASC);
    assertSort(sorts.get(1), "errorType", SortOrderEnum.ASC);
    assertSort(sorts.get(2), "processDefinitionKey", SortOrderEnum.ASC);
    assertSort(sorts.get(3), "processInstanceKey", SortOrderEnum.ASC);
    assertSort(sorts.get(4), "tenantId", SortOrderEnum.ASC);
    assertSort(sorts.get(5), "flowNodeInstanceKey", SortOrderEnum.ASC);
    assertSort(sorts.get(6), "flowNodeId", SortOrderEnum.ASC);
    assertSort(sorts.get(7), "state", SortOrderEnum.ASC);
    assertSort(sorts.get(8), "jobKey", SortOrderEnum.ASC);
    assertSort(sorts.get(9), "creationTime", SortOrderEnum.DESC);
  }

  @Test
  void shouldSearchWithFullPagination() {
    // when
    client
        .newIncidentQuery()
        .page(
            p ->
                p.from(23)
                    .limit(5)
                    .searchBefore(Arrays.asList("b"))
                    .searchAfter(Arrays.asList("a")))
        .send()
        .join();

    // then
    final IncidentSearchQueryRequest request =
        gatewayService.getLastRequest(IncidentSearchQueryRequest.class);
    final SearchQueryPageRequest pageRequest = request.getPage();
    assertThat(pageRequest.getFrom()).isEqualTo(23);
    assertThat(pageRequest.getLimit()).isEqualTo(5);
    assertThat(pageRequest.getSearchBefore()).isEqualTo(Arrays.asList("b"));
    assertThat(pageRequest.getSearchAfter()).isEqualTo(Arrays.asList("a"));
  }

  private void assertSort(
      final SearchQuerySortRequest sort, final String name, final SortOrderEnum order) {
    assertThat(sort.getField()).isEqualTo(name);
    assertThat(sort.getOrder()).isEqualTo(order);
  }
}
