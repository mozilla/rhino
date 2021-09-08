/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.ScriptableObject;

public class NumericSeparatorTest {
    private Context cx;
    private ScriptableObject scope;

    @Before
    public void setUp() {
        cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_ES6);
        scope = cx.initStandardObjects();
    }

    @After
    public void tearDown() {
        Context.exit();
    }

    /** Special Tokenizer test for numeric constant at end. */
    @Test
    public void numericAtEndOneDigit() {
        Object result = cx.evaluateString(scope, "1", "test", 1, null);
        assertEquals(1.0, result);
    }

    /** Special Tokenizer test for numeric constant at end. */
    @Test
    public void numericAtEndManyDigits() {
        Object result = cx.evaluateString(scope, "1234", "test", 1, null);
        assertEquals(1234, result);
    }

    /** Special Tokenizer test for numeric separator constant at end. */
    @Test(expected = EvaluatorException.class)
    public void numericSeparatorAtEnd() {
        cx.evaluateString(scope, "1_", "test", 1, null);
    }
}
