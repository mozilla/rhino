/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/** Test for the Object.hasOwn */
package org.mozilla.javascript.tests.es2022;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

public class NativeObjectTest {

    @Test
    public void hasStringOwn() {
        final String code =
                "let result = Object.hasOwn({ test: '123' }, 'test');\n" + "'result = ' + result";
        Utils.assertWithAllModes_ES6("result = true", code);
    }

    @Test
    public void hasUndefinedOwn() {
        final String code =
                "let result = Object.hasOwn({ test: undefined }, 'test');\n"
                        + "'result = ' + result;";
        Utils.assertWithAllModes_ES6("result = true", code);
    }

    @Test
    public void hasNullOwn() {
        final String code =
                "let result = Object.hasOwn({ test: null }, 'test');\n" + "'result = ' + result;";
        Utils.assertWithAllModes_ES6("result = true", code);
    }

    @Test
    public void hasArrayPropertyOwn() {
        final String code =
                "let dessert = [\"cake\", \"coffee\", \"chocolate\"];\n"
                        + "let result = Object.hasOwn(dessert, 2);\n"
                        + "'result = ' + result;";
        Utils.assertWithAllModes_ES6("result = true", code);
    }

    @Test
    public void hasNoOwn() {
        final String code =
                "let result = Object.hasOwn({ cake: 123 }, 'test');\n" + "'result = ' + result";
        Utils.assertWithAllModes_ES6("result = false", code);
    }

    @Test
    public void createHasOwn() {
        final String code =
                "var foo = Object.create(null);\n"
                        + "foo.prop = 'test';\n"
                        + "var result = Object.hasOwn(foo, 'prop');\n"
                        + "'result = ' + result;";
        Utils.assertWithAllModes_ES6("result = true", code);
    }

    @Test
    public void createNoHasOwn() {
        final String code =
                "var result = Object.hasOwn(Object.create({ q: 321 }), 'q');\n"
                        + "'result = ' + result; ";
        Utils.assertWithAllModes_ES6("result = false", code);
    }

    @Test
    public void calledTest() {
        final String code =
                "var called = false;\n"
                        + "try {\n"
                        + "          Object.hasOwn(null, { toString() { called = true } });\n"
                        + "} catch (e) {}\n"
                        + "'called = ' + called;";
        Utils.assertWithAllModes_ES6("called = false", code);
    }
}
