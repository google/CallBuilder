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

import com.google.common.collect.ImmutableList;

public class ImmutableListAdding {
  private ImmutableListAdding() {}

  public static <E> ImmutableList.Builder<E> start() {
    return new ImmutableList.Builder<>();
  }

  public static <E> ImmutableList<E> finish(ImmutableList.Builder<E> from) {
    return from.build();
  }

  public static <E> ImmutableList.Builder<E> addTo(ImmutableList.Builder<E> start, E item) {
    start.add(item);
    return start;
  }

  public static <E> ImmutableList.Builder<E> addAllTo(ImmutableList.Builder<E> start, Iterable<E> items) {
    start.addAll(items);
    return start;
  }
}
