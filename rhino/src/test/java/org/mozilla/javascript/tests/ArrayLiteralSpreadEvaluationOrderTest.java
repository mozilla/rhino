/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/** Array literals with spread must evaluate elements and consume iterators in source order. */
public class ArrayLiteralSpreadEvaluationOrderTest {

    @Test
    public void spreadConsumedBeforeLaterElements() {
        String script =
                "let count = 0;\n"
                        + "function f() { return count++; }\n"
                        + "let order = [];\n"
                        + "const src = { [Symbol.iterator]() { order.push('consume');"
                        + " return [1][Symbol.iterator](); } };\n"
                        + "let res = [...src, f(), f(), f(), (order.push('after'), 0)];\n"
                        + "order.join(',') + '|' + res.join(',')";
        Utils.assertWithAllModes_ES6("consume,after|1,0,1,2,0", script);
    }

    @Test
    public void elementsOnBothSidesOfSpread() {
        String script =
                "let order = [];\n"
                        + "function t(x) { order.push(x); return x; }\n"
                        + "const src = { [Symbol.iterator]() { order.push('consume');"
                        + " return [10, 20][Symbol.iterator](); } };\n"
                        + "let res = [t('a'), ...src, t('b')];\n"
                        + "order.join(',') + '|' + res.join(',')";
        Utils.assertWithAllModes_ES6("a,consume,b|a,10,20,b", script);
    }

    @Test
    public void throwingIteratorStopsLaterElements() {
        String script =
                "let order = [];\n"
                        + "const src = { [Symbol.iterator]() { throw new Error('boom'); } };\n"
                        + "try { [...src, order.push('after')]; } catch (e) { order.push(e.message); }\n"
                        + "order.join(',')";
        Utils.assertWithAllModes_ES6("boom", script);
    }

    @Test
    public void holesWithSpread() {
        String script =
                "let a = [1, 2];\n"
                        + "let r = [, ...a, , 3];\n"
                        + "r.length + '|' + (0 in r) + (1 in r) + (2 in r) + (3 in r) + (4 in r)";
        Utils.assertWithAllModes_ES6("5|falsetruetruefalsetrue", script);
    }

    @Test
    public void emptySpreadBeforeHole() {
        String script = "let r = [...[], , 1];\n" + "r.length + '|' + (0 in r) + (1 in r)";
        Utils.assertWithAllModes_ES6("2|falsetrue", script);
    }

    @Test
    public void nestedSpreadLiterals() {
        String script =
                "let order = [];\n"
                        + "function t(x) { order.push(x); return x; }\n"
                        + "let r = [t(1), ...[t(2), ...[t(3)], t(4)], t(5)];\n"
                        + "order.join(',') + '|' + r.join(',')";
        Utils.assertWithAllModes_ES6("1,2,3,4,5|1,2,3,4,5", script);
    }
}
