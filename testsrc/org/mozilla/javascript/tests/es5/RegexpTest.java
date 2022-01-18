/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 * Tests for the Object.getOwnPropertyDescriptor(obj, prop) method
 */
package org.mozilla.javascript.tests.es5;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

public class RegexpTest {

    private Context cx;
    private ScriptableObject scope;

    @Before
    public void setUp() {
        cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_1_8);
        scope = cx.initStandardObjects();
    }

    @After
    public void tearDown() {
        Context.exit();
    }

    /** see https://github.com/mozilla/rhino/issues/684. */
    @Test
    public void testSideEffect() {
        Object result2 =
                cx.evaluateString(
                        scope,
                        "var a = 'hello world';"
                                + "a.replace(/(.)/g, '#');"
                                + "var res = '';"
                                + "a.replace([], function (b, c) { res += c; });"
                                + "res;",
                        "test",
                        1,
                        null);
        assertEquals("0", result2);
    }
}
