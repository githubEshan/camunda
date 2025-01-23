/*
 * Copyright 2017-present Open Networking Foundation
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomix.utils.concurrent;

import io.camunda.zeebe.util.CheckedRunnable;
import io.camunda.zeebe.util.CloseableSilently;
import io.camunda.zeebe.util.RetryDelayStrategy;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/** Scheduler. */
public interface Scheduler extends CloseableSilently {

  /**
   * Schedules a runnable after a delay.
   *
   * @param delay the delay after which to run the callback
   * @param timeUnit the time unit
   * @param callback the callback to run
   * @return the scheduled callback
   */
  Scheduled schedule(final long delay, final TimeUnit timeUnit, final Runnable callback);

  /**
   * Schedules a runnable after a delay.
   *
   * @param delay the delay after which to run the callback
   * @param callback the callback to run
   * @return the scheduled callback
   */
  default Scheduled schedule(final Duration delay, final Runnable callback) {
    return schedule(delay.toMillis(), TimeUnit.MILLISECONDS, callback);
  }

  /**
   * Schedules a runnable at a fixed rate.
   *
   * @param initialDelay the initial delay
   * @param interval the interval at which to run the callback
   * @param timeUnit the time unit
   * @param callback the callback to run
   * @return the scheduled callback
   */
  default Scheduled schedule(
      final long initialDelay,
      final long interval,
      final TimeUnit timeUnit,
      final Runnable callback) {
    return schedule(
        Duration.ofMillis(timeUnit.toMillis(initialDelay)),
        Duration.ofMillis(timeUnit.toMillis(interval)),
        callback);
  }

  /**
   * Schedules a runnable at a fixed rate.
   *
   * @param initialDelay the initial delay
   * @param interval the interval at which to run the callback
   * @param callback the callback to run
   * @return the scheduled callback
   */
  Scheduled schedule(Duration initialDelay, Duration interval, Runnable callback);

  default <A> CompletableFuture<A> retryUntilSuccessful(
      final Callable<A> callable,
      final RetryDelayStrategy retryDelayStrategy,
      final Predicate<Exception> shouldRetry) {
    final var result = new CompletableFuture<A>();
    // cannot be a lambda as it needs a reference to itself for scheduling
    final Runnable runnable =
        new Runnable() {
          Scheduled scheduled = null;

          @Override
          public void run() {
            try {
              final var a = callable.call();
              result.complete(a);
            } catch (final Exception e) {
              if (shouldRetry.test(e)) {
                if (!result.isDone()) {
                  final var next = retryDelayStrategy.nextDelay();
                  scheduled = schedule(next, this);
                }
              } else {
                result.completeExceptionally(e);
              }
            }
          }
        };

    runnable.run();
    return result;
  }

  default CompletableFuture<Void> retryUntilSuccessful(
      final CheckedRunnable runnable,
      final RetryDelayStrategy retryDelayStrategy,
      final Predicate<Exception> shouldRetry) {
    return retryUntilSuccessful(
        CheckedRunnable.toCallable(runnable), retryDelayStrategy, shouldRetry);
  }

  @Override
  default void close() {}
}
