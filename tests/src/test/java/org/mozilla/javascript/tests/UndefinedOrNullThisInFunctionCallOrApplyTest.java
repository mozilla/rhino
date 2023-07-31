/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.HashMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

public class UndefinedOrNullThisInFunctionCallOrApplyTest {

    private Context cx;

    private BaseFunction function;

    @Before
    public void setUp() throws Exception {
        cx = Context.enter();
        function =
                new BaseFunction() {
                    @Override
                    public Object call(
                            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                        System.out.println("QWE");
                        return thisObj;
                    }
                };
    }

    @After
    public void tearDown() throws Exception {
        cx.setLanguageVersion(Context.VERSION_DEFAULT);
        Context.exit();
    }

    @Test
    @Ignore
    public void whenVersionGt17ThenPassNullAsThisObjJavaFunc() {
        HashMap<String, Scriptable> bindings = new HashMap<String, Scriptable>();
        bindings.put("F2", function);

        cx.setLanguageVersion(Context.VERSION_1_8);
        NativeArray arr =
                (NativeArray) Evaluator.eval("[this, F2.apply(), F2.apply(undefined)];", bindings);

        assertNotEquals(arr.get(0), arr.get(1));
        assertNotEquals(arr.get(0), arr.get(2));
        assertEquals(arr.get(1), arr.get(2));

        arr = (NativeArray) Evaluator.eval("[this, F2.apply(), F2.apply(null)];", bindings);
        assertNotEquals(arr.get(0), arr.get(1));
        assertNotEquals(arr.get(0), arr.get(2));
        assertEquals(arr.get(1), arr.get(2));

        cx.setLanguageVersion(Context.VERSION_ES6);
        arr = (NativeArray) Evaluator.eval("[this, F2.apply(), F2.apply(undefined)];", bindings);

        assertNotEquals(arr.get(0), arr.get(1));
        assertNotEquals(arr.get(0), arr.get(2));
        assertEquals(arr.get(1), arr.get(2));

        arr = (NativeArray) Evaluator.eval("[this, F2.apply(), F2.apply(null)];", bindings);
        assertNotEquals(arr.get(0), arr.get(1));
        assertNotEquals(arr.get(0), arr.get(2));
        assertEquals(arr.get(1), arr.get(2));
    }

    @Test
    @Ignore
    public void whenVersionLtEq17ThenPassGlobalThisObjJavaFunc() {
        HashMap<String, Scriptable> bindings = new HashMap<String, Scriptable>();
        bindings.put("F2", function);

        cx.setLanguageVersion(Context.VERSION_1_7);
        NativeArray arr =
                (NativeArray)
                        Evaluator.eval(
                                "{return this;};[this, F2.apply(), F2.apply(undefined)];",
                                bindings);

        assertEquals(arr.get(0), arr.get(1));
        assertEquals(arr.get(0), arr.get(2));
        assertEquals(arr.get(1), arr.get(2));

        arr = (NativeArray) Evaluator.eval("[this, F2.apply(), F2.apply(null)];", bindings);

        assertEquals(arr.get(0), arr.get(1));
        assertEquals(arr.get(0), arr.get(2));
        assertEquals(arr.get(1), arr.get(2));
    }

    @Test
    public void whenVersionGt17ThenPassNullAsThisObjForApplyJS() {
        cx.setLanguageVersion(Context.VERSION_1_8);
        NativeArray arr =
                (NativeArray)
                        Evaluator.eval(
                                "function F2() {return this;};[this, F2.apply(), F2.apply(undefined)];");

        assertNotEquals(arr.get(0), arr.get(1));
        assertNotEquals(arr.get(0), arr.get(2));
        assertNotEquals(arr.get(1), arr.get(2));
        assertEquals(Undefined.instance, arr.get(2));
        assertEquals(Undefined.SCRIPTABLE_UNDEFINED, arr.get(2));

        arr =
                (NativeArray)
                        Evaluator.eval(
                                "function F2() {return this;};[this, F2.apply(), F2.apply(null)];");
        assertNotEquals(arr.get(0), arr.get(1));
        assertNotEquals(arr.get(0), arr.get(2));
        assertEquals(arr.get(1), arr.get(2));

        cx.setLanguageVersion(Context.VERSION_ES6);
        arr =
                (NativeArray)
                        Evaluator.eval(
                                "function F2() {return this;};[this, F2.apply(), F2.apply(undefined)];");

        assertNotEquals(arr.get(0), arr.get(1));
        assertNotEquals(arr.get(0), arr.get(2));
        assertNotEquals(arr.get(1), arr.get(2));
        assertEquals(Undefined.instance, arr.get(2));
        assertEquals(Undefined.SCRIPTABLE_UNDEFINED, arr.get(2));

        arr =
                (NativeArray)
                        Evaluator.eval(
                                "function F2() {return this;};[this, F2.apply(), F2.apply(null)];");
        assertNotEquals(arr.get(0), arr.get(1));
        assertNotEquals(arr.get(0), arr.get(2));
        assertEquals(arr.get(1), arr.get(2));
    }

    @Test
    public void whenVersionLtEq17ThenPassGlobalThisObjForApplyJS() {
        cx.setLanguageVersion(Context.VERSION_1_7);
        NativeArray arr =
                (NativeArray)
                        Evaluator.eval(
                                "function F2() {return this;};[this, F2.apply(), F2.apply(undefined)];");

        assertEquals(arr.get(0), arr.get(1));
        assertEquals(arr.get(0), arr.get(2));
        assertEquals(arr.get(1), arr.get(2));

        arr =
                (NativeArray)
                        Evaluator.eval(
                                "function F2() {return this;};[this, F2.apply(), F2.apply(null)];");

        assertEquals(arr.get(0), arr.get(1));
        assertEquals(arr.get(0), arr.get(2));
        assertEquals(arr.get(1), arr.get(2));
    }

    @Test
    public void whenVersionGt17ThenPassNullAsThisObjForCallJS() {
        cx.setLanguageVersion(Context.VERSION_1_8);
        NativeArray arr =
                (NativeArray)
                        Evaluator.eval(
                                "function F2() {return this;};[this, F2.call(), F2.call(undefined)];");

        assertNotEquals(arr.get(0), arr.get(1));
        assertNotEquals(arr.get(0), arr.get(2));
        assertNotEquals(arr.get(1), arr.get(2));
        assertEquals(Undefined.instance, arr.get(2));
        assertEquals(Undefined.SCRIPTABLE_UNDEFINED, arr.get(2));

        arr =
                (NativeArray)
                        Evaluator.eval(
                                "function F2() {return this;};[this, F2.call(), F2.call(null)];");
        assertNotEquals(arr.get(0), arr.get(1));
        assertNotEquals(arr.get(0), arr.get(2));
        assertEquals(arr.get(1), arr.get(2));

        cx.setLanguageVersion(Context.VERSION_ES6);
        arr =
                (NativeArray)
                        Evaluator.eval(
                                "function F2() {return this;};[this, F2.call(), F2.call(undefined)];");

        assertNotEquals(arr.get(0), arr.get(1));
        assertNotEquals(arr.get(0), arr.get(2));
        assertNotEquals(arr.get(1), arr.get(2));
        assertEquals(Undefined.instance, arr.get(2));
        assertEquals(Undefined.SCRIPTABLE_UNDEFINED, arr.get(2));

        arr =
                (NativeArray)
                        Evaluator.eval(
                                "function F2() {return this;};[this, F2.call(), F2.call(null)];");
        assertNotEquals(arr.get(0), arr.get(1));
        assertNotEquals(arr.get(0), arr.get(2));
        assertEquals(arr.get(1), arr.get(2));
    }

    @Test
    public void whenVersionLtEq17ThenPassGlobalThisObjForCallJS() {
        cx.setLanguageVersion(Context.VERSION_1_7);
        NativeArray arr =
                (NativeArray)
                        Evaluator.eval(
                                "function F2() {return this;};[this, F2.call(), F2.call(undefined)];");

        assertEquals(arr.get(0), arr.get(1));
        assertEquals(arr.get(0), arr.get(2));
        assertEquals(arr.get(1), arr.get(2));

        arr =
                (NativeArray)
                        Evaluator.eval(
                                "function F2() {return this;};[this, F2.call(), F2.call(null)];");

        assertEquals(arr.get(0), arr.get(1));
        assertEquals(arr.get(0), arr.get(2));
        assertEquals(arr.get(1), arr.get(2));
    }
}
