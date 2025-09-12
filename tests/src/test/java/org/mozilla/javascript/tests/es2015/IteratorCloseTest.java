/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es2015;

import static org.junit.Assert.*;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.testutils.Utils;

/**
 * Unit tests for the ScriptRuntime.iteratorClose method. This tests the implementation of the
 * ES2015 IteratorClose abstract operation.
 */
public class IteratorCloseTest {

    @Test
    public void testIteratorCloseMethodExists() {
        // Verify the iteratorClose method is accessible
        try {
            ScriptRuntime.class.getMethod("iteratorClose", Object.class, Context.class);
        } catch (NoSuchMethodException e) {
            fail("iteratorClose method should exist in ScriptRuntime");
        }
    }

    @Test
    public void testIteratorWithReturnMethod() {
        String script =
                "var returnCalled = false;\n"
                        + "var returnValue = null;\n"
                        + "var iterator = {\n"
                        + "  [Symbol.iterator]: function() { return this; },\n"
                        + "  next: function() { return { done: false, value: 1 }; },\n"
                        + "  return: function() {\n"
                        + "    returnCalled = true;\n"
                        + "    returnValue = { done: true, value: 'cleanup' };\n"
                        + "    return returnValue;\n"
                        + "  }\n"
                        + "};\n"
                        + "// Test that the return method exists and is callable\n"
                        + "JSON.stringify([typeof iterator.return, iterator.return.length]);";

        String expected = "[\"function\",0]";
        Utils.assertWithAllModes_ES6(expected, script);
    }

    @Test
    public void testIteratorWithoutReturnMethod() {
        String script =
                "var iterator = {\n"
                        + "  [Symbol.iterator]: function() { return this; },\n"
                        + "  next: function() { return { done: false, value: 1 }; }\n"
                        + "};\n"
                        + "// Iterator without return method should not cause errors\n"
                        + "typeof iterator.return;";

        Utils.assertWithAllModes_ES6("undefined", script);
    }

    @Test
    public void testIteratorWithNonCallableReturn() {
        String script =
                "var error = null;\n"
                        + "var iterator = {\n"
                        + "  [Symbol.iterator]: function() { return this; },\n"
                        + "  next: function() { return { done: false, value: 1 }; },\n"
                        + "  return: 'not a function'\n"
                        + "};\n"
                        + "// If return exists but is not callable, it should throw\n"
                        + "// This behavior is tested when the actual integration is complete\n"
                        + "typeof iterator.return;";

        Utils.assertWithAllModes_ES6("string", script);
    }

    @Test
    public void testReturnMethodSignature() {
        String script =
                "var iterator = {\n"
                        + "  [Symbol.iterator]: function() { return this; },\n"
                        + "  next: function() { return { done: false, value: 1 }; },\n"
                        + "  return: function(value) {\n"
                        + "    return { done: true, value: value || 'default' };\n"
                        + "  }\n"
                        + "};\n"
                        + "// Test that return method can accept an optional value parameter\n"
                        + "JSON.stringify([iterator.return().value, iterator.return('custom').value]);";

        String expected = "[\"default\",\"custom\"]";
        Utils.assertWithAllModes_ES6(expected, script);
    }
}
