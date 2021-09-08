/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import org.junit.Test;
import org.mozilla.javascript.ScriptableObject;

/*
 * This testcase tests the basic access to Java classess implementing Iterable
 * (eg. ArrayList)
 */
public class JavaListIteratorTest extends TestCase {

    private static final String FOO_BAR_BAZ = "foo,bar,42.5,";

    private List<Object> createJavaList() {
        List<Object> list = new ArrayList<>();
        list.add("foo");
        list.add("bar");
        list.add(42.5);
        return list;
    }

    @Test
    public void testArrayIterator() {
        String js =
                "var ret = '';\n"
                        + "var iter = list.iterator();\n"
                        + "while(iter.hasNext()) ret += iter.next()+',';\n"
                        + "ret";
        testJavaListIterate(js, FOO_BAR_BAZ);
        // there is no .iterator() function on the JS side
    }

    @Test
    public void testArrayForEach() {
        String js = "var ret = '';\n" + "for each(elem in list)  ret += elem + ',';\n" + "ret";
        testJsArrayIterate(js, FOO_BAR_BAZ);
        testJavaListIterate(js, FOO_BAR_BAZ);
        testJavaArrayIterate(js, FOO_BAR_BAZ);
    }

    @Test
    public void testArrayForKeys() {
        String js = "var ret = '';\n" + "for(elem in list)  ret += elem + ',';\n" + "ret";
        testJsArrayIterate(js, "0,1,2,");
        testJavaListIterate(js, "0,1,2,");
        testJavaArrayIterate(js, "0,1,2,");
    }

    @Test
    public void testArrayForIndex() {
        String js =
                "var ret = '';\n"
                        + "for(var idx = 0; idx < list.length; idx++)  ret += idx + ',';\n"
                        + "ret";
        testJsArrayIterate(js, "0,1,2,");
        testJavaArrayIterate(js, "0,1,2,");
        testJavaListIterate(js, "0,1,2,");
    }

    private void testJavaArrayIterate(String script, String expected) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    final ScriptableObject scope = cx.initStandardObjects();
                    scope.put("list", scope, createJavaList().toArray());
                    Object o = cx.evaluateString(scope, script, "testJavaArrayIterate.js", 1, null);
                    assertEquals(expected, o);

                    return null;
                });
    }

    private void testJavaListIterate(String script, String expected) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    final ScriptableObject scope = cx.initStandardObjects();
                    scope.put("list", scope, createJavaList());
                    Object o = cx.evaluateString(scope, script, "testJavaListIterate.js", 1, null);
                    assertEquals(expected, o);

                    return null;
                });
    }

    private void testJsArrayIterate(String script, String expected) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    final ScriptableObject scope = cx.initStandardObjects();

                    scope.put("list", scope, cx.newArray(scope, createJavaList().toArray()));
                    Object o = cx.evaluateString(scope, script, "testJsArrayIterate.js", 1, null);
                    assertEquals(expected, o);
                    return null;
                });
    }
}
