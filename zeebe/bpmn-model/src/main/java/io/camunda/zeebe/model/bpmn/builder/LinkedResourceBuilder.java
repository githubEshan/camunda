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

import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeBindingType;
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeLinkedResource;

public class LinkedResourceBuilder {
  private final ZeebeLinkedResource element;
  private final AbstractBaseElementBuilder<?, ?> elementBuilder;

  protected LinkedResourceBuilder(
      final ZeebeLinkedResource element, final AbstractBaseElementBuilder<?, ?> elementBuilder) {
    this.element = element;
    this.elementBuilder = elementBuilder;
  }

  public LinkedResourceBuilder resourceId(final String resourceId) {
    element.setResourceId(resourceId);
    return this;
  }

  public LinkedResourceBuilder bindingType(final ZeebeBindingType bindingType) {
    element.setBindingType(bindingType);
    return this;
  }

  public LinkedResourceBuilder resourceType(final String resourceType) {
    element.setResourceType(resourceType);
    return this;
  }

  public LinkedResourceBuilder versionTag(final String versionTag) {
    element.setVersionTag(versionTag);
    return this;
  }

  public LinkedResourceBuilder linkName(final String linkName) {
    element.setLinkName(linkName);
    return this;
  }
}
