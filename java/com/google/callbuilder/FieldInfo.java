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

import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;

@AutoValue
abstract class FieldInfo {
  /**
   * Method parameter from which this field was automatically generated.
   */
  abstract VariableElement parameter();

  abstract Optional<FieldStyle> style();

  /**
   * The name of this field, which is used in the builder method names. This is the name of the
   * original {@link parameter()}.
   */
  String name() {
    return parameter().getSimpleName().toString();
  }

  /**
   * The type of the field after it is converted by the style's {@code finish} method.
   */
  String finishType() {
    return parameter().asType().toString();
  }

  static FieldInfo from(Elements elementUtils, VariableElement parameter) {
    Optional<FieldStyle> style = Optional.absent();

    // Look for style field on the @BuilderField annotation. If the annotation is
    // present, the value of that field overrides the default set above.
    for (AnnotationMirror ann : parameter.getAnnotationMirrors()) {
      if (ann.getAnnotationType().toString()
          .equals(BuilderField.class.getCanonicalName())) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> annEl :
             ann.getElementValues().entrySet()) {
          if ("style".equals(annEl.getKey().getSimpleName().toString())) {
            style = Optional.of(
                FieldStyle.fromStyleClass((DeclaredType) annEl.getValue().getValue()));
          }
        }
      }
    }

    return new AutoValue_FieldInfo(parameter, style);
  }

  static ImmutableList<FieldInfo> fromAll(
      Elements elementUtils, Iterable<? extends VariableElement> parameters) {
    ImmutableList.Builder<FieldInfo> fields = new ImmutableList.Builder<>();
    for (VariableElement parameter : parameters) {
      fields.add(from(elementUtils, parameter));
    }
    return fields.build();
  }
}
