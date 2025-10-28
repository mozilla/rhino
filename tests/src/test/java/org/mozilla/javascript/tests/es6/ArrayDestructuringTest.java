/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

public class ArrayDestructuringTest {

    /** Test for issue #662. There was a ClassCastException. */
    @Test
    public void strangeCase() {
        Utils.assertWithAllModes("", "var a = ''; [a.x] = ''; a");
    }

    /** Test for issue #662. */
    @Test
    public void strangeCase2() {
        Utils.assertWithAllModes("", "[1..h]=''");
    }

    /** Test for issue #662. */
    @Test
    public void strangeCase3() {
        Utils.assertWithAllModes(123, "[0..toString.h] = [123]; Number.prototype.toString.h");
    }

    /** Test nested array destructuring with default values in arrow function parameters. */
    @Test
    public void nestedArrayDestructuringWithDefaults() {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                    org.mozilla.javascript.ScriptableObject scope = cx.initStandardObjects();

                    String script =
                            "assert = { sameValue(a, b, e) { if (a !== b) throw e; } };\n"
                                    + "var callCount = 0;\n"
                                    + "var f;\n"
                                    + "f = ([[x, y, z] = [4, 5, 6]]) => {\n"
                                    + "    assert.sameValue(x, 7);\n"
                                    + "    assert.sameValue(y, 8);\n"
                                    + "    assert.sameValue(z, 9);\n"
                                    + "    callCount = callCount + 1;\n"
                                    + "};\n"
                                    + "f([[7, 8, 9]]);\n"
                                    + "assert.sameValue(callCount, 1, 'arrow function invoked exactly once');";

                    cx.evaluateString(scope, script, "test", 1, null);
                    return null;
                });
    }

    /** Test nested array destructuring with default values being used when argument is missing. */
    @Test
    public void nestedArrayDestructuringWithDefaultsUsed() {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                    org.mozilla.javascript.ScriptableObject scope = cx.initStandardObjects();

                    String script =
                            "assert = { sameValue(a, b, e) { if (a !== b) throw e; } };\n"
                                    + "var f = ([[x, y, z] = [4, 5, 6]]) => {\n"
                                    + "    assert.sameValue(x, 4);\n"
                                    + "    assert.sameValue(y, 5);\n"
                                    + "    assert.sameValue(z, 6);\n"
                                    + "};\n"
                                    + "f([]);\n";

                    cx.evaluateString(scope, script, "test", 1, null);
                    return null;
                });
    }

    /** Test nested object destructuring with default values in arrow function parameters. */
    @Test
    public void nestedObjectDestructuringWithDefaults() {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                    org.mozilla.javascript.ScriptableObject scope = cx.initStandardObjects();

                    String script =
                            "assert = { sameValue(a, b, e) { if (a !== b) throw e; } };\n"
                                    + "var f = ([{x, y} = {x: 4, y: 5}]) => {\n"
                                    + "    assert.sameValue(x, 7);\n"
                                    + "    assert.sameValue(y, 8);\n"
                                    + "};\n"
                                    + "f([{x: 7, y: 8}]);\n";

                    cx.evaluateString(scope, script, "test", 1, null);
                    return null;
                });
    }

    /** Test multiple nested arrays with defaults. */
    @Test
    public void multipleNestedArraysWithDefaults() {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                    org.mozilla.javascript.ScriptableObject scope = cx.initStandardObjects();

                    String script =
                            "assert = { sameValue(a, b, e) { if (a !== b) throw e; } };\n"
                                    + "var f = ([[a, b] = [1, 2], [c, d] = [3, 4]]) => {\n"
                                    + "    assert.sameValue(a, 10);\n"
                                    + "    assert.sameValue(b, 20);\n"
                                    + "    assert.sameValue(c, 30);\n"
                                    + "    assert.sameValue(d, 40);\n"
                                    + "};\n"
                                    + "f([[10, 20], [30, 40]]);\n";

                    cx.evaluateString(scope, script, "test", 1, null);
                    return null;
                });
    }

    /**
     * Test that default value is NOT used when element exists (even if empty array). Based on
     * test262: language/statements/const/dstr/ary-ptrn-elem-ary-elision-iter.js
     */
    @Test
    public void nestedArrayDefaultNotUsedWhenElementExists() {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                    org.mozilla.javascript.ScriptableObject scope = cx.initStandardObjects();

                    String script =
                            "var callCount = 0;\n"
                                    + "function g() {\n"
                                    + "  callCount += 1;\n"
                                    + "  return [];\n"
                                    + "}\n"
                                    + "const [[,] = g()] = [[]];\n"
                                    + "if (callCount !== 0) throw new Error('Expected callCount to be 0, got ' + callCount);";

                    cx.evaluateString(scope, script, "test", 1, null);
                    return null;
                });
    }

    /** Test that array destructuring of non-iterable objects throws TypeError. */
    @Test
    public void arrayDestructuringNonIterableThrows() {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                    org.mozilla.javascript.ScriptableObject scope = cx.initStandardObjects();

                    String script =
                            "var errorThrown = false;\n"
                                    + "function f([x = 1, y = 2] = {x: 3, y: 4}) {\n"
                                    + "  return x + y;\n"
                                    + "}\n"
                                    + "try {\n"
                                    + "  f();\n"
                                    + "} catch (e) {\n"
                                    + "  if (e instanceof TypeError) {\n"
                                    + "    errorThrown = true;\n"
                                    + "  }\n"
                                    + "}\n"
                                    + "if (!errorThrown) throw new Error('Expected TypeError to be thrown');";

                    cx.evaluateString(scope, script, "test", 1, null);
                    return null;
                });
    }

    /** Test that array destructuring uses Symbol.iterator when present. */
    @Test
    public void arrayDestructuringUsesSymbolIterator() {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                    org.mozilla.javascript.ScriptableObject scope = cx.initStandardObjects();

                    String script =
                            "function f([x = 1, y = 2] = {x: 100, y: 200,\n"
                                    + "                              [Symbol.iterator]: function*() {\n"
                                    + "                                yield 3;\n"
                                    + "                                yield 4;\n"
                                    + "                              }}) {\n"
                                    + "  return x + y;\n"
                                    + "}\n"
                                    + "var result = f();\n"
                                    + "if (result !== 7) throw new Error('Expected 7, got ' + result);";

                    cx.evaluateString(scope, script, "test", 1, null);
                    return null;
                });
    }

    /** Test that errors thrown by Symbol.iterator propagate correctly. */
    @Test
    public void arrayDestructuringIteratorErrorPropagates() {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                    org.mozilla.javascript.ScriptableObject scope = cx.initStandardObjects();

                    String script =
                            "function Test262Error() {}\n"
                                    + "var iter = {};\n"
                                    + "iter[Symbol.iterator] = function() {\n"
                                    + "  throw new Test262Error();\n"
                                    + "};\n"
                                    + "var f = ([x]) => {};\n"
                                    + "var errorThrown = false;\n"
                                    + "try {\n"
                                    + "  f(iter);\n"
                                    + "} catch (e) {\n"
                                    + "  if (e instanceof Test262Error) {\n"
                                    + "    errorThrown = true;\n"
                                    + "  }\n"
                                    + "}\n"
                                    + "if (!errorThrown) throw new Error('Expected Test262Error to be thrown');";

                    cx.evaluateString(scope, script, "test", 1, null);
                    return null;
                });
    }

    /** Test destructuring with defaults in for loop initialization. */
    @Test
    public void destructuringWithDefaultsInForLoop() {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                    org.mozilla.javascript.ScriptableObject scope = cx.initStandardObjects();

                    String script =
                            "function a() {}\n"
                                    + "var result = (function() {\n"
                                    + "  for (let {x = a()} = {}; ; ) {\n"
                                    + "    return 3;\n"
                                    + "  }\n"
                                    + "})();\n"
                                    + "if (result !== 3) throw new Error('Expected 3, got ' + result);";

                    cx.evaluateString(scope, script, "test", 1, null);
                    return null;
                });
    }

    /**
     * Test that iterator return() is NOT called when iterator is exhausted normally. Based on
     * test262: language/expressions/arrow-function/dstr/ary-init-iter-no-close.js
     */
    @Test
    public void arrayDestructuringIteratorNoCloseWhenExhausted() {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                    org.mozilla.javascript.ScriptableObject scope = cx.initStandardObjects();

                    String script =
                            "var doneCallCount = 0;\n"
                                    + "var iter = {};\n"
                                    + "iter[Symbol.iterator] = function() {\n"
                                    + "  return {\n"
                                    + "    next: function() {\n"
                                    + "      return { value: null, done: true };\n"
                                    + "    },\n"
                                    + "    return: function() {\n"
                                    + "      doneCallCount += 1;\n"
                                    + "      return {};\n"
                                    + "    }\n"
                                    + "  };\n"
                                    + "};\n"
                                    + "var f = ([x]) => {\n"
                                    + "  if (doneCallCount !== 0) throw new Error('return() should not be called');\n"
                                    + "};\n"
                                    + "f(iter);\n"
                                    + "if (doneCallCount !== 0) throw new Error('Expected doneCallCount to be 0, got ' + doneCallCount);";

                    cx.evaluateString(scope, script, "test", 1, null);
                    return null;
                });
    }

    /**
     * Test that array destructuring with holes respects prototype chain. Based on Mozilla test
     * js1_8/regress/regress-469625-02.js
     */
    @Test
    public void arrayDestructuringWithHolesAndPrototype() {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                    org.mozilla.javascript.ScriptableObject scope = cx.initStandardObjects();

                    String script =
                            "Array.prototype[1] = 'y';\n"
                                    + "var [x, y, z] = ['x', , 'z'];\n"
                                    + "if (y !== 'y') throw new Error('Expected y=\\'y\\' from prototype, got ' + y);";

                    cx.evaluateString(scope, script, "test", 1, null);
                    return null;
                });
    }

    /**
     * Test rest parameters in array destructuring assignment. Currently not supported - this test
     * documents expected behavior when implemented.
     */
    @org.junit.Ignore("Rest parameters in array destructuring not yet implemented")
    @Test
    public void arrayDestructuringWithRest() {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                    org.mozilla.javascript.ScriptableObject scope = cx.initStandardObjects();

                    String script =
                            "var a, b, rest;\n"
                                    + "[a, b, ...rest] = [10, 20, 30, 40, 50];\n"
                                    + "if (a !== 10) throw new Error('Expected a=10, got ' + a);\n"
                                    + "if (b !== 20) throw new Error('Expected b=20, got ' + b);\n"
                                    + "if (rest.length !== 3) throw new Error('Expected rest.length=3, got ' + rest.length);\n"
                                    + "if (rest[0] !== 30) throw new Error('Expected rest[0]=30, got ' + rest[0]);\n"
                                    + "if (rest[1] !== 40) throw new Error('Expected rest[1]=40, got ' + rest[1]);\n"
                                    + "if (rest[2] !== 50) throw new Error('Expected rest[2]=50, got ' + rest[2]);";

                    cx.evaluateString(scope, script, "test", 1, null);
                    return null;
                });
    }
}
