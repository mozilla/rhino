package org.mozilla.javascript.tests;

import org.junit.Test;
import static org.junit.Assert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.junit.matchers.JUnitMatchers.both;

import org.mozilla.javascript.NativeArray;
import java.util.Arrays;

public class NativeArrayTest {

  @Test
  public void getIdsShouldIncludeBothIndexAndNormalProperties() {
    NativeArray array = new NativeArray(1);
    array.put(0, array, "index");
    array.put("a", array, "normal");

    assertThat(array.getIds(), is(new Object[]{0, "a"}));
  }

  @Test
  public void deleteShouldRemoveIndexProperties() {
    NativeArray array = new NativeArray(new Object[]{"a"});
    array.delete(0);
    assertThat(array.has(0, array), is(false));
  }

  @Test
  public void deleteShouldRemoveNormalProperties() {
    NativeArray array = new NativeArray(1);
    array.put("p", array, "a");
    array.delete("p");
    assertThat(array.has("p", array), is(false));
  }

  @Test
  public void putShouldAddIndexProperties() {
    NativeArray array = new NativeArray(1);
    array.put(0, array, "a");
    assertThat(array.has(0, array), is(true));
  }

  @Test
  public void putShouldAddNormalProperties() {
    NativeArray array = new NativeArray(1);
    array.put("p", array, "a");
    assertThat(array.has("p", array), is(true));
  }

  @Test
  public void getShouldReturnIndexProperties() {
    NativeArray array = new NativeArray(new Object[]{"a"});
    assertThat(array.get(0, array), is((Object)"a"));
  }

  @Test
  public void getShouldReturnNormalProperties() {
    NativeArray array = new NativeArray(1);
    array.put("p", array, "a");
    assertThat(array.get("p", array), is((Object)"a"));
  }

  @Test
  public void hasShouldBeFalseForANewArray() {
    NativeArray array = new NativeArray(0);
    assertThat(array.has(0, array), is(false));
  }

}
