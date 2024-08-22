/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class EqualObjectGraphsTest {

    @Test
    public void cyclic() {
        assertTrue(equal(makeCyclic("foo"), makeCyclic("foo")));
        // countertest; make unequal ones and make sure they test unequal
        assertFalse(equal(makeCyclic("foo"), makeCyclic("bar")));
    }

    private static boolean equal(Object o1, Object o2) {
        return new EqualObjectGraphs().equalGraphs(o1, o2);
    }

    private static Object makeCyclic(String key) {
        Object[] o1 = new Object[1];
        List<Object> o2 = new ArrayList<>();
        Map<String, Object> o3 = new HashMap<>();
        o1[0] = o2;
        o2.add(o3);
        o3.put(key, o1);
        return o1;
    }

    private static class TestObject {
        private final int x;

        TestObject(int x) {
            this.x = x;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof TestObject) {
                return x == ((TestObject) o).x;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return x;
        }
    }

    @Test
    public void sameValueDifferentTopology() {
        Object[] o1 = new Object[2];
        Object[] o2 = new Object[2];
        Object t1 = new TestObject(0);
        Object t2 = new TestObject(0);
        o1[0] = t1;
        o1[1] = t2;
        Object t3 = new TestObject(0);
        Object t4 = new TestObject(0);
        o2[0] = t3;
        o2[1] = t4;

        // Same values, same topology
        assertTrue(equal(o1, o2));

        // Same values, different topology
        o2[1] = t3;
        assertFalse(equal(o1, o2));
    }

    @Test
    public void heterogenousScriptables() {
        try (Context cx = Context.enter()) {
            ScriptableObject top = cx.initStandardObjects();
            ScriptRuntime.doTopCall(
                    (Callable)
                            (c, scope, thisObj, args) -> {
                                assertTrue(
                                        equal(
                                                makeHeterogenousScriptable(cx, "v1"),
                                                makeHeterogenousScriptable(cx, "v1")));
                                assertFalse(
                                        equal(
                                                makeHeterogenousScriptable(cx, "v1"),
                                                makeHeterogenousScriptable(cx, "v2")));
                                return null;
                            },
                    cx,
                    top,
                    top,
                    null,
                    false);
        }
    }

    private static Object makeHeterogenousScriptable(Context cx, String discriminator) {
        ScriptableObject global = cx.initStandardObjects();
        ScriptableObject s = (ScriptableObject) cx.newObject(global);
        s.put(0, s, "i0");
        s.put(1, s, "i1");
        s.put(2, s, "i2");
        s.put("s0", s, "v0");
        s.put("s1", s, discriminator);
        s.put("s2", s, "v2");
        return s;
    }
}
