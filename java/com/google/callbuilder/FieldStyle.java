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

import com.google.callbuilder.util.ValueType;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementFilter;

final class FieldStyle extends ValueType {
  private final DeclaredType styleClass;
  private final ImmutableList<ExecutableElement> modifiers;
  private final ExecutableElement start;
  private final ExecutableElement finish;

  FieldStyle(DeclaredType styleClass, ImmutableList<ExecutableElement> modifiers,
      ExecutableElement start, ExecutableElement finish) {
    this.styleClass = Preconditions.checkNotNull(styleClass);
    this.modifiers = Preconditions.checkNotNull(modifiers);
    this.start = Preconditions.checkNotNull(start);
    this.finish = Preconditions.checkNotNull(finish);
  }

  @Override
  protected void addFields(FieldReceiver fields) {
    fields.add("styleClass", styleClass);
    fields.add("modifiers", modifiers);
    fields.add("start", start);
    fields.add("finish", finish);
  }

  /**
   * The type corresponding to the field's style, which may be customized with the
   * @{@link BuilderField} annotation.
   */
  DeclaredType styleClass() {
    return styleClass;
  }

  ImmutableList<ExecutableElement> modifiers() {
    return modifiers;
  }

  ExecutableElement start() {
    return start;
  }

  ExecutableElement finish() {
    return finish;
  }

  public static FieldStyle fromStyleClass(DeclaredType styleClass) {
    ImmutableList.Builder<ExecutableElement> modifiers = new ImmutableList.Builder<>();
    ExecutableElement start = null;
    ExecutableElement finish = null;

    for (ExecutableElement modifier :
         ElementFilter.methodsIn(styleClass.asElement().getEnclosedElements())) {
      String name = modifier.getSimpleName().toString();
      if (name.equals("start")) {
        start = modifier;
      } else if (name.equals("finish")) {
        finish = modifier;
      } else {
        modifiers.add(modifier);
      }
    }

    if(start == null || finish == null) {
      throw new IllegalArgumentException(String.format(
          "could not find start() and/or finish() method on BuilderField style class %s",
          styleClass));
    }

    return new FieldStyle(styleClass, modifiers.build(), start, finish);
  }
}
