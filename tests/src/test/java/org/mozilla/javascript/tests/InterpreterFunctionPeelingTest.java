package org.mozilla.javascript.tests;

import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContinuationPending;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

// Tests that continuations work across arrow function, bound function, and apply/call invocations.
public class InterpreterFunctionPeelingTest {
    public static final Runnable CAPTURER =
            () -> {
                try (var cx = Context.enter()) {
                    throw cx.captureContinuation();
                }
            };

    public static void executeScript(String script) {
        try (var cx = Context.enter()) {
            cx.setOptimizationLevel(-1);
            Script s = cx.compileString(script, "unknown source", 0, null);
            Scriptable scope = cx.initStandardObjects();
            scope.put("c", scope, Context.javaToJS(CAPTURER, scope));
            Assert.assertThrows(
                    ContinuationPending.class,
                    () -> {
                        cx.executeScriptWithContinuations(s, scope);
                    });
        }
    }

    @Test
    public void testBind() {
        executeScript("function capture(){c.run()};capture.bind(this)()");
    }

    @Test
    public void testBindCall() {
        executeScript("function capture(){c.run()};capture.bind(this).call()");
    }

    @Test
    public void testBindApply() {
        executeScript("function capture(){c.run()};capture.bind(this).apply()");
    }

    @Test
    public void testArrow() {
        executeScript("capture=()=>{c.run()};capture()");
    }

    @Test
    public void testArrowCall() {
        executeScript("capture=()=>{c.run()};capture.call()");
    }

    @Test
    public void testArrowApply() {
        executeScript("capture=()=>{c.run()};capture.apply()");
    }

    @Test
    public void testArrowBindCall() {
        executeScript("capture=()=>{c.run()};capture.bind(this).call()");
    }

    @Test
    public void testArrowBindApply() {
        executeScript("capture=()=>{c.run()};capture.bind(this).apply()");
    }

    @Test
    public void testArrowImmediate() {
        executeScript("(()=>{c.run()})()");
    }
}
