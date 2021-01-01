/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Wrapper;

import junit.framework.TestCase;

/*
 * This testcase tests the support of converting javascript functions to
 * functional interface adapters
 *
 */
public class InterfaceAdapterTest extends TestCase {

    public static String joinString(String a) {
        return a + "|";
    }

    private void testIt(String js, Object expected) {
        Utils.runWithAllOptimizationLevels(cx -> {
            final ScriptableObject scope = cx.initStandardObjects();
            scope.put("list", scope, createList());
            Object o = cx.evaluateString(scope, js,
                    "testNativeFunction.js", 1, null);
            if (o instanceof Wrapper) {
                o = ((Wrapper) o).unwrap();
            }
            assertEquals(expected, o);

            return null;
        });
    }

    private List<String> createList() {
        List<String> list = new ArrayList<>();
        list.add("foo");
        list.add("bar");
        return list;
    }

    @Test
    public void testNativeFunctionAsConsumer() {
        String js = "var ret = '';\n"
                + "list.forEach(function(elem) {  ret += elem });\n"
                + "ret";

        testIt(js, "foobar");
    }

    @Test
    public void testArrowFunctionAsConsumer() {
        String js = "var ret = '';\n"
                + "list.forEach(elem => ret += elem);\n"
                + "ret";

        testIt(js, "foobar");
    }

    @Test
    public void testBoundFunctionAsConsumer() {
        String js = "var ret = '';\n"
                + "list.forEach(((c, elem) => ret += elem + c).bind(null, ','));\n"
                + "ret";

        testIt(js, "foo,bar,");
    }

    @Test
    public void testJavaMethodAsConsumer() {
        String js = "var ret = '';\n"
                + "list.stream().map(org.mozilla.javascript.tests.InterfaceAdapterTest.joinString)\n"
                +     ".forEach(elem => ret += elem);\n"
                + "ret";

        testIt(js, "foo|bar|");
    }

    @Test
    public void testArrowFunctionAsComparator() {
        String js = "list";
        testIt(js, Arrays.asList("foo", "bar"));

        js = "list.sort((a,b) => a > b ? 1:-1)\n"
                + "list";
        testIt(js, Arrays.asList("bar", "foo"));
    }

    @Test
    public void testNativeFunctionAsComparator() {
        String js = "list";
        testIt(js, Arrays.asList("foo", "bar"));
        
        js = "list.sort(function(a,b) { return a > b ? 1:-1 })\n"
                + "list";
        testIt(js, Arrays.asList("bar", "foo"));
    }

}
