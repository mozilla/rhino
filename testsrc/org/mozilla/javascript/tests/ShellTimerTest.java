package org.mozilla.javascript.tests;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.tools.shell.Global;
import org.mozilla.javascript.tools.shell.Timers;

/** Test the setTimeout and clearTimeout functions added to the shell. */
@RunWith(Parameterized.class)
public class ShellTimerTest {
    final int optLevel;
    private Context cx;
    private Scriptable global;
    private final Timers timers = new Timers();

    public ShellTimerTest(int optLevel) {
        this.optLevel = optLevel;
    }

    @Before
    public void init() {
        cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_ES6);
        global = new Global(cx);
        timers.install(global);
        global.put("TestsComplete", global, false);
    }

    @After
    public void cleanup() {
        cx.close();
    }

    @Parameters(name = "{index}, opt={0}")
    public static Collection<Object[]> optLevels() {
        int[] optLevels = Utils.getTestOptLevels();
        ArrayList<Object[]> params = new ArrayList<>();
        for (int ol : optLevels) {
            params.add(new Object[] {ol});
        }
        return params;
    }

    /** Just make sure that a timeout fires. */
    @Test
    public void setImmediateTimeout() throws InterruptedException {
        runTest("setTimeout(() => { TestsComplete = true; });");
    }

    /** Ensure that parameters work. */
    @Test
    public void checkTimeoutParameters() throws InterruptedException {
        runTest(
                "load('testsrc/assert.js');\n"
                        + "setTimeout((a, b) => { \n"
                        + "assertTrue(this != null);\n"
                        + "assertTrue(this != undefined);\n"
                        + "assertEquals('one', a);\n"
                        + "assertEquals(2, b);\n"
                        + "TestsComplete = true; }, 0, 'one', 2);");
    }

    /** Ensure that invalid stuff doesn't happen. */
    @Test
    public void checkTypeChecks() throws InterruptedException {
        runTest(
                "load('testsrc/assert.js');\n"
                        +
                        // Timeout must be a function and we don't support the "eval" style of
                        // setTimeout
                        "assertThrows(() => {setTimeout('x = 1;');}, TypeError);\n"
                        +
                        // Need to pass a function as the first argument
                        "assertThrows(() => {setTimeout();}, TypeError);\n"
                        +
                        // Need to pass a timer ID as the second argument
                        "assertThrows(() => {clearTimeout()}, TypeError);\n"
                        +
                        // OK because you can clear a timeout with an invalid ID.
                        "clearTimeout(999);\n"
                        +
                        // OK because of the semantics of "ToInteger"
                        "setTimeout(() => {TestsComplete = true}, 'later');\n"
                        + "clearTimeout('hello');");
    }

    /** Make sure that timeouts are executed in absolute numerical order. */
    @Test
    public void setTimeoutOrder() throws InterruptedException {
        runTest(
                "load('testsrc/assert.js');\n"
                        + "var count = 0;\n"
                        + "setTimeout(() => { assertEquals(0, count++); });\n"
                        + "setTimeout(() => { assertEquals(1, count++); }, 1);\n"
                        + "setTimeout(() => { assertEquals(2, count); TestsComplete = true; }, 2);");
    }

    /** Make sure that timers can be cancelled. */
    @Test
    public void timerCancellation() throws InterruptedException {
        runTest(
                "load('testsrc/assert.js');\n"
                        + "let count = 0;\n"
                        + "let timer1 = setTimeout(() => { count++; });\n"
                        + "assertEquals('number', typeof timer1);\n"
                        + "setTimeout(() => { assertEquals(0, count++); }, 1);\n"
                        + "setTimeout(() => { assertEquals(1, count); TestsComplete = true; }, 2);\n"
                        + "clearTimeout(timer1);");
    }

    /** Make sure that timers can be nested inside other timers and so on. */
    @Test
    public void nestedTimers() throws InterruptedException {
        runTest(
                "load('testsrc/assert.js');\n"
                        + "let count = 0;\n"
                        + "setTimeout(() => { assertEquals(0, count++);\n"
                        + "  setTimeout(() => { assertEquals(2, count++); TestsComplete = true; }, 5);\n"
                        + "  setTimeout(() => { assertEquals(1, count++); }, 2);\n"
                        + "});");
    }

    private void runTest(String script) throws InterruptedException {
        cx.evaluateString(global, script, "test.js", 1, null);
        timers.runAllTimers(cx, global);
        assertTrue(ScriptRuntime.toBoolean(global.get("TestsComplete", global)));
    }
}
