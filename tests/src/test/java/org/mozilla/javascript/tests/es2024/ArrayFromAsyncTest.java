package org.mozilla.javascript.tests.es2024;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

/**
 * Tests for ES2024 Array.fromAsync
 *
 * <p>Note: Full async testing requires manual promise queue processing in Rhino. These tests verify
 * basic functionality.
 */
public class ArrayFromAsyncTest {

    @Test
    public void testArrayFromAsyncExists() {
        String script = "typeof Array.fromAsync === 'function';";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void testArrayFromAsyncReturnsPromise() {
        String script = "Array.fromAsync([1, 2, 3]) instanceof Promise;";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void testArrayFromAsyncWithEmptyArray() {
        String script = "Array.fromAsync([]) instanceof Promise;";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void testArrayFromAsyncWithPromiseArray() {
        String script =
                "Array.fromAsync([Promise.resolve(1), Promise.resolve(2)]) instanceof Promise;";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void testArrayFromAsyncWithIterable() {
        String script =
                "var iterable = {"
                        + "  [Symbol.iterator]: function* () { yield 1; yield 2; }"
                        + "};"
                        + "Array.fromAsync(iterable) instanceof Promise;";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void testArrayFromAsyncWithArrayLike() {
        String script =
                "var arrayLike = { 0: 'a', 1: 'b', length: 2 };"
                        + "Array.fromAsync(arrayLike) instanceof Promise;";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void testArrayFromAsyncWithMapper() {
        String script =
                "Array.fromAsync([1, 2], function(x) { return x * 2; }) instanceof Promise;";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void testArrayFromAsyncWithMapperAndThis() {
        String script =
                "var obj = { multiplier: 2 };"
                        + "Array.fromAsync([1, 2], function(x) { return x * this.multiplier; }, obj) instanceof Promise;";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void testArrayFromAsyncLength() {
        String script = "Array.fromAsync.length === 1;";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void testArrayFromAsyncName() {
        String script = "Array.fromAsync.name === 'fromAsync';";
        Utils.assertWithAllModes_ES6(true, script);
    }
}
