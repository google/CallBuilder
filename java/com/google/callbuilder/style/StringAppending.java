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

public class StringAppending {
  private StringAppending() {}

  public static StringBuilder start() {
    return new StringBuilder();
  }

  public static String finish(StringBuilder from) {
    return from.toString();
  }

  public static StringBuilder appendTo(StringBuilder start, String value) {
    return start.append(value);
  }
}
