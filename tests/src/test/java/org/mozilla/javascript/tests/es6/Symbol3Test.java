/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.SymbolKey;
import org.mozilla.javascript.tests.Utils;

/** Tests for Symbol support. */
public class Symbol3Test {

    @Test
    public void scriptRuntimeTypeofSymbolKey() {
        final String code =
                "function foo() {"
                        + "  var sym = Object.getOwnPropertySymbols(arguments);"
                        + "  return '' + sym.length + ' ' + typeof sym[0];"
                        + "}"
                        + "foo()";
        Utils.assertWithAllModes_ES6("1 symbol", code);
    }

    @Test
    public void scriptRuntimeTypeofSymbol() {
        final String code = "typeof Symbol.toStringTag";
        Utils.assertWithAllModes_ES6("symbol", code);
    }

    @Test
    public void symbolProperty() throws Exception {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    final String script =
                            "var sym = Object.getOwnPropertySymbols(MyHostObject);"
                                    + "var str = sym[0].toString();"
                                    + "var result = sym[0].toString();"
                                    + "result += ' ' + sym.length + ' ' + typeof sym[0];"
                                    + "result += ' ' + MyHostObject[sym[0]]";

                    try {
                        final ScriptableObject jsObj = new MyHostObject();
                        jsObj.setParentScope(scope);

                        jsObj.defineProperty(
                                SymbolKey.TO_STRING_TAG, "foo", ScriptableObject.DONTENUM);

                        scope.put("MyHostObject", scope, jsObj);
                    } catch (Exception e) {
                    }

                    final String result =
                            (String) cx.evaluateString(scope, script, "myScript", 1, null);

                    assertEquals("Symbol(Symbol.toStringTag) 1 symbol foo", result);

                    return null;
                });
    }

    public static class MyHostObject extends ScriptableObject {

        @Override
        public String getClassName() {
            return "MyHostObject";
        }
    }
}
