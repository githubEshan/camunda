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
package io.camunda.zeebe.model.bpmn.validation.zeebe;

import io.camunda.zeebe.model.bpmn.impl.ZeebeConstants;
import io.camunda.zeebe.model.bpmn.instance.BpmnModelElementInstance;
import io.camunda.zeebe.model.bpmn.instance.ExtensionElements;
import io.camunda.zeebe.model.bpmn.instance.SendTask;
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebePublishMessage;
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeTaskDefinition;
import java.util.Collection;
import java.util.Collections;
import org.camunda.bpm.model.xml.validation.ModelElementValidator;
import org.camunda.bpm.model.xml.validation.ValidationResultCollector;

public final class SendTaskValidator implements ModelElementValidator<SendTask> {

  @Override
  public Class<SendTask> getElementType() {
    return SendTask.class;
  }

  @Override
  public void validate(
      final SendTask element, final ValidationResultCollector validationResultCollector) {
    IdentifiableBpmnElementValidator.validate(element, validationResultCollector);
    final ExtensionElements extensionElements = element.getExtensionElements();

    final Collection<ZeebePublishMessage> publishMessageExtensions =
        getExtensionElementsByType(extensionElements, ZeebePublishMessage.class);
    final Collection<ZeebeTaskDefinition> taskDefinitionExtensions =
        getExtensionElementsByType(extensionElements, ZeebeTaskDefinition.class);

    if (!hasExactlyOneExtension(publishMessageExtensions, taskDefinitionExtensions)) {
      validationResultCollector.addError(
          0,
          String.format(
              "Must have either one 'zeebe:%s' or one 'zeebe:%s' extension element",
              ZeebeConstants.ELEMENT_PUBLISH_MESSAGE, ZeebeConstants.ELEMENT_TASK_DEFINITION));
    }

    if (!publishMessageExtensions.isEmpty() && element.getMessage() == null) {
      validationResultCollector.addError(0, "Must reference a message");
    }
  }

  public <T extends BpmnModelElementInstance> Collection<T> getExtensionElementsByType(
      final ExtensionElements extensionElements, final Class<T> type) {
    if (extensionElements == null) {
      return Collections.emptyList();
    }
    return extensionElements.getChildElementsByType(type);
  }

  private boolean hasExactlyOneExtension(
      final Collection<ZeebePublishMessage> publishMessageExtensions,
      final Collection<ZeebeTaskDefinition> taskDefinitionExtensions) {
    return publishMessageExtensions.size() == 1 && taskDefinitionExtensions.isEmpty()
        || publishMessageExtensions.isEmpty() && taskDefinitionExtensions.size() == 1;
  }
}
