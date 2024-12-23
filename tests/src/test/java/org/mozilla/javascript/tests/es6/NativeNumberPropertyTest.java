package org.mozilla.javascript.tests.es6;

import org.junit.Test;
import org.mozilla.javascript.tests.Utils;

public class NativeNumberPropertyTest {

    @Test
    public void definingAProperty() {
        final String code =
                "var func = function (number) {"
                        + "   number.snippetText = 'abc';"
                        + "   return number.snippetText;"
                        + "};"
                        + "try { "
                        + "  '' + func(-334918463);"
                        + "} catch (e) { e.message }";
        Utils.assertWithAllModes_ES6("undefined", code);
    }

    @Test
    public void definingAPropertyStrict() {
        final String code =
                "var func = function (number) {"
                        + "  'use strict';"
                        + "   number.snippetText = 'abc';"
                        + "   return number.snippetText;"
                        + "};"
                        + "try { "
                        + "  '' + func(-334918463);"
                        + "} catch (e) { e.message }";
        Utils.assertWithAllModes_ES6(
                "Cannot set property \"snippetText\" of -334918463 to \"abc\"", code);
    }

    @Test
    public void extensible() {
        final String code =
                "var func = function (number) {"
                        + "   return Object.isExtensible(number) + ' ' + Object.isExtensible(new Object(number));"
                        + "};"
                        + "try { "
                        + "  func(-334918463);"
                        + "} catch (e) { e.message }";
        Utils.assertWithAllModes_ES6("false true", code);
    }

    @Test
    public void extensibleStrict() {
        final String code =
                "var func = function (number) {"
                        + "  'use strict';"
                        + "   return Object.isExtensible(number) + ' ' + Object.isExtensible(new Object(number));"
                        + "};"
                        + "try { "
                        + "  func(-334918463);"
                        + "} catch (e) { e.message }";
        Utils.assertWithAllModes_ES6("false true", code);
    }

    @Test
    public void sealed() {
        final String code =
                "var func = function (number) {"
                        + "   return Object.isSealed(number) + ' ' + Object.isSealed(new Object(number));"
                        + "};"
                        + "try { "
                        + "  func(-334918463);"
                        + "} catch (e) { e.message }";
        Utils.assertWithAllModes_ES6("true false", code);
    }

    @Test
    public void sealedStrict() {
        final String code =
                "var func = function (number) {"
                        + "  'use strict';"
                        + "   return Object.isSealed(number) + ' ' + Object.isSealed(new Object(number));"
                        + "};"
                        + "try { "
                        + "  func(-334918463);"
                        + "} catch (e) { e.message }";
        Utils.assertWithAllModes_ES6("true false", code);
    }
}
