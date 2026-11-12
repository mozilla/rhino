/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.optimizer;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/**
 * Equality between a direct call parameter that is also used in a number context and a number
 * literal. See https://github.com/mozilla/rhino/issues/2407
 */
public class DirectCallStrictEqualityTest {

    // g(a) passes `a` on as a number, so f keeps the parameter as a double.
    private static final String PREFIX = "function g(a) { }\n";

    private static void check(String expected, String fBody, String args) {
        Utils.assertWithAllModes(
                expected,
                PREFIX
                        + "function f(a) { var r = "
                        + fBody
                        + "; g(a); return r; }\n"
                        + "function run() { return ["
                        + args
                        + "].join(); }\n"
                        + "run();");
    }

    @Test
    public void issueExample() {
        Utils.assertWithAllModes(
                "true 0",
                Utils.lines(
                        "var out;",
                        "function funcA(a) { }",
                        "function funcB(a) {",
                        "  if (a === 0) { out = 'true ' + a; } else { out = 'false ' + a; }",
                        "  funcA(a);",
                        "}",
                        "(function funcC() { funcB(0); })();",
                        "out;"));
    }

    @Test
    public void strictEquals() {
        check("true,false,true,false,false", "a === 0", "f(0), f(1), f(-0), f(NaN), f('0')");
    }

    @Test
    public void strictNotEquals() {
        check("false,true,false,true,true", "a !== 0", "f(0), f(1), f(-0), f(NaN), f('0')");
    }

    @Test
    public void looseEquals() {
        check("true,false,true,false,true", "a == 0", "f(0), f(1), f(-0), f(NaN), f('0')");
    }

    @Test
    public void looseNotEquals() {
        check("false,true,false,true,false", "a != 0", "f(0), f(1), f(-0), f(NaN), f('0')");
    }

    @Test
    public void strictEqualsInCondition() {
        check("yes,no,no", "a === 2.5 ? 'yes' : 'no'", "f(2.5), f(2), f('2.5')");
    }
}
