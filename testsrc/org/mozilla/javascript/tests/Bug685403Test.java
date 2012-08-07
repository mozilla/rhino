/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContinuationPending;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * @author André Bargull
 *
 */
public class Bug685403Test {
    private Context cx;
    private ScriptableObject scope;

    @Before
    public void setUp() {
        cx = Context.enter();
        cx.setOptimizationLevel(-1);
        scope = cx.initStandardObjects();
    }

    @After
    public void tearDown() {
        Context.exit();
    }

    public static Object continuation(Context cx, Scriptable thisObj,
            Object[] args, Function funObj) {
        ContinuationPending pending = cx.captureContinuation();
        throw pending;
    }

    @Test
    public void test() {
        String source = "var state = '';";
        source += "function A(){state += 'A'}";
        source += "function B(){state += 'B'}";
        source += "function C(){state += 'C'}";
        source += "try { A(); continuation(); B() } finally { C() }";
        source += "state";

        String[] functions = new String[] { "continuation" };
        scope.defineFunctionProperties(functions, Bug685403Test.class,
                ScriptableObject.DONTENUM);

        Object state = null;
        Script script = cx.compileString(source, "", 1, null);
        try {
            cx.executeScriptWithContinuations(script, scope);
            fail("expected ContinuationPending exception");
        } catch (ContinuationPending pending) {
            state = cx.resumeContinuation(pending.getContinuation(), scope, "");
        }
        assertEquals("ABC", state);
    }

}
