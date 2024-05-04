/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

/**
 * Example of defining global functions.
 *
 * @author Norris Boyd
 */
public class DefineFunctionPropertiesTest {

    ScriptableObject global;
    static Object key = "DefineFunctionPropertiesTest";

    /**
     * Demonstrates how to create global functions in JavaScript from static methods defined in
     * Java.
     */
    @Before
    public void setUp() {
        try (Context cx = Context.enter()) {
            global = cx.initStandardObjects();
            String[] names = {"f", "g"};
            global.defineFunctionProperties(
                    names, DefineFunctionPropertiesTest.class, ScriptableObject.DONTENUM);
        }
    }

    /** Simple global function that doubles its input. */
    public static int f(int a) {
        return a * 2;
    }

    /** Simple test: call 'f' defined above */
    @Test
    public void simpleFunction() {
        try (Context cx = Context.enter()) {
            Object result = cx.evaluateString(global, "f(7) + 1", "test source", 1, null);
            assertEquals(15.0, result);
        }
    }

    /**
     * More complicated example: this form of call allows variable argument lists, and allows access
     * to the 'this' object. For a global function, the 'this' object is the global object. In this
     * case we look up a value that we associated with the global object using {@link
     * ScriptableObject#getAssociatedValue(Object)}.
     */
    public static Object g(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        Object arg = args.length > 0 ? args[0] : Undefined.instance;
        Object privateValue = Undefined.instance;
        if (thisObj instanceof ScriptableObject) {
            privateValue = ((ScriptableObject) thisObj).getAssociatedValue(key);
        }
        return arg.toString() + privateValue;
    }

    /** Associate a value with the global scope and call function 'g' defined above. */
    @Test
    public void privateData() {
        try (Context cx = Context.enter()) {
            global.associateValue(key, "bar");
            Object result = cx.evaluateString(global, "g('foo');", "test source", 1, null);
            assertEquals("foobar", result);
        }
    }
}
