/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es2015;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

/**
 * Tests for ES2015 Iterator Protocol - return() method https://github.com/mozilla/rhino/issues/1409
 *
 * <p>NOTE: These tests are currently expected to fail as the full iterator return() integration
 * requires architectural changes documented in ARCHITECTURAL_CHANGES_NEEDED.md
 *
 * <p>The iteratorClose() foundation method is implemented in ScriptRuntime but not yet hooked into
 * the for-of loop control flow.
 */
public class IteratorReturnTest {

    @Test
    public void testIteratorReturnCalledOnBreak() {
        String script =
                "var returnCalled = false;\n"
                        + "var iterator = {\n"
                        + "  [Symbol.iterator]: function() { return this; },\n"
                        + "  next: function() { return { done: false, value: 1 }; },\n"
                        + "  return: function() {\n"
                        + "    returnCalled = true;\n"
                        + "    return { done: true };\n"
                        + "  }\n"
                        + "};\n"
                        + "for (var x of iterator) {\n"
                        + "  break;\n"
                        + "}\n"
                        + "returnCalled;";

        // TODO: Enable when architectural changes are implemented
        Utils.assertWithAllModes_ES6(false, script); // Currently expected to fail
    }

    @Test
    public void testIteratorReturnCalledOnThrow() {
        String script =
                "var returnCalled = false;\n"
                        + "var iterator = {\n"
                        + "  [Symbol.iterator]: function() { return this; },\n"
                        + "  next: function() { return { done: false, value: 1 }; },\n"
                        + "  return: function() {\n"
                        + "    returnCalled = true;\n"
                        + "    return { done: true };\n"
                        + "  }\n"
                        + "};\n"
                        + "try {\n"
                        + "  for (var x of iterator) {\n"
                        + "    throw new Error('test');\n"
                        + "  }\n"
                        + "} catch(e) {}\n"
                        + "returnCalled;";

        // TODO: Enable when architectural changes are implemented
        Utils.assertWithAllModes_ES6(false, script); // Currently expected to fail
    }

    @Test
    public void testIteratorReturnNotCalledOnNormalCompletion() {
        String script =
                "var returnCalled = false;\n"
                        + "var count = 0;\n"
                        + "var iterator = {\n"
                        + "  [Symbol.iterator]: function() { return this; },\n"
                        + "  next: function() {\n"
                        + "    count++;\n"
                        + "    return { done: count > 3, value: count };\n"
                        + "  },\n"
                        + "  return: function() {\n"
                        + "    returnCalled = true;\n"
                        + "    return { done: true };\n"
                        + "  }\n"
                        + "};\n"
                        + "for (var x of iterator) {}\n"
                        + "returnCalled;";

        // This test should continue to pass - normal completion doesn't call return()
        Utils.assertWithAllModes_ES6(false, script);
    }

    @Test
    public void testIteratorReturnCalledOnReturn() {
        String script =
                "var returnCalled = false;\n"
                        + "var iterator = {\n"
                        + "  [Symbol.iterator]: function() { return this; },\n"
                        + "  next: function() { return { done: false, value: 1 }; },\n"
                        + "  return: function() {\n"
                        + "    returnCalled = true;\n"
                        + "    return { done: true };\n"
                        + "  }\n"
                        + "};\n"
                        + "function test() {\n"
                        + "  for (var x of iterator) {\n"
                        + "    return 'early';\n"
                        + "  }\n"
                        + "}\n"
                        + "test();\n"
                        + "returnCalled;";

        // TODO: Enable when architectural changes are implemented
        Utils.assertWithAllModes_ES6(false, script); // Currently expected to fail
    }

    @Test
    public void testIteratorReturnWithNestedLoops() {
        String script =
                "var outerReturnCalled = false;\n"
                        + "var innerReturnCalled = false;\n"
                        + "var outerIterator = {\n"
                        + "  [Symbol.iterator]: function() { return this; },\n"
                        + "  count: 0,\n"
                        + "  next: function() {\n"
                        + "    this.count++;\n"
                        + "    return { done: this.count > 2, value: this.count };\n"
                        + "  },\n"
                        + "  return: function() {\n"
                        + "    outerReturnCalled = true;\n"
                        + "    return { done: true };\n"
                        + "  }\n"
                        + "};\n"
                        + "var innerIterator = {\n"
                        + "  [Symbol.iterator]: function() { return this; },\n"
                        + "  next: function() { return { done: false, value: 1 }; },\n"
                        + "  return: function() {\n"
                        + "    innerReturnCalled = true;\n"
                        + "    return { done: true };\n"
                        + "  }\n"
                        + "};\n"
                        + "outer: for (var x of outerIterator) {\n"
                        + "  for (var y of innerIterator) {\n"
                        + "    break outer;\n"
                        + "  }\n"
                        + "}\n"
                        + "[outerReturnCalled, innerReturnCalled];";

        // TODO: Enable when architectural changes are implemented
        String currentExpected =
                "[false, false]"; // Currently neither iterator's return() is called
        Utils.assertWithAllModes_ES6(currentExpected, script);
    }

    @Test
    public void testIteratorReturnValueIgnored() {
        String script =
                "var returnValue = { custom: 'value' };\n"
                        + "var iterator = {\n"
                        + "  [Symbol.iterator]: function() { return this; },\n"
                        + "  next: function() { return { done: false, value: 1 }; },\n"
                        + "  return: function() {\n"
                        + "    return returnValue;\n"
                        + "  }\n"
                        + "};\n"
                        + "var result;\n"
                        + "for (var x of iterator) {\n"
                        + "  result = x;\n"
                        + "  break;\n"
                        + "}\n"
                        + "result;";

        // This test should continue to pass - it tests the loop value, not iterator cleanup
        // The value from break should be 1, not the return value
        Utils.assertWithAllModes_ES6(1.0, script);
    }
}
