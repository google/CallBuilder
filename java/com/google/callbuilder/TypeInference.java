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

import com.google.callbuilder.Unification.Sequence;
import com.google.callbuilder.Unification.Substitution;
import com.google.callbuilder.Unification.Unifiable;
import com.google.callbuilder.Unification.Variable;
import com.google.callbuilder.util.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.element.VariableElement;

/**
 * Utility methods for inferring the types of things.
 */
final class TypeInference {
  private final AtomAndVarRegistry registry;
  private final Unifiable builderFieldType;

  TypeInference(AtomAndVarRegistry registry, Unifiable builderFieldType) {
    this.registry = Preconditions.checkNotNull(registry);
    this.builderFieldType = Preconditions.checkNotNull(builderFieldType);
  }

  private static Map<String, Variable> overridenTypeVariables(ExecutableElement method) {
    Map<String, Variable> variables = new HashMap<>();
    for (TypeParameterElement typeParameter : method.getTypeParameters()) {
      variables.put(typeParameter.toString(), new Variable());
    }
    return variables;
  }

  /**
   * Attempts to perform type inference for the field that has the given style.
   * @param fieldStyle the style of the field
   * @param parameter the original parameter on the annotated method for which the field is being
   *     generated
   */
  static @Nullable TypeInference forField(FieldStyle fieldStyle, VariableElement parameter) {
    List<Unifiable> lhs = new ArrayList<>();
    List<Unifiable> rhs = new ArrayList<>();
    AtomAndVarRegistry registry = new AtomAndVarRegistry();

    Variable builderFieldType = new Variable();
    Map<String, Variable> startOverridenTypeVariables =
        overridenTypeVariables(fieldStyle.start());
    Map<String, Variable> finishOverridenTypeVariables =
        overridenTypeVariables(fieldStyle.finish());

    // The generic type parameters of the start and finish method are actually variables in
    // unification, while type parameters of enclosing classes etc. are not. Override the
    // start/finish type parameters.

    // Each .add pair represents an equality constraint.

    // The return of start() must match the type of the builder field.
    lhs.add(registry.encode(
        fieldStyle.start().getReturnType(),
        startOverridenTypeVariables));
    rhs.add(builderFieldType);

    // The parameter type of finish() must also match the type of the builder field.
    lhs.add(builderFieldType);
    List<? extends VariableElement> finishParameters = fieldStyle.finish().getParameters();
    // TODO: report an error if the number of elements in finishParameters is not 1.
    rhs.add(registry.encode(
        finishParameters.get(0).asType(),
        finishOverridenTypeVariables));

    // The return type of finish() must match the value expected by the annotated method.
    lhs.add(registry.encode(
        fieldStyle.finish().getReturnType(),
        finishOverridenTypeVariables));
    rhs.add(registry.encode(parameter.asType(), Collections.<String, Variable>emptyMap()));

    Substitution result = Unification.unify(new Sequence(lhs), new Sequence(rhs));
    if (result != null) {
      return new TypeInference(registry, result.resolve(builderFieldType));
    } else {
      return null;
    }
  }

  /**
   * Returns the fully-qualified name of the field in the builder. This type is dictated by:
   * <ul>
   *   <li>the type the annotated method expects in its parameter list
   *   <li>the type the type returned by the {@link BuilderField} style's {@code start()} method
   *   <li>the type accepted by the {@link BuilderField} style's {@code finish()} method
   *   <li>the type returned by the {@link BuilderField} style's {@code finish()} method
   * </ul>
   * The {@code finish()} method can perform arbitrary changes to the builder field, so this is not
   * as simple as it first seems. For instance, the {@code finish()} method may convert a Guava
   * {@code Optional} to a nullable field, or vice-versa.
   */
  String builderFieldType() {
    return registry.toType(builderFieldType);
  }

  /**
   * Returns the fully-qualified types of each parameter in the <em>generated</em> modifier, or
   * {@code null} if unification failed.
   */
  @Nullable List<String> modifierParameterTypes(ExecutableElement modifier) {
    Map<String, Variable> overridenTypeVariables = overridenTypeVariables(modifier);

    Substitution result = Unification.unify(
        builderFieldType, registry.encode(modifier.getReturnType(), overridenTypeVariables));
    if (result != null) {
      List<String> parameterTypes = new ArrayList<>();
      List<? extends VariableElement> parameters = modifier.getParameters();
      for (VariableElement parameter : parameters.subList(1, parameters.size())) {
        parameterTypes.add(
            registry.toType(
                result.resolve(
                    registry.encode(parameter.asType(), overridenTypeVariables))));
      }
      return parameterTypes;
    }
    return null;
  }
}
