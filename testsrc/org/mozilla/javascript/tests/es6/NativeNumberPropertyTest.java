package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

public class NativeNumberPropertyTest {
    private Context cx;
    private ScriptableObject scope;

    @Before
    public void setUp() {
        cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_ES6);
        scope = cx.initStandardObjects();
    }

    @After
    public void tearDown() {
        Context.exit();
    }

    @Test
    public void testDefiningAProperty() {
        Object result =
                cx.evaluateString(
                        scope,
                        "var func = function (number) {"
                                + "   number.snippetText = 'abc';"
                                + "   return number.snippetText;"
                                + "};"
                                + "try { "
                                + "  '' + func(-334918463);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("undefined", result);
    }

    @Test
    public void testDefiningAPropertyStrict() {
        Object result =
                cx.evaluateString(
                        scope,
                        "var func = function (number) {"
                                + "  'use strict';"
                                + "   number.snippetText = 'abc';"
                                + "   return number.snippetText;"
                                + "};"
                                + "try { "
                                + "  '' + func(-334918463);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("Cannot set property \"snippetText\" of -334918463 to \"abc\"", result);
    }

    @Test
    public void testExtensible() {
        Object result =
                cx.evaluateString(
                        scope,
                        "var func = function (number) {"
                                + "   return Object.isExtensible(number) + ' ' + Object.isExtensible(new Object(number));"
                                + "};"
                                + "try { "
                                + "  func(-334918463);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("false true", result);
    }

    @Test
    public void testExtensibleStrict() {
        Object result =
                cx.evaluateString(
                        scope,
                        "var func = function (number) {"
                                + "  'use strict';"
                                + "   return Object.isExtensible(number) + ' ' + Object.isExtensible(new Object(number));"
                                + "};"
                                + "try { "
                                + "  func(-334918463);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("false true", result);
    }

    @Test
    public void testSealed() {
        Object result =
                cx.evaluateString(
                        scope,
                        "var func = function (number) {"
                                + "   return Object.isSealed(number) + ' ' + Object.isSealed(new Object(number));"
                                + "};"
                                + "try { "
                                + "  func(-334918463);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("true false", result);
    }

    @Test
    public void testSealedStrict() {
        Object result =
                cx.evaluateString(
                        scope,
                        "var func = function (number) {"
                                + "  'use strict';"
                                + "   return Object.isSealed(number) + ' ' + Object.isSealed(new Object(number));"
                                + "};"
                                + "try { "
                                + "  func(-334918463);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("true false", result);
    }
}
