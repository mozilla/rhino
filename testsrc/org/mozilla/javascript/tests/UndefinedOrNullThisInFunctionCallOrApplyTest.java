/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;

/**
 * 15.3.4.3-1.js
 */
public class UndefinedOrNullThisInFunctionCallOrApplyTest {
    private Context cx;

    private BaseFunction function;
    private HashMap<String, Scriptable> bindings;

    @Before
    public void setUp() throws Exception {
        cx = Context.enter();
        function = new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                return thisObj;
            }
        };
        bindings = new HashMap<String, Scriptable>();
        bindings.put("myFunc", function);
    }

    @After
    public void tearDown() throws Exception {
        Context.exit();
    }

    @Test
    public void test1() {
        cx.setLanguageVersion(Context.VERSION_ES6);
        Object o = Evaluator.eval("myFunc.apply(undefined);", bindings);
        assertEquals(Undefined.instance, o);
    }
}
