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
package io.camunda.zeebe.model.bpmn.builder;

import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.camunda.zeebe.model.bpmn.instance.AdHocSubProcess;
import io.camunda.zeebe.model.bpmn.instance.FlowElement;
import io.camunda.zeebe.model.bpmn.instance.FlowNode;
import io.camunda.zeebe.model.bpmn.instance.bpmndi.BpmnShape;
import io.camunda.zeebe.model.bpmn.instance.dc.Bounds;
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeAdHoc;

public class AbstractAdHocSubProcessBuilder<B extends AbstractAdHocSubProcessBuilder<B>>
    extends AbstractSubProcessBuilder<B> {

  protected boolean isDone = false;

  protected AbstractAdHocSubProcessBuilder(
      final BpmnModelInstance modelInstance,
      final AdHocSubProcess element,
      final Class<?> selfType) {
    super(modelInstance, element, selfType);
  }

  /**
   * Sets the expression to retrieve the collection of active elements.
   *
   * @param expression the expression for the active elements collection
   * @return the builder object
   */
  public B zeebeActiveElementsCollectionExpression(final String expression) {
    final ZeebeAdHoc adHoc = getCreateSingleExtensionElement(ZeebeAdHoc.class);
    adHoc.setActiveElementsCollection(asZeebeExpression(expression));
    return myself;
  }

  @Override
  protected <T extends FlowNode> T createTarget(final Class<T> typeClass, final String identifier) {
    if (isDone) {
      // add the element after the ad-hoc subprocess
      return super.createTarget(typeClass, identifier);

    } else {
      // add the element inside the ad-hoc subprocess
      return createChildInsideAdHocSubProcess(typeClass, identifier);
    }
  }

  protected <T extends FlowNode> T createChildInsideAdHocSubProcess(
      final Class<T> typeClass, final String identifier) {
    final double maxBoundY = getMaxBoundY();

    final T child = createChild(typeClass, identifier);
    final BpmnShape childShape = createBpmnShape(child);

    setCoordinates(childShape, maxBoundY);
    resizeBpmnShape(childShape);

    return child;
  }

  protected void setCoordinates(final BpmnShape childShape, final double maxBoundY) {
    final BpmnShape subProcessShape = findBpmnShape(element);
    if (subProcessShape != null) {
      final Bounds subProcessBounds = subProcessShape.getBounds();
      final Bounds childBounds = childShape.getBounds();

      final double subProcessX = subProcessBounds.getX();
      childBounds.setX(subProcessX + SPACE);

      final double subProcessY = subProcessBounds.getY();
      final double childMinY = subProcessY + SPACE;
      final double childY = Math.max(childMinY, maxBoundY + SPACE);
      childBounds.setY(childY);
    }
  }

  protected double getMaxBoundY() {
    double maxY = 0;
    for (final FlowElement flowElement : element.getFlowElements()) {
      final BpmnShape bpmnShape = findBpmnShape(flowElement);
      if (bpmnShape != null) {
        final Bounds bounds = bpmnShape.getBounds();
        maxY = Math.max(bounds.getY() + bounds.getHeight(), maxY);
      }
    }
    return maxY;
  }

  B adHocSubProcessDone() {
    isDone = true;
    return myself;
  }
}
