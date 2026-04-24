/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.testutils.Utils;

/** Tests for {@code async function*} async generator functions. */
public class AsyncGeneratorTest {

    /**
     * Evaluates {@code setup}, flushes microtasks so any pending async work completes, then
     * evaluates {@code finalExpr} and asserts its stringified value.
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
    public void asyncGeneratorHasAsyncIteratorSymbol() {
        Utils.assertWithAllModes_ES6(
                "function",
                "async function* g() {}\n" + "var it = g();\n" + "typeof it[Symbol.asyncIterator]");
    }

    @Test
    public void asyncGeneratorSelfIsAsyncIterator() {
        Utils.assertWithAllModes_ES6(
                true,
                "async function* g() {}\n"
                        + "var it = g();\n"
                        + "it[Symbol.asyncIterator]() === it");
    }

    @Test
    public void basicYieldSequence() {
        assertAsyncResult(
                "1,2,3",
                "var out = [];\n"
                        + "async function* g() { yield 1; yield 2; yield 3; }\n"
                        + "async function consume() {\n"
                        + "  for await (let v of g()) out.push(v);\n"
                        + "}\n"
                        + "consume();",
                "out.join(',')");
    }

    @Test
    public void awaitInsideAsyncGenerator() {
        assertAsyncResult(
                "10,21",
                "var out = [];\n"
                        + "async function* g() {\n"
                        + "  var a = await Promise.resolve(10);\n"
                        + "  yield a;\n"
                        + "  var b = await Promise.resolve(20);\n"
                        + "  yield b + 1;\n"
                        + "}\n"
                        + "async function consume() {\n"
                        + "  for await (let v of g()) out.push(v);\n"
                        + "}\n"
                        + "consume();",
                "out.join(',')");
    }

    @Test
    public void yieldedPromiseIsAwaited() {
        assertAsyncResult(
                "42",
                "var out = [];\n"
                        + "async function* g() { yield Promise.resolve(42); }\n"
                        + "async function consume() { for await (let v of g()) out.push(v); }\n"
                        + "consume();",
                "out.join(',')");
    }

    @Test
    public void nextReturnsPromiseOfIteratorResult() {
        assertAsyncResult(
                "true|1|false",
                "var firstIsPromise, firstValue, firstDone;\n"
                        + "async function* g() { yield 1; }\n"
                        + "var it = g();\n"
                        + "var p = it.next();\n"
                        + "firstIsPromise = p instanceof Promise;\n"
                        + "p.then(r => { firstValue = r.value; firstDone = r.done; });",
                "firstIsPromise + '|' + firstValue + '|' + firstDone");
    }

    @Test
    public void returnMethodCompletesGenerator() {
        assertAsyncResult(
                "1|early,true",
                "var firstVal, retVal, retDone;\n"
                        + "async function* g() { yield 1; yield 2; }\n"
                        + "var it = g();\n"
                        + "it.next().then(r => { firstVal = r.value; });\n"
                        + "it.return('early').then(r => { retVal = r.value; retDone = r.done; });",
                "firstVal + '|' + retVal + ',' + retDone");
    }

    @Test
    public void throwMethodDeliversErrorToGenerator() {
        assertAsyncResult(
                "caught:boom",
                "var result = null;\n"
                        + "async function* g() {\n"
                        + "  try { yield 1; }\n"
                        + "  catch (e) { yield 'caught:' + e; }\n"
                        + "}\n"
                        + "var it = g();\n"
                        + "it.next().then(r => {});\n"
                        + "it.throw('boom').then(r => { result = r.value; });",
                "result");
    }

    @Test
    public void requestsAreQueued() {
        // Three next() calls before any has resolved; each should get a distinct value.
        assertAsyncResult(
                "a,b,c",
                "var vals = [];\n"
                        + "async function* g() { yield 'a'; yield 'b'; yield 'c'; }\n"
                        + "var it = g();\n"
                        + "it.next().then(r => vals.push(r.value));\n"
                        + "it.next().then(r => vals.push(r.value));\n"
                        + "it.next().then(r => vals.push(r.value));",
                "vals.join(',')");
    }

    @Test
    public void generatorRespectsAwaitOrdering() {
        // Each await point yields to the microtask queue; subsequent pushes must wait.
        assertAsyncResult(
                "A,B,C",
                "var out = [];\n"
                        + "async function* g() {\n"
                        + "  yield await Promise.resolve('A');\n"
                        + "  yield await Promise.resolve('B');\n"
                        + "  yield await Promise.resolve('C');\n"
                        + "}\n"
                        + "async function consume() { for await (let v of g()) out.push(v); }\n"
                        + "consume();",
                "out.join(',')");
    }

    @Test
    public void asyncGeneratorFunctionConstructorName() {
        // Already asserted in AsyncFunctionPrototypeTest but keep as a Phase 2 smoke test.
        Utils.assertWithAllModes_ES6(
                "AsyncGeneratorFunction", "(async function*(){}).constructor.name");
    }

    @Test
    public void yieldStarAsyncIterableDelegates() {
        assertAsyncResult(
                "1,2,3",
                "var out = [];\n"
                        + "async function* inner() { yield 1; yield 2; yield 3; }\n"
                        + "async function* outer() { yield* inner(); }\n"
                        + "async function consume() { for await (var v of outer()) out.push(v); }\n"
                        + "consume();",
                "out.join(',')");
    }

    @Test
    public void yieldStarPrefersAsyncIteratorWhenBothPresent() {
        assertAsyncResult(
                "async-1,async-2,async-3",
                "var out = [];\n"
                        + "var iterable = {\n"
                        + "  [Symbol.asyncIterator]() {\n"
                        + "    var i = 0;\n"
                        + "    return { next: () => Promise.resolve({ value: 'async-' + (++i),"
                        + " done: i > 3 }) };\n"
                        + "  },\n"
                        + "  [Symbol.iterator]() {\n"
                        + "    var i = 0;\n"
                        + "    return { next: () => ({ value: 'sync-' + (++i), done: i > 3 }) };\n"
                        + "  }\n"
                        + "};\n"
                        + "async function* g() { yield* iterable; }\n"
                        + "async function consume() { for await (var v of g()) out.push(v); }\n"
                        + "consume();",
                "out.join(',')");
    }

    @Test
    public void yieldStarFallsBackToSymbolIterator() {
        assertAsyncResult(
                "sync-1,sync-2",
                "var out = [];\n"
                        + "var iterable = {\n"
                        + "  [Symbol.iterator]() {\n"
                        + "    var i = 0;\n"
                        + "    return { next: () => ({ value: 'sync-' + (++i), done: i > 2 }) };\n"
                        + "  }\n"
                        + "};\n"
                        + "async function* g() { yield* iterable; }\n"
                        + "async function consume() { for await (var v of g()) out.push(v); }\n"
                        + "consume();",
                "out.join(',')");
    }

    @Test
    public void yieldStarFallsBackWhenAsyncIteratorIsUndefined() {
        // Explicitly-undefined Symbol.asyncIterator should fall through to Symbol.iterator.
        assertAsyncResult(
                "a,b",
                "var out = [];\n"
                        + "var iterable = {\n"
                        + "  [Symbol.asyncIterator]: undefined,\n"
                        + "  [Symbol.iterator]() {\n"
                        + "    var i = 0, vals = ['a', 'b'];\n"
                        + "    return { next: () => ({ value: vals[i], done: i++ >= vals.length })"
                        + " };\n"
                        + "  }\n"
                        + "};\n"
                        + "async function* g() { yield* iterable; }\n"
                        + "async function consume() { for await (var v of g()) out.push(v); }\n"
                        + "consume();",
                "out.join(',')");
    }

    @Test
    public void yieldStarRejectedPromiseThrowsIntoGenerator() {
        assertAsyncResult(
                "1,caught:boom",
                "var out = [];\n"
                        + "var iterable = {\n"
                        + "  [Symbol.asyncIterator]() {\n"
                        + "    var i = 0;\n"
                        + "    return {\n"
                        + "      next() {\n"
                        + "        i++;\n"
                        + "        if (i === 2) return Promise.reject('boom');\n"
                        + "        return Promise.resolve({ value: i, done: false });\n"
                        + "      }\n"
                        + "    };\n"
                        + "  }\n"
                        + "};\n"
                        + "async function* g() {\n"
                        + "  try { yield* iterable; }\n"
                        + "  catch (e) { yield 'caught:' + e; }\n"
                        + "}\n"
                        + "async function consume() { for await (var v of g()) out.push(v); }\n"
                        + "consume();",
                "out.join(',')");
    }

    @Test
    public void yieldStarReturnClosesAsyncDelegee() {
        assertAsyncResult(
                "a|done|true",
                "var firstVal, retVal, retDone;\n"
                        + "async function* delegatee() { yield 'a'; yield 'b'; }\n"
                        + "async function* outer() { yield* delegatee(); }\n"
                        + "var it = outer();\n"
                        + "it.next().then(r => { firstVal = r.value; });\n"
                        + "it.return('done').then(r => { retVal = r.value; retDone = r.done; });",
                "firstVal + '|' + retVal + '|' + retDone");
    }

    @Test
    public void yieldStarThrowForwardsToAsyncDelegee() {
        assertAsyncResult(
                "a|caught:boom",
                "var firstVal, secondVal;\n"
                        + "async function* delegatee() {\n"
                        + "  try { yield 'a'; yield 'b'; }\n"
                        + "  catch (e) { yield 'caught:' + e; }\n"
                        + "}\n"
                        + "async function* outer() { yield* delegatee(); }\n"
                        + "var it = outer();\n"
                        + "it.next().then(r => { firstVal = r.value; });\n"
                        + "it.throw('boom').then(r => { secondVal = r.value; });",
                "firstVal + '|' + secondVal");
    }
}
