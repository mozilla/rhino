package org.mozilla.javascript.tests.es2015;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

/**
 * Tests for Array.from spec compliance, particularly prioritizing iterable over array-like. See
 * issue #1518
 */
public class ArrayFromTest {

    @Test
    public void testArrayFromPrioritizesIterableOverArrayLike() {
        // Test that Array.from uses Symbol.iterator even on native arrays
        String script =
                "let counter = 0;"
                        + "Array.prototype[Symbol.iterator] = function* () {"
                        + "    for (let i = 0; i < this.length; i++) {"
                        + "        counter++;"
                        + "        yield this[i];"
                        + "    }"
                        + "};"
                        + "Array.from(['a','b','c']);"
                        + "counter;";

        Utils.assertWithAllModes_ES6(3, script);
    }

    @Test
    public void testArrayFromUsesCustomIterator() {
        // Test that custom iterator is used over array-like behavior
        String script =
                "const obj = {"
                        + "    length: 3,"
                        + "    0: 'a',"
                        + "    1: 'b',"
                        + "    2: 'c',"
                        + "    [Symbol.iterator]: function* () {"
                        + "        yield 'x';"
                        + "        yield 'y';"
                        + "    }"
                        + "};"
                        + "JSON.stringify(Array.from(obj));";

        Utils.assertWithAllModes_ES6("[\"x\",\"y\"]", script);
    }

    @Test
    public void testArrayFromWithMapFunction() {
        // Test that map function works with custom iterator
        String script =
                "const obj = {"
                        + "    length: 2,"
                        + "    0: 'ignored',"
                        + "    1: 'ignored',"
                        + "    [Symbol.iterator]: function* () {"
                        + "        yield 'a';"
                        + "        yield 'b';"
                        + "    }"
                        + "};"
                        + "const result = Array.from(obj, function(x, i) {"
                        + "    return x.toUpperCase();"
                        + "});"
                        + "JSON.stringify(result);";

        Utils.assertWithAllModes_ES6("[\"A\",\"B\"]", script);
    }

    @Test
    public void testArrayFromFallsBackToArrayLike() {
        // Test that array-like behavior still works when no iterator
        String script =
                "const obj = {"
                        + "    length: 2,"
                        + "    0: 'a',"
                        + "    1: 'b'"
                        + "};"
                        + "JSON.stringify(Array.from(obj));";

        Utils.assertWithAllModes_ES6("[\"a\",\"b\"]", script);
    }

    @Test
    public void testArrayFromWithEmptyIterator() {
        // Test that empty iterator takes precedence over array-like properties
        String script =
                "const obj = {"
                        + "    length: 2,"
                        + "    0: 'a',"
                        + "    1: 'b',"
                        + "    [Symbol.iterator]: function* () {}"
                        + "};"
                        + "JSON.stringify(Array.from(obj));";

        Utils.assertWithAllModes_ES6("[]", script);
    }
}
