/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/**
 * {@code JSON.stringify} reads {@code toJSON} once, with a plain Get and no HasProperty check. See
 * https://github.com/mozilla/rhino/issues/2597
 */
public class JsonStringifyToJsonTest {

    @Test
    public void getterRunsOnce() {
        Utils.assertWithAllModes_ES6(
                "\"result\" 1",
                "var calls = 0, o = {};\n"
                        + "Object.defineProperty(o, 'toJSON', { get: function () {\n"
                        + "  calls++; return function () { return 'result'; };\n"
                        + "} });\n"
                        + "JSON.stringify(o) + ' ' + calls");
    }

    @Test
    public void callsTheFunctionFromTheFirstRead() {
        Utils.assertWithAllModes_ES6(
                "\"result\"",
                "var read = false, o = {};\n"
                        + "Object.defineProperty(o, 'toJSON', { get: function () {\n"
                        + "  if (read) return undefined;\n"
                        + "  read = true; return function () { return 'result'; };\n"
                        + "} });\n"
                        + "JSON.stringify(o)");
    }

    @Test
    public void proxyUsesOnlyTheGetTrap() {
        Utils.assertWithAllModes_ES6(
                "\"result\" get toJSON",
                "var trace = [];\n"
                        + "var p = new Proxy({ toJSON: function () { return 'result'; } }, {\n"
                        + "  has: function (t, k) { trace.push('has ' + String(k)); return k in t; },\n"
                        + "  get: function (t, k) { trace.push('get ' + String(k)); return t[k]; }\n"
                        + "});\n"
                        + "JSON.stringify(p) + ' ' + trace.join(', ')");
    }

    @Test
    public void proxyToJsonFromGetTrapOnly() {
        Utils.assertWithAllModes_ES6(
                "\"result\"",
                "var p = new Proxy({}, { get: function (t, k) {\n"
                        + "  return k === 'toJSON' ? function () { return 'result'; } : undefined;\n"
                        + "} });\n"
                        + "JSON.stringify(p)");
    }

    @Test
    public void nonCallableToJsonIsIgnored() {
        Utils.assertWithAllModes_ES6("{\"toJSON\":1}", "JSON.stringify({ toJSON: 1 })");
    }

    @Test
    public void keyIsPassedToToJson() {
        Utils.assertWithAllModes_ES6(
                "{\"a\":\"a\",\"b\":[\"0\"]}",
                "var t = { toJSON: function (k) { return k; } };\n"
                        + "JSON.stringify({ a: t, b: [t] })");
    }

    @Test
    public void bigIntGetterRunsOnce() {
        Utils.assertWithAllModes_ES6(
                "\"big\" 1",
                "var calls = 0;\n"
                        + "Object.defineProperty(BigInt.prototype, 'toJSON', { configurable: true,\n"
                        + "  get: function () { calls++; return function () { return 'big'; }; } });\n"
                        + "var r = JSON.stringify(10n) + ' ' + calls;\n"
                        + "delete BigInt.prototype.toJSON;\n"
                        + "r");
    }
}
