/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

public class ES6IteratorTest {
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
    public void valueDone() {
        Object result =
                cx.evaluateString(
                        scope,
                        "  var res = '';\n"
                                + "  var arr = ['x'];\n"
                                + "  var arrIter = arr[Symbol.iterator]();\n"
                                + "  for (var p in arrIter.next()) {\n"
                                + "    res = res + p + ' ';\n"
                                + "  }\n",
                        "test",
                        1,
                        null);
        // this is the order used by all current browsers
        assertEquals("value done ", result);
    }
}
