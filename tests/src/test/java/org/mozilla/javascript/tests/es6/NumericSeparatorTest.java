/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tests.Utils;

public class NumericSeparatorTest {

    /** Special Tokenizer test for numeric constant at end. */
    @Test
    public void numericAtEndOneDigit() {
        Utils.assertWithAllModes_ES6(1, "1");
    }

    /** Special Tokenizer test for numeric constant at end. */
    @Test
    public void numericAtEndManyDigits() {
        Utils.runWithMode(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    Object result = cx.evaluateString(scope, "1234", "test", 1, null);
                    assertEquals(1234.0, result);

                    return null;
                },
                true);

        // the byte code generator adds a cast to integer
        Utils.runWithMode(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    Object result = cx.evaluateString(scope, "1234", "test", 1, null);
                    assertEquals(1234, result);

                    return null;
                },
                false);
    }

    /** Special Tokenizer test for numeric separator constant at end. */
    @Test
    public void numericSeparatorAtEnd() {
        Utils.assertEvaluatorExceptionES6("number format error (test#1)", "1_");
    }
}
