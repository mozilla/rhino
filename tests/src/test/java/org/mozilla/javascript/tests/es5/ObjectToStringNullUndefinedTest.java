/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 * Tests for Object.prototype.toString.call(...) on null & undefined
 */
package org.mozilla.javascript.tests.es5;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

public class ObjectToStringNullUndefinedTest {
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
    public void toStringNullUndefined() {
        Object result0 =
                cx.evaluateString(
                        scope, "Object.prototype.toString.call(null)", "15.2.4.2", 1, null);
        assertEquals("[object Null]", result0);

        Object result1 =
                cx.evaluateString(
                        scope, "Object.prototype.toString.call(undefined)", "15.2.4.2", 1, null);
        assertEquals("[object Undefined]", result1);
    }
}
