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
}
