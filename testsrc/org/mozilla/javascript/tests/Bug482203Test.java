/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import java.io.InputStreamReader;
import org.junit.Test;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class Bug482203Test {

    @Test
    public void jsApi() throws Exception {
        try (Context cx = Context.enter()) {
            cx.setOptimizationLevel(-1);
            InputStreamReader in =
                    new InputStreamReader(Bug482203Test.class.getResourceAsStream("Bug482203.js"));
            Script script = cx.compileReader(in, "", 1, null);
            Scriptable scope = cx.initStandardObjects();
            script.exec(cx, scope);
            int counter = 0;
            for (; ; ) {
                Object cont = ScriptableObject.getProperty(scope, "c");
                if (cont == null) {
                    break;
                }
                counter++;
                ((Callable) cont).call(cx, scope, scope, new Object[] {null});
            }
            assertEquals(counter, 5);
            assertEquals(Double.valueOf(3), ScriptableObject.getProperty(scope, "result"));
        }
    }

    @Test
    public void javaApi() throws Exception {
        try (Context cx = Context.enter()) {
            cx.setOptimizationLevel(-1);
            InputStreamReader in =
                    new InputStreamReader(Bug482203Test.class.getResourceAsStream("Bug482203.js"));
            Script script = cx.compileReader(in, "", 1, null);
            Scriptable scope = cx.initStandardObjects();
            cx.executeScriptWithContinuations(script, scope);
            int counter = 0;
            for (; ; ) {
                Object cont = ScriptableObject.getProperty(scope, "c");
                if (cont == null) {
                    break;
                }
                counter++;
                cx.resumeContinuation(cont, scope, null);
            }
            assertEquals(counter, 5);
            assertEquals(Double.valueOf(3), ScriptableObject.getProperty(scope, "result"));
        }
    }
}
