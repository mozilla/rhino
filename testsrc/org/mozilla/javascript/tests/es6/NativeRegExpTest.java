/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.ScriptableObject;

/**
 * @author Ronald Brill
 */
public class NativeRegExpTest {

    @Test
    public void regExIsCallableForBackwardCompatibility() {
        Context cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_1_8);
        ScriptableObject scope = cx.initStandardObjects();

        String source = "var a = new RegExp('1'); a(1).toString();";
        assertEquals("1", cx.evaluateString(scope, source, "test", 0, null));

        source = "/^\\{(.*)\\}$/('{1234}').toString();";
        assertEquals("{1234},1234", cx.evaluateString(scope, source, "test", 0, null));

        source = "RegExp('a|b','g')()";
        assertNull(cx.evaluateString(scope, source, "test", 0, null));

        source = "new /z/();";
        assertNull(cx.evaluateString(scope, source, "test", 0, null));

        source = "(new new RegExp).toString()";
        assertEquals("", cx.evaluateString(scope, source, "test", 0, null));

        Context.exit();
    }

    @Test
    public void regExIsNotCallable() {
        Context cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_ES6);
        ScriptableObject scope = cx.initStandardObjects();

        String source = "var a = new RegExp('1'); a(1);";
        try {
            cx.evaluateString(scope, source, "test", 0, null);
            fail();
        }
        catch (EcmaError e) {
            // expected
            assertTrue(e.getMessage(), e.getMessage().startsWith("TypeError: "));
        }

        source = "/^\\{(.*)\\}$/('{1234}');";
        try {
            cx.evaluateString(scope, source, "test", 0, null);
            fail();
        }
        catch (EcmaError e) {
            // expected
            assertTrue(e.getMessage(), e.getMessage().startsWith("TypeError: "));
        }

        source = "RegExp('a|b','g')();";
        try {
            cx.evaluateString(scope, source, "test", 0, null);
            fail();
        }
        catch (EcmaError e) {
            // expected
            assertTrue(e.getMessage(), e.getMessage().startsWith("TypeError: "));
        }

        source = "new /z/();";
        try {
            cx.evaluateString(scope, source, "test", 0, null);
            fail();
        }
        catch (EcmaError e) {
            // expected
            assertTrue(e.getMessage(), e.getMessage().startsWith("TypeError: "));
        }


        source = "new new RegExp";
        try {
            cx.evaluateString(scope, source, "test", 0, null);
            fail();
        }
        catch (EcmaError e) {
            // expected
            assertTrue(e.getMessage(), e.getMessage().startsWith("TypeError: "));
        }

        Context.exit();
    }
}
