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

import io.camunda.zeebe.model.bpmn.instance.ExclusiveGateway;
import io.camunda.zeebe.model.bpmn.instance.FlowNode;
import io.camunda.zeebe.model.bpmn.instance.InclusiveGateway;
import org.camunda.bpm.model.xml.validation.ModelElementValidator;
import org.camunda.bpm.model.xml.validation.ValidationResultCollector;

public class FlowNodeValidator implements ModelElementValidator<FlowNode> {

  @Override
  public Class<FlowNode> getElementType() {
    return FlowNode.class;
  }

  @Override
  public void validate(
      final FlowNode element, final ValidationResultCollector validationResultCollector) {
    IdentifiableBpmnElementValidator.validate(element, validationResultCollector);
    if (element instanceof ExclusiveGateway || element instanceof InclusiveGateway) {
      return;
    }

    final boolean hasAnyConditionalFlow =
        element.getOutgoing().stream().anyMatch(s -> s.getConditionExpression() != null);

    if (hasAnyConditionalFlow) {
      validationResultCollector.addError(
          0, "Conditional sequence flows are only supported at exclusive or inclusive gateway");
    }
  }
}
