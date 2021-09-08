/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

/*
 * This testcase tests the basic access to Java classess implementing Iterable
 * (eg. ArrayList)
 */
@RunWith(Parameterized.class)
public class JavaIterableIteratorTest extends TestCase {

    private static final String FOO_BAR_BAZ = "foo,bar,42.5,";

    @Parameters
    public static Collection<Iterable<Object>> data() {
        return Arrays.asList(
                new Iterable[] {arrayList(), linkedHashSet(), iterable(), collection()});
    }

    private Iterable<Object> iterable;

    public JavaIterableIteratorTest(Iterable<Object> iterable) {
        this.iterable = iterable;
    }

    private static List<Object> arrayList() {
        List<Object> list = new ArrayList<>();
        list.add("foo");
        list.add("bar");
        list.add(42.5);
        return list;
    }

    private static Set<Object> linkedHashSet() {
        return new LinkedHashSet<>(arrayList());
    }

    private static Iterable<Object> iterable() {
        return new Iterable<Object>() {

            @Override
            public Iterator<Object> iterator() {
                return arrayList().iterator();
            }
        };
    }

    private static Collection<Object> collection() {
        return new AbstractCollection<Object>() {

            @Override
            public Iterator<Object> iterator() {
                return arrayList().iterator();
            }

            @Override
            public int size() {
                return arrayList().size();
            }
        };
    }

    @Test
    public void testJavaIterator() {
        String js =
                "var ret = '';\n"
                        + "var iter = list.iterator();\n"
                        + "while(iter.hasNext()) ret += iter.next()+',';\n"
                        + "ret";
        testIterate(js, FOO_BAR_BAZ);
    }

    @Test
    public void testForEach() {
        String jsForEach =
                "var ret = '';\n" + "for each(elem in list)  ret += elem + ',';\n" + "ret";

        testIterate(jsForEach, FOO_BAR_BAZ);
    }

    @Test
    public void testForKeys() {
        String jsForKeys = "var ret = '';\n" + "for (elem in list)  ret += elem + ',';\n" + "ret";
        testIterate(jsForKeys, "0,1,2,");
    }

    @Test
    public void testForOf() {
        String jsForKeys =
                "var ret = '';\n" + "for (var elem of list)  ret += elem + ',';\n" + "ret";
        testIterate(jsForKeys, FOO_BAR_BAZ);
    }

    // use the java object directly
    private void testIterate(String script, String expected) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final ScriptableObject scope = cx.initStandardObjects();
                    scope.put("list", scope, iterable);
                    Object o = cx.evaluateString(scope, script, "testIterate.js", 1, null);
                    assertEquals(expected, o);

                    return null;
                });
    }
}
