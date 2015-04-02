/*
 * Copyright 2015 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.callbuilder.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(JUnit4.class)
public class ValueTypeTest {
  private final class Empty extends ValueType {
    @Override
    protected void addFields(FieldReceiver fields) { }
  }

  private final class Name extends ValueType {
    private final String family;
    private final String given;

    Name(String family, String given) {
      this.family = family;
      this.given = given;
    }

    @Override
    protected void addFields(FieldReceiver fields) {
      fields.add("family", family);
      fields.add("given", given);
    }
  }

  private final class PairThatOmitsNull extends ValueType {
    private final Object first;
    private final Object second;

    PairThatOmitsNull(Object first, Object second) {
      this.first = first;
      this.second = second;
    }

    @Override
    protected void addFields(FieldReceiver fields) {
      if (first != null) {
        fields.add("first", first);
      }
      if (second != null) {
        fields.add("second", second);
      }
    }
  }

  @Test
  public void toString_noValues() {
    assertEquals("Empty{}", new Empty().toString());
  }

  @Test
  public void toString_basic() {
    assertEquals("Name{family=Doe, given=John}", new Name("Doe", "John").toString());
  }

  @Test
  public void toString_hasNull() {
    assertEquals("Name{family=Doe, given=null}", new Name("Doe", null).toString());
  }

  private void assertMutuallyEqual(List<Object> equalityGroup) {
    for (int i = 1; i < equalityGroup.size(); i++) {
      assertEquals(equalityGroup.get(0), equalityGroup.get(i));
      assertEquals(equalityGroup.get(i), equalityGroup.get(0));
      assertEquals(equalityGroup.get(0).hashCode(), equalityGroup.get(i).hashCode());
      assertEquals(equalityGroup.get(0).toString(), equalityGroup.get(i).toString());
    }
  }

  private void assertMutuallyUnequal(List<Object> items) {
    for (int i = 0; i < items.size() - 1; i++) {
      for (int j = i + 1; j < items.size(); j++) {
        assertNotEquals(items.get(i), items.get(j));
        assertNotEquals(items.get(j), items.get(i));
      }
    }
  }

  @Test
  public void hashAndEquals() {
    List<List<Object>> equalityGroups = new ArrayList<>();
    equalityGroups.add(Arrays.asList(new Empty(), new Empty()));
    equalityGroups.add(Arrays.asList(new Name("Foo", "Bar"), new Name("Foo", "Bar")));
    equalityGroups.add(Arrays.asList(new Name("Foo", "NotBar")));
    equalityGroups.add(Arrays.asList(new Name("NotFoo", "Bar")));
    equalityGroups.add(Arrays.asList(new Name("Foo", null), new Name("Foo", null)));
    equalityGroups.add(Arrays.asList(new Name(null, "Bar"), new Name(null, "Bar")));
    equalityGroups.add(
        Arrays.asList(new PairThatOmitsNull(null, 42), new PairThatOmitsNull(null, 42)));
    equalityGroups.add(
        Arrays.asList(new PairThatOmitsNull(42, null), new PairThatOmitsNull(42, null)));
    equalityGroups.add(Arrays.asList(new PairThatOmitsNull(42, 42), new PairThatOmitsNull(42, 42)));

    List<Object> distinctItems = new ArrayList<>();
    for (List<Object> group : equalityGroups) {
      assertMutuallyEqual(group);
      distinctItems.add(group.get(0));
    }

    assertMutuallyUnequal(distinctItems);
  }
}
