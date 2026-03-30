/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.*;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.testutils.Utils;

public class AsyncFunctionPrototypeTest {

    private void assertScript(String expected, String script) {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    TopLevel scope = cx.initStandardObjects();
                    Object result = cx.evaluateString(scope, script, "test", 1, null);
                    assertEquals(expected, Context.toString(result));
                    return null;
                });
    }

    private void assertScriptThrowsTypeError(String script) {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    TopLevel scope = cx.initStandardObjects();
                    try {
                        cx.evaluateString(scope, script, "test", 1, null);
                        fail("Expected TypeError");
                    } catch (EcmaError e) {
                        assertTrue(
                                "Expected TypeError, got: " + e.getMessage(),
                                e.getMessage().contains("TypeError"));
                    }
                    return null;
                });
    }

    @Test
    public void asyncFunctionConstructorNameIsAsyncFunction() {
        assertScript("AsyncFunction", "(async function(){}).constructor.name");
    }

    @Test
    public void asyncFunctionIsInstanceOfFunction() {
        assertScript("true", "(async function(){}) instanceof Function");
    }

    @Test
    public void asyncFunctionTypeofIsFunction() {
        assertScript("function", "typeof (async function(){})");
    }

    @Test
    public void asyncArrowFunctionConstructorNameIsAsyncFunction() {
        assertScript("AsyncFunction", "(async () => {}).constructor.name");
    }

    @Test
    public void asyncFunctionPrototypeIsNotFunctionPrototype() {
        assertScript(
                "true",
                "var asyncProto = (async function(){}).constructor.prototype;"
                        + "asyncProto !== Function.prototype");
    }

    @Test
    public void asyncFunctionPrototypeChainIncludesFunctionPrototype() {
        assertScript(
                "true",
                "var AsyncFunction = (async function(){}).constructor;"
                        + "Object.getPrototypeOf(AsyncFunction.prototype) === Function.prototype");
    }

    @Test
    public void asyncFunctionIsNotConstructor() {
        assertScriptThrowsTypeError("new (async function(){})()");
    }

    @Test
    public void asyncGeneratorFunctionConstructorNameIsAsyncGeneratorFunction() {
        assertScript("AsyncGeneratorFunction", "(async function*(){}).constructor.name");
    }

    @Test
    public void asyncFunctionDeclarationIsAccessible() {
        assertScript("function", "async function myAsyncFn() {} typeof myAsyncFn");
    }

    @Test
    public void asyncMethodConstructorIsAsyncFunction() {
        assertScript(
                "AsyncFunction", "var obj = { async method() {} }; obj.method.constructor.name");
    }
}
