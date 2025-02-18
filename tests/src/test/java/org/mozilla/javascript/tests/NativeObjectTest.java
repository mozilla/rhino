/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.testutils.Utils;

public class NativeObjectTest {

    /**
     * Freeze has to take care of MemberBox based properties with a delegateTo defined.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void freeze_captureStackTrace() throws Exception {
        final String script =
                "var myError = {};\n"
                        + "Error.captureStackTrace(myError);\n"
                        + "Object.freeze(myError);\n"
                        + "myError.stack.trim();";
        Utils.assertWithAllModesTopLevelScope_ES6("at test.js:1", script);
    }

    /**
     * getOwnPropertyDescriptor has to take care of MemberBox based properties with a delegateTo
     * defined.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void getOwnPropertyDescriptor_captureStackTrace() throws Exception {
        final String script =
                "var myError = {};\n"
                        + "Error.captureStackTrace(myError);\n"
                        + "var desc = Object.getOwnPropertyDescriptor(myError, 'stack');\n"
                        + "var res = '' + desc.get + '-' + desc.set + '-' + desc.value;\n"
                        + "res = res.replace(/(\\n|\\r)/gm, '');";
        Utils.assertWithAllModesTopLevelScope_ES6("undefined-undefined-\tat test.js:1", script);
    }

    /**
     * getOwnPropertyDescriptor has to take care of MemberBox based properties with a delegateTo
     * defined.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void getOwnPropertyDescriptorAttributes_captureStackTrace() throws Exception {
        final String script =
                "var myError = {};\n"
                        + "Error.captureStackTrace(myError);\n"
                        + "var desc = Object.getOwnPropertyDescriptor(myError, 'stack');\n"
                        + "desc.writable + ' ' + desc.configurable + ' ' + desc.enumerable";
        Utils.assertWithAllModesTopLevelScope_ES6("true true false", script);
    }

    public static class JavaObj {
        public String name = "test";
    }

    @Test
    public void nativeJavaObject_hasOwnProperty() {
        try (Context cx = Context.enter()) {
            Scriptable scope = cx.initStandardObjects();
            ScriptableObject.putProperty(scope, "javaObj", Context.javaToJS(new JavaObj(), scope));
            Object result =
                    cx.evaluateString(
                            scope,
                            "Object.prototype.hasOwnProperty.call(javaObj, \"name\");",
                            "",
                            1,
                            null);
            assertTrue((Boolean) result);
        }
    }
}
