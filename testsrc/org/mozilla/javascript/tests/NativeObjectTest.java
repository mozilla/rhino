/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class NativeObjectTest {

    /**
     * Freeze has to take care of MemberBox based properties
     * with a delegateTo defined.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void freeze_captureStackTrace() throws Exception {
        Context cx = Context.enter();
        Scriptable global = cx.initStandardObjects();
        Object result = cx.evaluateString(global,
                "var myError = {};\n"
                + "Error.captureStackTrace(myError);\n"
                + "Object.freeze(myError);"
                + "myError.stack;",
                "", 1, null);
        Assert.assertTrue(result instanceof String);
        Context.exit();
    }

    /**
     * getOwnPropertyDescriptor has to take care of MemberBox based properties
     * with a delegateTo defined.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void getOwnPropertyDescriptor_captureStackTrace() throws Exception {
        Context cx = Context.enter();
        Scriptable global = cx.initStandardObjects();
        Object result = cx.evaluateString(global,
                "var myError = {};\n"
                + "Error.captureStackTrace(myError);\n"
                + "var desc = Object.getOwnPropertyDescriptor(myError, 'stack');"
                + "'' + desc.get + '-' + desc.set + '-' + desc.value;",
                "", 1, null);
        Assert.assertEquals("undefined-undefined-\tat :2\r\n", result);
        Context.exit();
    }

    /**
     * getOwnPropertyDescriptor has to take care of MemberBox based properties
     * with a delegateTo defined.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void getOwnPropertyDescriptorAttributes_captureStackTrace() throws Exception {
        Context cx = Context.enter();
        Scriptable global = cx.initStandardObjects();
        Object result = cx.evaluateString(global,
                "var myError = {};\n"
                + "Error.captureStackTrace(myError);\n"
                + "var desc = Object.getOwnPropertyDescriptor(myError, 'stack');"
                + "desc.writable + ' ' + desc.configurable + ' ' + desc.enumerable",
                "", 1, null);
        Assert.assertEquals("true true false", result);
        Context.exit();
    }
}
