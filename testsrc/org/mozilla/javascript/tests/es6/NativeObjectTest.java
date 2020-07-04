/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 * Tests for the Object.getOwnPropertyDescriptor(obj, prop) method
 */
package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

/**
 * Test for NativeObject.
 */
public class NativeObjectTest {

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
    public void testAssignPropertyGetter() {
        Object result = cx.evaluateString(
                scope, "var obj = Object.defineProperty({}, 'propA', {\n"
                        + "                 enumerable: true,\n"
                        + "                 get: function () {\n"
                        + "                        Object.defineProperty(this, 'propB', {\n"
                        + "                                 value: 3,\n"
                        + "                                 enumerable: false\n"
                        + "                        });\n"
                        + "                      }\n"
                        + "          });\n"
                        + "Object.assign(obj, {propB: 2});\n"
                        + "Object.assign({propB: 1}, obj);\n"
                        + "'obj.propB = ' + obj.propB + ', enumerable = ' + Object.getOwnPropertyDescriptor(obj, 'propB').enumerable",
                "test", 1, null
        );

        assertEquals("obj.propB = 3, enumerable = false", result);
    }

    @Test
    public void testAssignNumericPropertyGetter() {
        Object result = cx.evaluateString(
                scope, "var obj = Object.defineProperty({}, 1, {\n"
                        + "                 enumerable: true,\n"
                        + "                 get: function () {\n"
                        + "                        Object.defineProperty(this, 2, {\n"
                        + "                                 value: 3,\n"
                        + "                                 enumerable: false\n"
                        + "                        });\n"
                        + "                      }\n"
                        + "          });\n"
                        + "Object.assign(obj, {2: 2});\n"
                        + "Object.assign({2: 1}, obj);\n"
                        + "'obj[2] = ' + obj[2] + ', enumerable = ' + Object.getOwnPropertyDescriptor(obj, 2).enumerable",
                "test", 1, null
        );

        assertEquals("obj[2] = 3, enumerable = false", result);
    }
}
