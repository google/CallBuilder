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
import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Performs symbolic unification.
 */
final class Unification {
  private Unification() {}

  static interface Unifiable {
    Unifiable apply(Map<Variable, Unifiable> substitutions);
  }

  static final class Atom implements Unifiable {
    @Override
    public Unifiable apply(Map<Variable, Unifiable> substitutions) {
      return this;
    }
  }

  static final class Variable implements Unifiable {
    @Override
    public Unifiable apply(Map<Variable, Unifiable> substitutions) {
      return substitutions.containsKey(this) ? substitutions.get(this) : this;
    }
  }

  @AutoValue
  static abstract class Sequence implements Unifiable {
    abstract ImmutableList<Unifiable> items();

    @Override
    public Unifiable apply(Map<Variable, Unifiable> substitutions) {
      return sequenceApply(substitutions, items());
    }
  }

  private static Sequence sequenceApply(
      Map<Variable, Unifiable> substitutions, Iterable<Unifiable> items) {
    ImmutableList.Builder<Unifiable> newItems = new ImmutableList.Builder<>();
    for (Unifiable oldItem : items) {
      newItems.add(oldItem.apply(substitutions));
    }
    return new AutoValue_Unification_Sequence(newItems.build());
  }

  /**
   * Applies s1 to the elements of s2 and adds them into a single list.
   */
  static final ImmutableMap<Variable, Unifiable> compose(
      Map<Variable, Unifiable> s1, Map<Variable, Unifiable> s2) {
    ImmutableMap.Builder<Variable, Unifiable> composed = new ImmutableMap.Builder<>();
    composed.putAll(s1);
    for (Map.Entry<Variable, Unifiable> entry2 : s2.entrySet()) {
      composed.put(entry2.getKey(), entry2.getValue().apply(s1));
    }
    return composed.build();
  }

  private static Optional<Result> sequenceUnify(Sequence lhs, Sequence rhs) {
    if (lhs.items().size() != rhs.items().size()) {
      return FAIL;
    }
    if (lhs.items().isEmpty()) {
      return EMPTY;
    }
    Unifiable firstLhs = lhs.items().get(0);
    Unifiable firstRhs = rhs.items().get(0);
    for (Result subs1 : unify(firstLhs, firstRhs).asSet()) {
      Sequence restLhs = sequenceApply(subs1.rawResult(), lhs.items().subList(1, lhs.items().size()));
      Sequence restRhs = sequenceApply(subs1.rawResult(), rhs.items().subList(1, rhs.items().size()));
      for (Result subs2 : sequenceUnify(restLhs, restRhs).asSet()) {
        return success(
            new ImmutableMap.Builder<Variable, Unifiable>()
                .putAll(subs1.rawResult())
                .putAll(subs2.rawResult())
                .build());
      }
    }
    return FAIL;
  }

  private static Optional<Result> success(ImmutableMap<Variable, Unifiable> mapping) {
    return Optional.<Result>of(new AutoValue_Unification_Result(mapping));
  }

  static Optional<Result> unify(Unifiable lhs, Unifiable rhs) {
    if (lhs instanceof Variable) {
      return success(ImmutableMap.<Variable, Unifiable>of((Variable) lhs, rhs));
    }
    if (rhs instanceof Variable) {
      return success(ImmutableMap.<Variable, Unifiable>of((Variable) rhs, lhs));
    }
    if (lhs instanceof Atom && rhs instanceof Atom) {
      return lhs == rhs ? EMPTY : FAIL;
    }
    if (lhs instanceof Sequence && rhs instanceof Sequence) {
      return sequenceUnify((Sequence) lhs, (Sequence) rhs);
    }
    return FAIL;
  }

  /**
   * The results of a successful unification. This object gives access to the raw variable mapping
   * that resulted from the algorithm, but also supplies functionality for resolving a variable to
   * the fullest extent possible with the {@code resolve} method.
   */
  @AutoValue
  abstract static class Result {
    /**
     * The result of the unification algorithm proper. This does not have everything completely
     * resolved - some variable substitutions are required before getting the most atom-y
     * representation.
     */
    abstract ImmutableMap<Variable, Unifiable> rawResult();

    final Unifiable resolve(Unifiable unifiable) {
      Unifiable previous;
      Unifiable current = unifiable;
      do {
        previous = current;
        current = current.apply(rawResult());
      } while (!current.equals(previous));
      return current;
    }
  }

  private static final Optional<Result> FAIL = Optional.absent();
  private static final Optional<Result> EMPTY = success(ImmutableMap.<Variable, Unifiable>of());
}
