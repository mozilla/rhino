/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/**
 * Object literal evaluation-order tests. Particularly relevant to InterpreterV2's no-spread
 * optimization, which pre-populates literal-token property values out of source order; these tests
 * verify that the user-observable left-to-right ordering of side-effecting evaluations is preserved
 * in all execution modes.
 */
public class ObjectLiteralEvaluationOrderTest {

    @Test
    public void literalsInterleavedWithSideEffectingValues() {
        String script =
                "var log = [];\n"
                        + "var o = {a: 1, b: (log.push('b'), 10), c: 2, d: (log.push('d'), 20)};\n"
                        + "log.join(',') + '|' + o.a + ',' + o.b + ',' + o.c + ',' + o.d";
        Utils.assertWithAllModes_ES6("b,d|1,10,2,20", script);
    }

    @Test
    public void literalAfterNonLiteralValue() {
        String script =
                "var log = [];\n"
                        + "var o = {a: (log.push('a'), 1), b: 2};\n"
                        + "log.join(',') + '|' + o.a + ',' + o.b";
        Utils.assertWithAllModes_ES6("a|1,2", script);
    }

    @Test
    public void computedKeyBetweenLiterals() {
        String script =
                "var log = [];\n"
                        + "function k() { log.push('k'); return 'kk'; }\n"
                        + "var o = {a: 1, [k()]: 'kv', b: 2};\n"
                        + "log.join(',') + '|' + o.a + ',' + o.kk + ',' + o.b";
        Utils.assertWithAllModes_ES6("k|1,kv,2", script);
    }

    @Test
    public void computedKeyDuplicateOverwriteOrder() {
        String script = "var o = {a: 'first', [\"a\"]: 'second', b: 3};\n" + "o.a + ',' + o.b";
        Utils.assertWithAllModes_ES6("second,3", script);
    }

    @Test
    public void allLiteralsRoundTrip() {
        String script =
                "var o = {a: 1, b: 'two', c: null, d: true, e: false, f: undefined};\n"
                        + "Object.keys(o).join(',') + '|' + o.a + ',' + o.b + ',' + (o.c === null)"
                        + " + ',' + o.d + ',' + o.e + ',' + (o.f === undefined)";
        Utils.assertWithAllModes_ES6("a,b,c,d,e,f|1,two,true,true,false,true", script);
    }

    @Test
    public void methodWithLiteralsAround() {
        String script =
                "var o = {a: 1, m() { return this.a + this.b; }, b: 2};\n"
                        + "o.m() + ':' + Object.keys(o).join(',')";
        Utils.assertWithAllModes_ES6("3:a,m,b", script);
    }

    @Test
    public void computedKeyWithLiteralValue() {
        String script =
                "var log = [];\n"
                        + "function k() { log.push('k'); return 'kk'; }\n"
                        + "var o = {[k()]: 1};\n"
                        + "log.join(',') + '|' + o.kk";
        Utils.assertWithAllModes_ES6("k|1", script);
    }

    @Test
    public void computedKeyWithLiteralValueAmongOthers() {
        String script =
                "var log = [];\n"
                        + "function k() { log.push('k'); return 'kk'; }\n"
                        + "function v() { log.push('v'); return 99; }\n"
                        + "var o = {a: 1, [k()]: 'lit', b: v(), c: 2};\n"
                        + "log.join(',') + '|' + o.a + ',' + o.kk + ',' + o.b + ',' + o.c";
        Utils.assertWithAllModes_ES6("k,v|1,lit,99,2", script);
    }

    @Test
    public void getterSetterWithLiteralsAround() {
        String script =
                "var o = {a: 1, get x() { return 100 + this.a; }, set x(v) { this.a = v; }, b:"
                        + " 2};\n"
                        + "var first = o.x;\n"
                        + "o.x = 5;\n"
                        + "first + ':' + o.x + ':' + o.b";
        Utils.assertWithAllModes_ES6("101:105:2", script);
    }
}
