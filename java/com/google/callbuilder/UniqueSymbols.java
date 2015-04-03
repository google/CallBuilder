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

import com.google.callbuilder.util.Preconditions;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Generates unique identifiers given ones that are declared or used by the user.
 */
public final class UniqueSymbols {
  public static final class Builder {
    private final Set<String> userDefined = new HashSet<>();

    public Builder addUserDefined(String name) {
      userDefined.add(name);
      return this;
    }

    public Builder addAllUserDefined(Collection<String> names) {
      userDefined.addAll(names);
      return this;
    }

    public UniqueSymbols build() {
      return new UniqueSymbols(userDefined);
    }
  }

  private final Set<String> userDefined;

  private final Map<String, String> cached;
  private final Set<String> used;

  private UniqueSymbols(Set<String> userDefined) {
    this.userDefined = Collections.unmodifiableSet(new HashSet<>(userDefined));

    this.cached = new HashMap<>();
    this.used = new HashSet<>();
    this.used.addAll(userDefined);
  }

  public Set<String> getUserDefined() {
    return userDefined;
  }

  private static String template(String desired, int tryIndex) {
    return String.format("%s_gensym_%d", desired, tryIndex);
  }

  /**
   * Returns a unique name which can be used for an identifier generated implicitly. Note that this
   * method returns differing {@link String}s even when called repeatedly with the same value for
   * {@code desired}. This is to encourage storing the generated name elsewhere, which avoids typos
   * and discourages implicit naming conventions.
   */
  public String get(String desired) {
    if (cached.containsKey(desired)) {
      return cached.get(desired);
    }

    int tryIndex = 0;
    while (true) {
      String collisionCheck = template(desired, tryIndex);
      if (used.add(collisionCheck)) {
        cached.put(desired, collisionCheck);
        return collisionCheck;
      }
    }
  }
}
