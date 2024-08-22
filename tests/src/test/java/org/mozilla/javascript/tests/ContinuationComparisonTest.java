/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertTrue;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Interpreter;
import org.mozilla.javascript.NativeContinuation;
import org.mozilla.javascript.ScriptableObject;

public class ContinuationComparisonTest {

    @Test
    public void test1() throws Exception {
        // Create two identical executions
        NativeContinuation c1 = createContinuation();
        NativeContinuation c2 = createContinuation();

        assertTrue(NativeContinuation.equalImplementations(c1, c2));
    }

    private NativeContinuation createContinuation() throws Exception {
        try (Context cx = Context.enter()) {
            cx.setOptimizationLevel(-1); // interpreter for continuations
            ScriptableObject global = cx.initStandardObjects();
            final AtomicReference<NativeContinuation> captured = new AtomicReference<>();
            ScriptableObject.putProperty(
                    global,
                    "capture",
                    (Callable)
                            (c, scope, thisObj, args) -> {
                                captured.set(Interpreter.captureContinuation(c));
                                return null;
                            });

            // Evaluate program
            try (Reader r =
                    new InputStreamReader(
                            getClass().getResourceAsStream("ContinuationComparisonTest.js"))) {
                cx.executeScriptWithContinuations(
                        cx.compileReader(r, "ContinuationComparisonTest.js", 1, null), global);
            }
            // Make the global standard again
            ScriptableObject.deleteProperty(global, "capture");

            return captured.get();
        }
    }
}
