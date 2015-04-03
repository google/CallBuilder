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

import com.google.callbuilder.style.ArrayListAdding;
import com.google.callbuilder.style.StringAppending;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Arrays;

@RunWith(JUnit4.class)
public class WithStyleTest {
  static class TwoArrayLists {
    ArrayList<String> first;
    ArrayList<Integer> second;

    @CallBuilder
    TwoArrayLists(
        @BuilderField(style = ArrayListAdding.class) ArrayList<String> first,
        @BuilderField(style = ArrayListAdding.class) ArrayList<Integer> second) {
      this.first = first;
      this.second = second;
    }
  }

  static class HasStrings {
    String address;
    String name;

    @CallBuilder
    HasStrings(
        @BuilderField(style = StringAppending.class) String address,
        @BuilderField(style = StringAppending.class) String name) {
      this.address = address;
      this.name = name;
    }
  }

  @Test
  public void immutableListAddingFieldStyle() {
    TwoArrayLists lists = new TwoArrayListsBuilder()
        .addToFirst("one")
        .addToSecond(1)
        .addToFirst("DOS")
        .addToSecond(2)
        .addAllToFirst(Arrays.asList("san", "ourfay", "FIVE"))
        .addAllToSecond(Arrays.asList(3, 4, 5))
        .build();
    Assert.assertEquals(
        Arrays.asList("one", "DOS", "san", "ourfay", "FIVE"),
        lists.first);
    Assert.assertEquals(
        Arrays.asList(1, 2, 3, 4, 5),
        lists.second);
  }

  @Test
  public void stringAppendingFieldStyle() {
    HasStrings bothEmpty = new HasStringsBuilder()
        .build();
    Assert.assertEquals("", bothEmpty.address);
    Assert.assertEquals("", bothEmpty.name);

    HasStrings addressOnly = new HasStringsBuilder()
        .appendToAddress("1212 Easy St.\n")
        .appendToAddress("Big Town, XY\n")
        .appendToAddress("42042\n")
        .build();
    Assert.assertEquals("1212 Easy St.\nBig Town, XY\n42042\n", addressOnly.address);
    Assert.assertEquals("", addressOnly.name);

    HasStrings hasBoth = new HasStringsBuilder()
        .appendToName("Doe, ")
        .appendToAddress("1600 Amphitheatre Pkwy\n")
        .appendToName("John")
        .appendToAddress("Mountain View\n")
        .build();
    Assert.assertEquals("Doe, John", hasBoth.name);
    Assert.assertEquals("1600 Amphitheatre Pkwy\nMountain View\n", hasBoth.address);
  }
}
