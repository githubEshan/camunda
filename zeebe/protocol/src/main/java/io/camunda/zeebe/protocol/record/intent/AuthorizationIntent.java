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
package io.camunda.zeebe.protocol.record.intent;

public enum AuthorizationIntent implements Intent {
  ADD_PERMISSION(0),
  PERMISSION_ADDED(1),
  REMOVE_PERMISSION(2),
  PERMISSION_REMOVED(3),
  CREATE(4),
  CREATED(5),
  DELETE(6),
  DELETED(7);

  private final short value;

  AuthorizationIntent(final int value) {
    this.value = (short) value;
  }

  @Override
  public short value() {
    return value;
  }

  @Override
  public boolean isEvent() {
    switch (this) {
      case PERMISSION_ADDED:
      case PERMISSION_REMOVED:
      case CREATED:
      case DELETED:
        return true;
      default:
        return false;
    }
  }

  public static Intent from(final short value) {
    switch (value) {
      case 0:
        return ADD_PERMISSION;
      case 1:
        return PERMISSION_ADDED;
      case 2:
        return REMOVE_PERMISSION;
      case 3:
        return PERMISSION_REMOVED;
      case 4:
        return CREATE;
      case 5:
        return CREATED;
      case 6:
        return DELETE;
      case 7:
        return DELETED;
      default:
        return UNKNOWN;
    }
  }
}
