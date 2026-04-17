/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.testutils.Utils;

/**
 * Tests for {@code for await ... of ...} and {@link
 * org.mozilla.javascript.SymbolKey#ASYNC_ITERATOR}.
 */
public class ForAwaitOfTest {

    /**
     * Evaluates {@code setup}, flushes microtasks so any async functions complete, then evaluates
     * {@code finalExpr} and returns its value as a string.
     */
    private static void assertAsyncResult(String expected, String setup, String finalExpr) {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    TopLevel scope = cx.initStandardObjects();
                    cx.evaluateString(scope, setup, "setup", 1, null);
                    cx.processMicrotasks();
                    Object result = cx.evaluateString(scope, finalExpr, "read", 1, null);
                    assertEquals(expected, Context.toString(result));
                    return null;
                });
    }

    @Test
    public void symbolAsyncIteratorIsWellKnown() {
        Utils.assertWithAllModes_ES6("symbol", "typeof Symbol.asyncIterator");
        Utils.assertWithAllModes_ES6(true, "Symbol.asyncIterator === Symbol.asyncIterator");
        Utils.assertWithAllModes_ES6(false, "Symbol.asyncIterator === Symbol.iterator");
    }

    @Test
    public void forAwaitOfOverAsyncIterable() {
        assertAsyncResult(
                "0,1,2",
                "var out = [];\n"
                        + "var obj = {\n"
                        + "  [Symbol.asyncIterator]() {\n"
                        + "    var i = 0;\n"
                        + "    return {\n"
                        + "      next() {\n"
                        + "        if (i < 3) return Promise.resolve({value: i++, done: false});\n"
                        + "        return Promise.resolve({value: undefined, done: true});\n"
                        + "      }\n"
                        + "    };\n"
                        + "  }\n"
                        + "};\n"
                        + "async function run() { for await (let v of obj) out.push(v); }\n"
                        + "run();",
                "out.join(',')");
    }

    @Test
    public void forAwaitOfFallsBackToSyncIterator() {
        assertAsyncResult(
                "10,20,30",
                "var out = [];\n"
                        + "async function run() {\n"
                        + "  for await (let v of [10, 20, 30]) out.push(v);\n"
                        + "}\n"
                        + "run();",
                "out.join(',')");
    }

    @Test
    public void forAwaitOfSupportsBreak() {
        assertAsyncResult(
                "1,2",
                "var out = [];\n"
                        + "async function run() {\n"
                        + "  for await (let v of [1, 2, 3, 4]) {\n"
                        + "    if (v === 3) break;\n"
                        + "    out.push(v);\n"
                        + "  }\n"
                        + "}\n"
                        + "run();",
                "out.join(',')");
    }

    @Test
    public void forAwaitOfSupportsContinue() {
        assertAsyncResult(
                "1,3",
                "var out = [];\n"
                        + "async function run() {\n"
                        + "  for await (let v of [1, 2, 3]) {\n"
                        + "    if (v === 2) continue;\n"
                        + "    out.push(v);\n"
                        + "  }\n"
                        + "}\n"
                        + "run();",
                "out.join(',')");
    }

    @Test
    public void forAwaitOfNullThrowsTypeError() {
        assertAsyncResult(
                "TypeError",
                "var caught = null;\n"
                        + "async function run() {\n"
                        + "  try { for await (let v of null) {} } catch (e) { caught = e.name; }\n"
                        + "}\n"
                        + "run();",
                "caught");
    }

    @Test
    public void forAwaitOutsideAsyncFunctionIsSyntaxError() {
        Utils.assertEvaluatorExceptionES6(
                "await is only allowed in async functions.",
                "function f() { for await (let x of []) {} }");
    }

    @Test
    public void forAwaitWithInIsSyntaxError() {
        Utils.assertEvaluatorExceptionES6(
                "for-await-of is only valid with 'of', not 'in'.",
                "async function f() { for await (let x in {}) {} }");
    }

    @Test
    public void forAwaitOfPrefersAsyncIteratorOverSyncIterator() {
        assertAsyncResult(
                "async",
                "var obj = {\n"
                        + "  [Symbol.iterator]() { throw new Error('should not be called'); },\n"
                        + "  [Symbol.asyncIterator]() {\n"
                        + "    var done = false;\n"
                        + "    return { next() {\n"
                        + "      if (done) return Promise.resolve({value: undefined, done: true});\n"
                        + "      done = true;\n"
                        + "      return Promise.resolve({value: 'async', done: false});\n"
                        + "    } };\n"
                        + "  }\n"
                        + "};\n"
                        + "var seen = null;\n"
                        + "async function run() { for await (let v of obj) seen = v; }\n"
                        + "run();",
                "seen");
    }
}
