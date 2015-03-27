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

import java.util.ArrayList;

public class ArrayListAdding {
  private ArrayListAdding() {}

  public static <T> ArrayList<T> start() {
    return new ArrayList<>();
  }

  public static <T> ArrayList<T> finish(ArrayList<T> list) {
    return list;
  }

  public static <T> ArrayList<T> addTo(ArrayList<T> to, T item) {
    to.add(item);
    return to;
  }

  public static <T, E> ArrayList<T> addAllTo(ArrayList<T> to, Iterable<T> items) {
    for (T item : items) {
      to.add(item);
    }
    return to;
  }
}
