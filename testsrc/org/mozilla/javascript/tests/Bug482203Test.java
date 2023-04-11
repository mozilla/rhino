/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import java.io.IOException;
import java.io.InputStreamReader;
import junit.framework.TestCase;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class Bug482203Test extends TestCase {

    public void testJsApi() throws Exception {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    Scriptable scope = cx.initStandardObjects();

                    try {
                        InputStreamReader in =
                                new InputStreamReader(
                                        Bug482203Test.class.getResourceAsStream("Bug482203.js"));

                        Script script = cx.compileReader(in, "", 1, null);
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
                        assertEquals(
                                Double.valueOf(3), ScriptableObject.getProperty(scope, "result"));
                    } catch (IOException e) {
                        fail(e.getMessage());
                    }

                    return null;
                });
    }

    public void testJavaApi() throws Exception {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    Scriptable scope = cx.initStandardObjects();

                    try {
                        InputStreamReader in =
                                new InputStreamReader(
                                        Bug482203Test.class.getResourceAsStream("Bug482203.js"));

                        Script script = cx.compileReader(in, "", 1, null);
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
                        assertEquals(
                                Double.valueOf(3), ScriptableObject.getProperty(scope, "result"));
                    } catch (IOException e) {
                        fail(e.getMessage());
                    }

                    return null;
                });

        try (Context cx = Context.enter()) {
            cx.setOptimizationLevel(-1);
        }
    }
}
