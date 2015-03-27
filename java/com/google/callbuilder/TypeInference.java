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

import com.google.callbuilder.Unification.Result;
import com.google.callbuilder.Unification.Sequence;
import com.google.callbuilder.Unification.Unifiable;
import com.google.callbuilder.Unification.Variable;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeParameterElement;
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

  private static ImmutableMap<String, Variable> overridenTypeVariables(ExecutableElement method) {
    ImmutableMap.Builder<String, Variable> variables = new ImmutableMap.Builder<>();
    for (TypeParameterElement typeParameter : method.getTypeParameters()) {
      variables.put(typeParameter.toString(), new Variable());
    }
    return variables.build();
  }

  /**
   * Attempts to perform type inference for the field that has the given style.
   * @param fieldStyle the style of the field
   * @param parameter the original parameter on the annotated method for which the field is being
   *     generated
   */
  static Optional<TypeInference> forField(FieldStyle fieldStyle, VariableElement parameter) {
    ImmutableList.Builder<Unifiable> lhs = new ImmutableList.Builder<>();
    ImmutableList.Builder<Unifiable> rhs = new ImmutableList.Builder<>();
    AtomAndVarRegistry registry = new AtomAndVarRegistry();

    Variable builderFieldType = new Variable();
    ImmutableMap<String, Variable> startOverridenTypeVariables =
        overridenTypeVariables(fieldStyle.start());
    ImmutableMap<String, Variable> finishOverridenTypeVariables =
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
    rhs.add(registry.encode(
        Iterables.getOnlyElement(fieldStyle.finish().getParameters()).asType(),
        finishOverridenTypeVariables));

    // The return type of finish() must match the value expected by the annotated method.
    lhs.add(registry.encode(
        fieldStyle.finish().getReturnType(),
        finishOverridenTypeVariables));
    rhs.add(registry.encode(parameter.asType(), ImmutableMap.<String, Variable>of()));

    Optional<Result> result = Unification.unify(new Sequence(lhs.build()), new Sequence(rhs.build()));
    if (result.isPresent()) {
      return Optional.of(new TypeInference(registry, result.get().resolve(builderFieldType)));
    } else {
      return Optional.absent();
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
   * as simple as it first seems. For instance, the {@code finish()} method may convert an
   * {@link Optional} to a nullable field, or vice-versa.
   *
   * <p>If the field type could not be determined, an {@code Optional.absent()} is returned.
   */
  String builderFieldType() {
    return registry.toType(builderFieldType);
  }

  /**
   * Returns the fully-qualified types of each parameter in the <em>generated</em> modifier, or
   * {@code Optional.absent()} if unification failed.
   */
  Optional<ImmutableList<String>> modifierParameterTypes(ExecutableElement modifier) {
    ImmutableMap<String, Variable> overridenTypeVariables = overridenTypeVariables(modifier);

    Optional<Result> maybeResult = Unification.unify(
        builderFieldType, registry.encode(modifier.getReturnType(), overridenTypeVariables));
    for (Result result : maybeResult.asSet()) {
      ImmutableList.Builder<String> parameterTypes = new ImmutableList.Builder<>();
      for (VariableElement parameters : Iterables.skip(modifier.getParameters(), 1)) {
        parameterTypes.add(
            registry.toType(
                result.resolve(
                    registry.encode(parameters.asType(), overridenTypeVariables))));
      }
      return Optional.of(parameterTypes.build());
    }
    return Optional.absent();
  }
}
