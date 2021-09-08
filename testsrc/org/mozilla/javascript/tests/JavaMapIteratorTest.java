/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/*
 * This testcase tests the basic access to Java classess implementing Iterable
 * (eg. ArrayList)
 */
@RunWith(Parameterized.class)
public class JavaMapIteratorTest {

    private static final String EXPECTED_VALUES = "7,2,5,";
    private static final String EXPECTED_KEYS = "foo,bar,baz,";

    @Parameters
    public static Collection<Map<?, ?>> data() {
        return Arrays.asList(new Map[] {mapWithEnumKey(), mapWithStringKey()});
    }

    private static Map<String, Integer> mapWithStringKey() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("foo", 7);
        map.put("bar", 2);
        map.put("baz", 5);
        return map;
    }

    public enum MyEnum {
        foo,
        bar,
        baz
    }

    private static Map<MyEnum, Integer> mapWithEnumKey() {
        Map<MyEnum, Integer> map = new EnumMap<>(MyEnum.class);
        map.put(MyEnum.foo, 7);
        map.put(MyEnum.bar, 2);
        map.put(MyEnum.baz, 5);
        return map;
    }

    private Map<?, ?> map;

    public JavaMapIteratorTest(Map<?, ?> map) {
        this.map = map;
    }

    // iterate over all values with 'for each'
    @Test
    public void testForEachValue() {
        String js = "var ret = '';\n" + "for each(value in map)  ret += value + ',';\n" + "ret";
        testJsMap(js, EXPECTED_VALUES);
        testJavaMap(js, EXPECTED_VALUES);
    }

    // iterate over all keys and concatenate them
    @Test
    public void testForKey() {
        String js = "var ret = '';\n" + "for(key in map)  ret += key + ',';\n" + "ret";
        testJsMap(js, EXPECTED_KEYS);
        testJavaMap(js, EXPECTED_KEYS);
    }

    // iterate over all keys and try to read the map value
    @Test
    public void testForKeyWithGet() {
        String js = "var ret = '';\n" + "for(key in map)  ret += map[key] + ',';\n" + "ret";
        testJsMap(js, EXPECTED_VALUES);
        testJavaMap(js, EXPECTED_VALUES);
    }

    // invoke map.forEach function.
    // NOTE: signature of forEach is different
    // EcmaScript Map: forEach(value, key, map)
    // Java: forEach(key, value)
    @Test
    public void testMapForEach1() {
        String js =
                "var ret = '';\n" + "map.forEach(function(key) {  ret += key + ',' });\n" + "ret";
        testJavaMap(js, EXPECTED_KEYS);
    }

    @Test
    public void testMapForEach2() {
        String js =
                "var ret = '';\n"
                        + "map.forEach(function(key, value) {  ret += value + ',' });\n"
                        + "ret";
        testJavaMap(js, EXPECTED_VALUES); // forEach(key, value)
    }

    @Test
    public void testMapForEach3() {
        String js =
                "var ret = '';\n"
                        + "map.forEach(function(key) {  ret += map[key] + ',' });\n"
                        + "ret";
        testJavaMap(js, EXPECTED_VALUES);
    }

    @Test
    public void testObjectKeys() {
        String js = "Object.keys(map).join(',')+',';\n";
        testJavaMap(js, EXPECTED_KEYS);
        testJsMap(js, EXPECTED_KEYS);
    }

    private void testJavaMap(String script, Object expected) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final ScriptableObject scope = cx.initStandardObjects();
                    scope.put("map", scope, map);
                    Object o = cx.evaluateString(scope, script, "testJavaMap.js", 1, null);
                    assertEquals(expected, o);

                    return null;
                });
    }

    private void testJsMap(String script, Object expected) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    final ScriptableObject scope = cx.initStandardObjects();
                    Scriptable obj = cx.newObject(scope);
                    map.forEach((key, value) -> obj.put(String.valueOf(key), obj, value));
                    scope.put("map", scope, obj);
                    Object o = cx.evaluateString(scope, script, "testJsMap.js", 1, null);
                    assertEquals(expected, o);

                    return null;
                });
    }
}
