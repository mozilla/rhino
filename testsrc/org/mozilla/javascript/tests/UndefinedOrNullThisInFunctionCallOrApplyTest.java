/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mozilla.javascript.*;
import org.mozilla.javascript.drivers.LanguageVersion;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

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
        cx.setLanguageVersion(Context.VERSION_DEFAULT);
        Context.exit();
    }

    @Test
    public void whenVersionGt17ThenPassNullAsThisObjJsFunc() {
        cx.setLanguageVersion(Context.VERSION_1_8);
        NativeArray arr = (NativeArray) Evaluator.eval("function F2() {return this;};[this, F2.apply(), F2.apply(undefined)];");

        assertNotEquals(arr.get(0), arr.get(1));
        assertNotEquals(arr.get(0), arr.get(2));
        assertEquals(arr.get(1), arr.get(2));

        arr = (NativeArray) Evaluator.eval("function F2() {return this;};[this, F2.apply(), F2.apply(null)];");
        assertNotEquals(arr.get(0), arr.get(1));
        assertNotEquals(arr.get(0), arr.get(2));
        assertEquals(arr.get(1), arr.get(2));

        cx.setLanguageVersion(Context.VERSION_ES6);
        arr = (NativeArray) Evaluator.eval("function F2() {return this;};[this, F2.apply(), F2.apply(undefined)];");

        assertNotEquals(arr.get(0), arr.get(1));
        assertNotEquals(arr.get(0), arr.get(2));
        assertEquals(arr.get(1), arr.get(2));

        arr = (NativeArray) Evaluator.eval("function F2() {return this;};[this, F2.apply(), F2.apply(null)];");
        assertNotEquals(arr.get(0), arr.get(1));
        assertNotEquals(arr.get(0), arr.get(2));
        assertEquals(arr.get(1), arr.get(2));

    }

    @Test
    public void whenVersionLtEq17ThenPassGlobalThisObjJsFunc() {
        cx.setLanguageVersion(Context.VERSION_1_7);
        NativeArray arr = (NativeArray) Evaluator.eval("function F2() {return this;};[this, F2.apply(), F2.apply(undefined)];");

        assertEquals(arr.get(0), arr.get(1));
        assertEquals(arr.get(0), arr.get(2));
        assertEquals(arr.get(1), arr.get(2));

        arr = (NativeArray) Evaluator.eval("function F2() {return this;};[this, F2.apply(), F2.apply(null)];");

        assertEquals(arr.get(0), arr.get(1));
        assertEquals(arr.get(0), arr.get(2));
        assertEquals(arr.get(1), arr.get(2));
    }
}
