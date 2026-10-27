/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/** */
package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.testutils.Utils;

/**
 * @author Ronald Brill
 */
public class NativeJsonTest {

    @Test
    public void stringifyResultAndResultType() {
        String jsScript =
                "function f(){ return JSON.stringify({property1:\"hello\", array1:[{subobject:1}]}); } f();";

        try (Context cx = Context.enter()) {
            TopLevel jsScope = cx.initStandardObjects();
            Object result = cx.evaluateString(jsScope, jsScript, "myscript.js", 1, null);
            assertEquals("{\"property1\":\"hello\",\"array1\":[{\"subobject\":1}]}", result);
            assertEquals("java.lang.String", result.getClass().getName());
        }
    }

    @Test
    public void reviverReceivesStringKeys() {
        Utils.assertWithAllModes_ES6(
                "string:0 string:1 string:a string:",
                "var keys = [];"
                        + "JSON.parse('{\"a\":[1,2]}', function(k, v) {"
                        + "  keys.push(typeof k + ':' + k); return v;"
                        + "});"
                        + "keys.join(' ')");
    }

    @Test
    public void replacerReceivesStringKeys() {
        Utils.assertWithAllModes_ES6(
                "string: string:a string:0",
                "var keys = [];"
                        + "JSON.stringify({a: [1]}, function(k, v) {"
                        + "  keys.push(typeof k + ':' + k); return v;"
                        + "});"
                        + "keys.join(' ')");
    }
}
