/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;

public class NativeArrayTest {
    private NativeArray array;

    @Before
    public void init() {
        array = new NativeArray(1);
    }

    @Test
    public void getIdsShouldIncludeBothIndexAndNormalProperties() {
        array.put(0, array, "index");
        array.put("a", array, "normal");

        assertArrayEquals(new Object[] {0, "a"}, array.getIds());
    }

    @Test
    public void deleteShouldRemoveIndexProperties() {
        array.put(0, array, "a");
        array.delete(0);
        assertFalse(array.has(0, array));
    }

    @Test
    public void deleteShouldRemoveNormalProperties() {
        array.put("p", array, "a");
        array.delete("p");
        assertFalse(array.has("p", array));
    }

    @Test
    public void putShouldAddIndexProperties() {
        array.put(0, array, "a");
        assertTrue(array.has(0, array));
    }

    @Test
    public void putShouldAddNormalProperties() {
        array.put("p", array, "a");
        assertTrue(array.has("p", array));
    }

    @Test
    public void getShouldReturnIndexProperties() {
        array.put(0, array, "a");
        array.put("p", array, "b");
        assertEquals("a", array.get(0, array));
    }

    @Test
    public void getShouldReturnNormalProperties() {
        array.put("p", array, "a");
        assertEquals("a", array.get("p", array));
    }

    @Test
    public void hasShouldBeFalseForANewArray() {
        assertFalse(new NativeArray(0).has(0, array));
    }

    @Test
    public void getIndexIdsShouldBeEmptyForEmptyArray() {
        assertEquals(new ArrayList<Integer>(), new NativeArray(0).getIndexIds());
    }

    @Test
    public void getIndexIdsShouldBeAZeroForSimpleSingletonArray() {
        array.put(0, array, "a");
        assertEquals(Arrays.asList(0), array.getIndexIds());
    }

    @Test
    public void getIndexIdsShouldWorkWhenIndicesSetAsString() {
        array.put("0", array, "a");
        assertEquals(Arrays.asList(0), array.getIndexIds());
    }

    @Test
    public void getIndexIdsShouldNotIncludeNegativeIds() {
        array.put(-1, array, "a");
        assertEquals(new ArrayList<Integer>(), array.getIndexIds());
    }

    @Test
    public void getIndexIdsShouldIncludeIdsLessThan2ToThe32() {
        int maxIndex = (int) (1L << 31) - 1;
        array.put(maxIndex, array, "a");
        assertEquals(Arrays.asList(maxIndex), array.getIndexIds());
    }

    @Test
    public void getIndexIdsShouldNotIncludeIdsGreaterThanOrEqualTo2ToThe32() {
        array.put((1L << 31) + "", array, "a");
        assertEquals(new ArrayList<Integer>(), array.getIndexIds());
    }

    @Test
    public void getIndexIdsShouldNotReturnNonNumericIds() {
        array.put("x", array, "a");
        assertEquals(new ArrayList<Integer>(), array.getIndexIds());
    }

    @Test
    public void testToString() {
        String source =
                "var f = function() {\n"
                        + "  var obj = [0,1];\n"
                        + "  var a = obj.map(function() {return obj;});\n"
                        + "  return a.toString();\n"
                        + "};\n"
                        + "f();";

        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);

            Scriptable scope = cx.initStandardObjects();
            String result = cx.evaluateString(scope, source, "source", 1, null).toString();
            Assert.assertEquals("0,1,0,1", result);
        }
    }
}
