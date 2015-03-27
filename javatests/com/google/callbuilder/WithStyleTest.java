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

import com.google.callbuilder.style.ImmutableListAdding;
import com.google.callbuilder.style.OptionalSetting;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class WithStyleTest {
  static class TwoImmutableLists {
    ImmutableList<String> first;
    ImmutableList<Integer> second;

    @CallBuilder
    TwoImmutableLists(
        @BuilderField(style = ImmutableListAdding.class) ImmutableList<String> first,
        @BuilderField(style = ImmutableListAdding.class) ImmutableList<Integer> second) {
      this.first = first;
      this.second = second;
    }
  }

  static class HasOptionals {
    Optional<Integer> age;
    Optional<Integer> income;

    @CallBuilder
    HasOptionals(
        @BuilderField(style = OptionalSetting.class) Optional<Integer> age,
        @BuilderField(style = OptionalSetting.class) Optional<Integer> income) {
      this.age = age;
      this.income = income;
    }
  }

  @Test
  public void immutableListAddingFieldStyle() {
    TwoImmutableLists lists = new TwoImmutableListsBuilder()
        .addToFirst("one")
        .addToSecond(1)
        .addToFirst("DOS")
        .addToSecond(2)
        .addAllToFirst(ImmutableList.of("san", "ourfay", "FIVE"))
        .addAllToSecond(ImmutableList.of(3, 4, 5))
        .build();
    Assert.assertEquals(
        ImmutableList.of("one", "DOS", "san", "ourfay", "FIVE"),
        lists.first);
    Assert.assertEquals(
        ImmutableList.of(1, 2, 3, 4, 5),
        lists.second);
  }

  @Test
  public void optionalSettingFieldStyle() {
    HasOptionals bothEmpty = new HasOptionalsBuilder()
        .build();
    Assert.assertEquals(Optional.absent(), bothEmpty.age);
    Assert.assertEquals(Optional.absent(), bothEmpty.income);

    HasOptionals ageOnly = new HasOptionalsBuilder()
        .setAge(14)
        .build();
    Assert.assertEquals(Optional.of(14), ageOnly.age);
    Assert.assertEquals(Optional.absent(), ageOnly.income);

    HasOptionals incomeOnly = new HasOptionalsBuilder()
        .setIncome(1000)
        .build();
    Assert.assertEquals(Optional.absent(), incomeOnly.age);
    Assert.assertEquals(Optional.of(1000), incomeOnly.income);
  }
}
