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
package io.camunda.client.api.search.filter;

import io.camunda.client.api.search.enums.BatchOperationState;
import io.camunda.client.api.search.enums.BatchOperationType;
import io.camunda.client.api.search.request.TypedSearchRequest.SearchRequestFilter;

public interface BatchOperationFilter extends SearchRequestFilter {

  /**
   * Filters batch operations by the specified batchOperationId.
   *
   * @param batchOperationId the ID of the batch operation
   * @return the updated filter
   */
  BatchOperationFilter batchOperationId(final String batchOperationId);

  /**
   * Filters batch operations by the specified type.
   *
   * @param operationType the operationType
   * @return the updated filter
   */
  BatchOperationFilter operationType(final BatchOperationType operationType);

  /**
   * Filters batch operations by the specified state.
   *
   * @param state the state
   * @return the updated filter
   */
  BatchOperationFilter state(final BatchOperationState state);
}
