/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.testutils.Utils;

class UndefinedOrNullThisInFunctionCallOrApplyTest {
    @Nested
    class LegacyVersion {
        @Test
        void applyStrictOrNonStrict() {
            // Legacy mode replaces missing this with global object always, strict or non-strict
            Utils.runWithAllModes(
                    cx -> {
                        cx.setLanguageVersion(Context.VERSION_1_7);
                        NativeArray arr =
                                (NativeArray)
                                        Evaluator.eval(
                                                cx,
                                                "function F2() {return this;};[this, F2.apply(), F2.apply(undefined), F2.apply(null)];");

                        assertSame(arr.get(0, arr), arr.get(1, arr));
                        assertSame(arr.get(0, arr), arr.get(2, arr));
                        assertSame(arr.get(0, arr), arr.get(3, arr));

                        arr =
                                (NativeArray)
                                        Evaluator.eval(
                                                cx,
                                                "function F2() {'use strict'; return this;};[this, F2.apply(), F2.apply(undefined), F2.apply(null)];");

                        assertSame(arr.get(0, arr), arr.get(1, arr));
                        assertSame(arr.get(0, arr), arr.get(2, arr));
                        assertSame(arr.get(0, arr), arr.get(3, arr));

                        return null;
                    });
        }

        @Test
        void applyBuiltIn() {
            Utils.runWithAllModes(
                    cx -> {
                        cx.setLanguageVersion(Context.VERSION_1_7);

                        Object res = Evaluator.eval("Array.prototype.at.apply(undefined);");
                        assertTrue(Undefined.isUndefined(res));

                        return null;
                    });
        }

        @Test
        void callStrictOrNonStrict() {
            // Legacy mode replaces missing this with global object always, strict or non strict
            Utils.runWithAllModes(
                    cx -> {
                        cx.setLanguageVersion(Context.VERSION_1_7);
                        NativeArray arr =
                                (NativeArray)
                                        Evaluator.eval(
                                                cx,
                                                "function F2() {return this;};[this, F2.call(), F2.call(undefined), F2.call(null)];");

                        assertSame(arr.get(0, arr), arr.get(1, arr));
                        assertSame(arr.get(0, arr), arr.get(2, arr));
                        assertSame(arr.get(0, arr), arr.get(3, arr));

                        arr =
                                (NativeArray)
                                        Evaluator.eval(
                                                cx,
                                                "function F2() {'use strict'; return this;};[this, F2.call(), F2.call(undefined), F2.call(null)];");

                        assertSame(arr.get(0, arr), arr.get(1, arr));
                        assertSame(arr.get(0, arr), arr.get(2, arr));
                        assertSame(arr.get(0, arr), arr.get(3, arr));

                        return null;
                    });
        }

        @Test
        void callBuiltIn() {
            Utils.runWithAllModes(
                    cx -> {
                        cx.setLanguageVersion(Context.VERSION_1_7);

                        Object res = Evaluator.eval("Array.prototype.at.call(undefined);");
                        assertTrue(Undefined.isUndefined(res));

                        return null;
                    });
        }

        @Test
        void unqualifiedCallBuiltIn() {
            // Legacy mode replaces missing this with global object for built-ins too
            Utils.runWithAllModes(
                    cx -> {
                        cx.setLanguageVersion(Context.VERSION_1_7);

                        Object res =
                                Evaluator.eval(
                                        cx,
                                        "var s = Object.prototype.valueOf; (function() { return s(); })() === this;");
                        assertSame(Boolean.TRUE, res);

                        return null;
                    });
        }
    }

    @Nested
    class ModernVersion {
        @Test
        void applyStrict() {
            // Preserves null/undefined arguments
            Utils.runWithAllModes(
                    cx -> {
                        cx.setLanguageVersion(Context.VERSION_1_8);
                        NativeArray arr =
                                (NativeArray)
                                        Evaluator.eval(
                                                cx,
                                                "function F2() {'use strict'; return this;};[this, F2.apply(), F2.apply(undefined), F2.apply(null)];");
                        assertNotEquals(arr.get(0, arr), arr.get(1, arr));
                        assertNotEquals(arr.get(0, arr), arr.get(2, arr));
                        assertNotEquals(arr.get(0, arr), arr.get(3, arr));
                        assertSame(Undefined.instance, arr.get(1, arr));
                        assertSame(Undefined.instance, arr.get(2, arr));
                        assertNull(arr.get(3, arr));

                        return null;
                    });
        }

        @Test
        void applyNonStrict() {
            // Replaces missing this with global object for non-strict functions
            Utils.runWithAllModes(
                    cx -> {
                        cx.setLanguageVersion(Context.VERSION_1_8);
                        NativeArray arr =
                                (NativeArray)
                                        Evaluator.eval(
                                                cx,
                                                "function F2() {return this;};[this, F2.apply(), F2.apply(undefined), F2.apply(null)];");

                        assertSame(arr.get(0, arr), arr.get(1, arr));
                        assertSame(arr.get(0, arr), arr.get(2, arr));
                        assertSame(arr.get(0, arr), arr.get(3, arr));

                        return null;
                    });
        }

        @Test
        void applyBuiltIn() {
            // Preserves null/undefined arguments, and thus throws an error
            Utils.runWithAllModes(
                    cx -> {
                        cx.setLanguageVersion(Context.VERSION_1_8);

                        assertThrows(
                                EcmaError.class,
                                () -> Evaluator.eval("Array.prototype.at.apply(undefined);"));

                        return null;
                    });
        }

        @Test
        void callStrict() {
            // Preserves null/undefined arguments
            Utils.runWithAllModes(
                    cx -> {
                        cx.setLanguageVersion(Context.VERSION_1_8);
                        NativeArray arr =
                                (NativeArray)
                                        Evaluator.eval(
                                                cx,
                                                "function F2() {'use strict'; return this;};[this, F2.call(), F2.call(undefined), F2.call(null)];");

                        assertNotEquals(arr.get(0, arr), arr.get(1, arr));
                        assertNotEquals(arr.get(0, arr), arr.get(2, arr));
                        assertNotEquals(arr.get(0, arr), arr.get(3, arr));
                        assertSame(Undefined.instance, arr.get(1, arr));
                        assertSame(Undefined.instance, arr.get(2, arr));
                        assertNull(arr.get(3));

                        return null;
                    });
        }

        @Test
        void callNonStrict() {
            // Replaces missing this with global object for non-strict functions
            Utils.runWithAllModes(
                    cx -> {
                        cx.setLanguageVersion(Context.VERSION_1_8);
                        NativeArray arr =
                                (NativeArray)
                                        Evaluator.eval(
                                                cx,
                                                "function F2() {return this;};[this, F2.call(), F2.call(undefined), F2.call(null)];");

                        assertSame(arr.get(0, arr), arr.get(1, arr));
                        assertSame(arr.get(0, arr), arr.get(2, arr));
                        assertSame(arr.get(0, arr), arr.get(3, arr));

                        return null;
                    });
        }

        @Test
        void callBuiltIn() {
            // Preserves null/undefined arguments, and thus throws an error
            Utils.runWithAllModes(
                    cx -> {
                        cx.setLanguageVersion(Context.VERSION_1_8);

                        assertThrows(
                                EcmaError.class,
                                () -> Evaluator.eval("Array.prototype.at.call(undefined);"));

                        return null;
                    });
        }

        @Test
        void unqualifiedCallBuiltIn() {
            // Preserves undefined this, and thus throws an error
            Utils.runWithAllModes(
                    cx -> {
                        cx.setLanguageVersion(Context.VERSION_1_8);

                        assertThrows(
                                EcmaError.class,
                                () ->
                                        Evaluator.eval(
                                                "var s = Object.prototype.valueOf; (function() { return s(); })();"));

                        return null;
                    });
        }
    }
}
