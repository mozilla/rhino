/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tests.Utils;

public class NumericSeparatorTest {

    /** Special Tokenizer test for numeric constant at end. */
    @Test
    public void numericAtEndOneDigit() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    Object result = cx.evaluateString(scope, "1", "test", 1, null);
                    assertEquals(1.0, result);

                    return null;
                });
    }

    /** Special Tokenizer test for numeric constant at end. */
    @Test
    public void numericAtEndManyDigits() {
        Utils.runWithOptimizationLevel(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    Object result = cx.evaluateString(scope, "1234", "test", 1, null);
                    assertEquals(1234.0, result);

                    return null;
                },
                -1);

        // the byte code generator adds a cast to integer
        Utils.runWithOptimizationLevel(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    Object result = cx.evaluateString(scope, "1234", "test", 1, null);
                    assertEquals(1234, result);

                    return null;
                },
                0);

        Utils.runWithOptimizationLevel(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    Object result = cx.evaluateString(scope, "1234", "test", 1, null);
                    assertEquals(1234, result);

                    return null;
                },
                1);
    }

    /** Special Tokenizer test for numeric separator constant at end. */
    @Test
    public void numericSeparatorAtEnd() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    try {
                        cx.evaluateString(scope, "1_", "test", 1, null);
                        Assert.fail("EvaluatorException expected");
                    } catch (EvaluatorException e) {
                        // expected
                    }

                    return null;
                });
    }
}
