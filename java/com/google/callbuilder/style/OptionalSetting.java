/*
 * Copyright (C) 2015 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.callbuilder.style;

import com.google.common.base.Optional;

public class OptionalSetting {
  private OptionalSetting() {}

  public static <E> Optional<E> start() {
    return Optional.absent();
  }

  public static <E> Optional<E> finish(Optional<E> from) {
    return from;
  }

  public static <E> Optional<E> set(Optional<E> start, E value) {
    return Optional.of(value);
  }
}
