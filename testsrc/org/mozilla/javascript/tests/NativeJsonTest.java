/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/** */
package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/** @author Ronald Brill */
public class NativeJsonTest {

    @Test
    public void stringifyResultAndResultType() {
        String jsScript =
                "function f(){ return JSON.stringify({property1:\"hello\", array1:[{subobject:1}]}); } f();";

        try (Context cx = Context.enter()) {
            Scriptable jsScope = cx.initStandardObjects();
            Object result = cx.evaluateString(jsScope, jsScript, "myscript.js", 1, null);
            assertEquals("{\"property1\":\"hello\",\"array1\":[{\"subobject\":1}]}", result);
            assertEquals("java.lang.String", result.getClass().getName());
        }
    }
}
