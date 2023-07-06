/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 * Tests for Function.prototype.apply method
 */
package org.mozilla.javascript.tests.es5;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.tests.Utils;

public class FunctionApplyArrayLikeArguments {

    private static void test(String testCode, Object expected) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();

                    Object result = null;
                    try {
                        result = cx.evaluateString(scope, testCode, "test", 1, null);
                    } catch (Exception e) {
                        result = "EXCEPTIONCAUGHT";
                    }
                    assertEquals(expected, result);

                    return null;
                });
    }

    @Test
    public void arrayLikeArgumentsOfFunctionApply() {
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

        test("function test() { return arguments[0]; }" + "test.apply(2,2);", "EXCEPTIONCAUGHT");

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
