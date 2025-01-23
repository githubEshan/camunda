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

public enum CommandDistributionIntent implements Intent {
  STARTED(0),
  DISTRIBUTING(1),
  ACKNOWLEDGE(2),
  ACKNOWLEDGED(3),
  FINISHED(4),
  ENQUEUED(5),
  CONTINUATION_REQUESTED(6),
  CONTINUED(7),
  FINISH(8),
  CONTINUE(9);

  private final short value;

  CommandDistributionIntent(final int value) {
    this.value = (short) value;
  }

  @Override
  public short value() {
    return value;
  }

  @Override
  public boolean isEvent() {
    switch (this) {
      case STARTED:
      case DISTRIBUTING:
      case ACKNOWLEDGED:
      case FINISHED:
      case ENQUEUED:
      case CONTINUATION_REQUESTED:
      case CONTINUED:
        return true;
      default:
        return false;
    }
  }

  public static Intent from(final short value) {
    switch (value) {
      case 0:
        return STARTED;
      case 1:
        return DISTRIBUTING;
      case 2:
        return ACKNOWLEDGE;
      case 3:
        return ACKNOWLEDGED;
      case 4:
        return FINISHED;
      case 5:
        return ENQUEUED;
      case 6:
        return CONTINUATION_REQUESTED;
      case 7:
        return CONTINUED;
      case 8:
        return FINISH;
      case 9:
        return CONTINUE;
      default:
        return Intent.UNKNOWN;
    }
  }
}
