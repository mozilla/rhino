/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.drivers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.LambdaFunction;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.tools.shell.Global;

/**
 * This class is used for creating test scripts that are loaded from JS scripts. Each test must be
 * represented by a class that extends this class, and has a "RhinoTest" annotation at the class
 * level that returns the file name of the script to execute.
 */
@RunWith(BlockJUnit4ClassRunner.class)
public abstract class ScriptTestsBase {

    private Object executeRhinoScript(int optLevel) {
        RhinoTest anno = this.getClass().getAnnotation(RhinoTest.class);
        assertNotNull(anno);

        int jsVersion = Context.VERSION_1_8;
        LanguageVersion jsVersionAnnotation = this.getClass().getAnnotation(LanguageVersion.class);
        if (jsVersionAnnotation != null) {
            jsVersion = jsVersionAnnotation.value();
            Context.checkLanguageVersion(jsVersion);
        }

        Reader script = null;
        String suiteName = null;

        try (Context cx = Context.enter()) {
            if (!"".equals(anno.value())) {
                script =
                        new InputStreamReader(
                                new FileInputStream(anno.value()), StandardCharsets.UTF_8);
                suiteName = anno.value();
            } else if (!"".equals(anno.inline())) {
                script =
                        new StringReader(
                                "load('testsrc/assert.js');\n"
                                        + anno.inline()
                                        + "\n"
                                        + "'success';");
                suiteName = "inline.js";
            }

            cx.setOptimizationLevel(optLevel);
            cx.setLanguageVersion(jsVersion);

            Global global = new Global(cx);
            loadNatives(global);

            Scriptable scope = cx.newObject(global);
            scope.setPrototype(global);
            scope.setParentScope(null);

            return cx.evaluateReader(scope, script, suiteName, 1, null);
        } catch (JavaScriptException ex) {
            fail(String.format("%s%n%s", ex.getMessage(), ex.getScriptStackTrace()));
            return null;
        } catch (TestFailureException tfe) {
            fail(tfe.getMessage());
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (null != script) script.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    /**
     * Load native functions that are used by the Promise tests:
     *
     * <p>AbortJS throws a Java exception that can be caught in the test code, but not by JavaScript
     * code.
     *
     * <p>EnqueueMicrotask enqueues a function to be called at the end of the current test run.
     *
     * <p>PerformMicrotaskCheckpoint ensures that all the pending microtasks are executed
     * immediately.
     */
    private void loadNatives(Scriptable scope) {
        ScriptableObject.putProperty(
                scope,
                "AbortJS",
                new LambdaFunction(
                        scope,
                        "AbortJS",
                        1,
                        (Context lcx, Scriptable lscope, Scriptable localThis, Object[] args) -> {
                            assert (args.length > 0);
                            throw new TestFailureException(ScriptRuntime.toString(args[0]));
                        }));

        ScriptableObject.putProperty(
                scope,
                "EnqueueMicrotask",
                new LambdaFunction(
                        scope,
                        "EnqueueMicrotask",
                        1,
                        (Context lcx, Scriptable lscope, Scriptable localThis, Object[] args) -> {
                            assert (args.length > 0);
                            assert (args[0] instanceof Callable);
                            lcx.enqueueMicrotask(
                                    () -> ((Callable) args[0]).call(lcx, lscope, localThis, args));
                            return Undefined.instance;
                        }));

        ScriptableObject.putProperty(
                scope,
                "PerformMicrotaskCheckpoint",
                new LambdaFunction(
                        scope,
                        "PerformMicrotaskCheckpoint",
                        0,
                        (Context lcx, Scriptable lscope, Scriptable localThis, Object[] args) -> {
                            lcx.processMicrotasks();
                            return Undefined.instance;
                        }));
    }

    @Test
    public void rhinoTestNoOpt() {
        assertEquals("success", executeRhinoScript(-1));
    }

    @Test
    public void rhinoTestOpt0() {
        assertEquals("success", executeRhinoScript(0));
    }

    @Test
    public void rhinoTestOpt9() {
        assertEquals("success", executeRhinoScript(9));
    }
}
