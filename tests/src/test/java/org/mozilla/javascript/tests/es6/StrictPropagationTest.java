package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaFunction;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.SerializableCallable;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.testutils.Utils;

public class StrictPropagationTest {

    @Test
    public void strictIsNotPropagatedWhenLoadingDynamically() {
        final String outer = "function outer() { 'use strict'; includeDynamic() } outer(); x";
        // This works in non-strict mode, and is a ReferenceError in strict mode
        final String nested = "(function() { x = 'x'; })()";

        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    Scriptable scope = cx.initStandardObjects(new TopLevel());

                    SerializableCallable includeDynamic =
                            (cx2, scope2, thisObj, args) ->
                                    cx2.evaluateString(scope2, nested, "nested", 1, null);
                    scope.put(
                            "includeDynamic",
                            scope,
                            new LambdaFunction(scope, "includeDynamic", 0, includeDynamic));

                    Object res = cx.evaluateString(scope, outer, "outer", 1, null);
                    assertEquals("x", res);

                    return null;
                });
    }
}
