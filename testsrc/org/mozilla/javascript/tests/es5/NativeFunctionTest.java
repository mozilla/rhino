/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es5;
import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

public class NativeFunctionTest {

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
    public void testFunctionPrototypeNotWritable() {
        Object result = cx.evaluateString(
                scope, "function e() {}"
                        + " e.__proto__ = true;"
                        + " e.__proto__ != true;",
                "test", 1, null
        );
        assertEquals(true, result);
    }

    @Test
    public void testFunctionPrototypeNotWritableStrict() {
        Object result = cx.evaluateString(
                scope, " 'use strict';"
                        + " function e() {}"
                        + " e.__proto__ = true;"
                        + " e.__proto__ != true;",
                "test", 1, null
        );
        assertEquals(true, result);
    }
}
