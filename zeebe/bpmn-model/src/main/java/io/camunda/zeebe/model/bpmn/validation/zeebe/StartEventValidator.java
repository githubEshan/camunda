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

import static io.camunda.zeebe.model.bpmn.util.ModelUtil.validateExecutionListenersDefinitionForElement;

import io.camunda.zeebe.model.bpmn.instance.EventDefinition;
import io.camunda.zeebe.model.bpmn.instance.StartEvent;
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeExecutionListener;
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeExecutionListenerEventType;
import java.util.Collection;
import org.camunda.bpm.model.xml.validation.ModelElementValidator;
import org.camunda.bpm.model.xml.validation.ValidationResultCollector;

public class StartEventValidator implements ModelElementValidator<StartEvent> {
  @Override
  public Class<StartEvent> getElementType() {
    return StartEvent.class;
  }

  @Override
  public void validate(
      final StartEvent element, final ValidationResultCollector validationResultCollector) {
    IdentifiableBpmnElementValidator.validate(element, validationResultCollector);
    final Collection<EventDefinition> eventDefinitions = element.getEventDefinitions();
    if (eventDefinitions.size() > 1) {
      validationResultCollector.addError(0, "Start event can't have more than one type");
    }

    validateExecutionListenersDefinitionForElement(
        element,
        validationResultCollector,
        listeners -> {
          final boolean startExecutionListenersDefined =
              listeners.stream()
                  .map(ZeebeExecutionListener::getEventType)
                  .anyMatch(ZeebeExecutionListenerEventType.start::equals);
          if (startExecutionListenersDefined) {
            validationResultCollector.addError(
                0, "Execution listeners of type 'start' are not supported by start events");
          }
        });
  }
}
