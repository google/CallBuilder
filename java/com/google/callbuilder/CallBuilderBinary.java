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
package com.google.callbuilder;

/**
 * This class is only here so that we can create a {@code java_binary} target which bundles up all
 * the transitive dependencies needed to release as a single jar. This is a hack until Bazel
 * supports the same functionality with a {@code java_plugin} target, or a similar feature.
 */
public class CallBuilderBinary {
  private CallBuilderBinary() {}

  public static void main(String[] args) {
    throw new UnsupportedOperationException();
  }
}
