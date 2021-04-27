/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.javascript.tests;

import junit.framework.TestCase;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.debug.DebugFrame;
import org.mozilla.javascript.debug.DebuggableScript;
import org.mozilla.javascript.debug.Debugger;

/** Ensures that setting generating debug allows to debug a script */
public class GeneratingDebugTest extends TestCase {

    public void testGeneratingDebug() {
        String script = "var x = 1 + 2;";
        Context ctx = Context.enter();
        ScriptableObject scope = ctx.initStandardObjects();
        ctx.setDebugger(new Debugger() {
            @Override public void handleCompilationDone(Context cx, DebuggableScript fnOrScript, String source) { }
            @Override public DebugFrame getFrame(Context cx, DebuggableScript fnOrScript) { return null; }
        }, null);

        ctx.setOptimizationLevel(9);
        ctx.setGeneratingDebug(true); // sets the optimization level back to 0

        // unless the optimization level is set to -1 on evaluation org.mozilla.javascript.Context.createCompiler()
        // a java.lang.RuntimeException: NOT SUPPORTED is thrown.
        ctx.evaluateString(scope, script, "script", 1, null);

    }

}
