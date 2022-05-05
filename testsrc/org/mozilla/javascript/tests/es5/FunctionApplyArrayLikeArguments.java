/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 * Tests for Function.prototype.apply method
 */
package org.mozilla.javascript.tests.es5;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

public class FunctionApplyArrayLikeArguments {

    private static final int[] OPTIMIZATIONS = {-1, 0, 1};

    private Scriptable m_scope;

    @Before
    public void init() {
        Context cx = Context.enter();
        cx.setOptimizationLevel(-1);
        m_scope = cx.initStandardObjects();
    }

    @After
    public void cleanup() {
        Context.exit();
    }

    private void test(String testCode, Object expected) {
        Object result = null;
        try {
            result = eval(testCode);
        } catch (Exception e) {
            result = "EXCEPTIONCAUGHT";
        }
        assertEquals(expected, result);
    }

    private Object eval(String source) {
        Context cx = Context.getCurrentContext();
        return cx.evaluateString(m_scope, source, "source", 1, null);
    }

    @Test
    public void testArrayLikeArgumentsOfFunctionApply() {
        for (int optIdx = 0; optIdx < OPTIMIZATIONS.length; optIdx++) {
            test(
                    "function test() { return arguments[0]; }" + "test.apply(this, {});",
                    Undefined.instance);

            test(
                    "function test() { return arguments[0]; }"
                            + "test.apply(this, {'length':1, '0':'banana'});",
                    "banana");

            test(
                    "function test() { return arguments[0]; }"
                            + "test.apply(this, {'length':'1', '0':'lala'});",
                    "lala");

            test(
                    "function test() { return arguments[0]; }" + "test.apply(2,2);",
                    "EXCEPTIONCAUGHT");

            test(
                    "function test() { return arguments[0]; }"
                            + "test.apply(this,{'length':'abc', '0':'banana'});",
                    Undefined.instance);

            test(
                    "function test() { return arguments[0]; }"
                            + "test.apply(this,{'length':function(){return 1;}, '0':'banana'});",
                    Undefined.instance);
        }
    }
}
