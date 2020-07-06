/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.javascript.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.Scriptable;

import static org.junit.Assert.assertEquals;

public class Issue663Test {
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
        try{
            result = eval(testCode);
        } catch(EcmaError e) {
            // Expected
        } catch(Exception e) {
            result = "EXCEPTIONCAUGHT";
        }

        assertEquals(expected, result);
    }

    private Object eval(String source) {
        Context cx = Context.getCurrentContext();
        return cx.evaluateString(m_scope, source, "source", 1, null);

    }

    @Test
    public void testIssue663() {
        test("\\u000a:S<6", null);
    }
}
