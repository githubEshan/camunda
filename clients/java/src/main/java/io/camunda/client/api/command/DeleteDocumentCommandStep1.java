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
package io.camunda.client.api.command;

import io.camunda.client.api.ExperimentalApi;
import io.camunda.client.api.response.DeleteDocumentResponse;

/** Command to delete a document from the document store. */
@ExperimentalApi("https://github.com/camunda/issues/issues/841")
public interface DeleteDocumentCommandStep1 extends FinalCommandStep<DeleteDocumentResponse> {

  /**
   * Sets the document store ID. If not set, the default document store for the cluster will be
   * used.
   *
   * @param storeId the document store ID
   */
  DeleteDocumentCommandStep1 storeId(String storeId);
}
