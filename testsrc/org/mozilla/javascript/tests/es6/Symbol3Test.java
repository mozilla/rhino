/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tests.Utils;

/** Tests for Symbol support. */
public class Symbol3Test {

    @Test
    public void scriptRuntimeTypeofSymbolKey() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    String code =
                            "function foo() {"
                                    + "  var sym = Object.getOwnPropertySymbols(arguments);"
                                    + "  return '' + sym.length + ' ' + typeof sym[0];"
                                    + "}"
                                    + "foo()";
                    String result = (String) cx.evaluateString(scope, code, "test", 1, null);
                    assertEquals("1 symbol", result);

                    return null;
                });
    }

    @Test
    public void scriptRuntimeTypeofSymbol() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    String result =
                            (String)
                                    cx.evaluateString(
                                            scope, "typeof Symbol.toStringTag", "test", 1, null);
                    assertEquals("symbol", result);

                    return null;
                });
    }
}
