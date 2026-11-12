/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.optimizer;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/**
 * A parameter of a direct call function used as a bare expression statement ({@code a;}). See
 * https://github.com/mozilla/rhino/issues/2570
 */
public class DirectCallParameterTest {

    @Test
    public void bareParameterStatement() {
        Utils.assertWithAllModes(
                "ok",
                Utils.lines(
                        "function f0() { return f1(1); }",
                        "function f1(a) { a; }",
                        "f0();",
                        "'ok';"));
    }

    @Test
    public void bareParameterStatementWithSeveralParameters() {
        Utils.assertWithAllModes(
                "undefined",
                Utils.lines(
                        "function f0() { return f1(1, 'x', {}); }",
                        "function f1(a, b, c) { a; b; c; }",
                        "typeof f0();"));
    }

    @Test
    public void bareParameterStatementWithNumberUse() {
        Utils.assertWithAllModes(
                6,
                Utils.lines(
                        "function f0() { return f1(3); }",
                        "function f1(a) { a; return a * 2; }",
                        "f0();"));
    }

    @Test
    public void bareParameterStatementDoesNotCallValueOf() {
        Utils.assertWithAllModes(
                "1|1",
                Utils.lines(
                        "var calls = 0;",
                        "var o = { valueOf: function() { calls++; return 2; } };",
                        "function f0() { return f1(o); }",
                        "function f1(a) { a; return a - 1; }",
                        "f0() + '|' + calls;"));
    }
}
