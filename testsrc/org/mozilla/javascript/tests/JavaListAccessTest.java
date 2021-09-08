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
 * This testcase tests the basic access to java List with []
 */
public class JavaListAccessTest extends TestCase {

    @Test
    public void testBeanAccess() {
        String js =
                "bean.integers[0] = 3;\n"
                        + "bean.doubles[0] = 3;"
                        + "bean.doubles[0].getClass().getSimpleName() + ' ' "
                        + "+ bean.integers[0].getClass().getSimpleName()\n";
        testIt(js, "Double Integer");
    }

    @Test
    public void testListAccess() {
        String js =
                "intList[0] = 3;\n"
                        + "dblList[0] = 3;"
                        + "dblList[0].getClass().getSimpleName() + ' ' "
                        + "+ intList[0].getClass().getSimpleName()\n";
        testIt(js, "Double Integer");
    }

    @Test
    public void testIntListIncrement() {
        String js =
                "intList[0] = 3.5;\n"
                        + "intList[0]++;\n"
                        + "intList[0].getClass().getSimpleName() + ' ' + intList[0]\n";
        testIt(js, "Integer 4");
    }

    @Test
    public void testDblListIncrement() {
        String js =
                "dblList[0] = 3.5;\n"
                        + "dblList[0]++;\n"
                        + "dblList[0].getClass().getSimpleName() + ' ' + dblList[0]\n";
        testIt(js, "Double 4.5");
    }

    public static class Bean {
        public List<Integer> integers = new ArrayList<>();
        private List<Double> doubles = new ArrayList<>();

        public List<Double> getDoubles() {
            return doubles;
        }

        public List<Number> numbers = new ArrayList<>();
    }

    private List<Integer> createIntegerList() {
        List<Integer> list = new ArrayList<Integer>() {};

        list.add(42);
        list.add(7);
        return list;
    }

    private List<Double> createDoubleList() {
        List<Double> list = new ArrayList<Double>() {};

        list.add(42.5);
        list.add(7.5);
        return list;
    }

    private List<Number> createNumberList() {
        List<Number> list = new ArrayList<Number>() {};

        list.add(42);
        list.add(7.5);
        return list;
    }

    private void testIt(String script, String expected) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    final ScriptableObject scope = cx.initStandardObjects();
                    scope.put("intList", scope, createIntegerList());
                    scope.put("dblList", scope, createDoubleList());
                    scope.put("numList", scope, createNumberList());
                    scope.put("bean", scope, new Bean());
                    Object o = cx.evaluateString(scope, script, "testJavaArrayIterate.js", 1, null);
                    assertEquals(expected, o);

                    return null;
                });
    }
}
