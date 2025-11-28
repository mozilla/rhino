package org.mozilla.javascript;

import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.debug.DebugFrame;
import org.mozilla.javascript.debug.DebuggableScript;
import org.mozilla.javascript.debug.Debugger;

public class DebuggerTest {
    private static final String TEST_SCRIPT =
            "function ScriptDebuggerTest() {\n"
                    + "	this._sInternalAttribute = 'foobar';\n"
                    + "}\n"
                    + "\n"
                    + "ScriptDebuggerTest.prototype.debug = function() {\n"
                    + "	return \"Debug: \" + this._sInternalAttribute;\n"
                    + "};\n"
                    + "\n"
                    + "new ScriptDebuggerTest().debug()";

    private static final String GLOBAL = "global";
    private static final String THIS_OBJ_ATTRIBUTE = "_sInternalAttribute";
    static final int BREAKPOINT_LINE_OUTSIDE_FUNC = 8;
    static final int BREAKPOINT_LINE_INSIDE_FUNC = 5;
    static final int NUM_BREAKPOINTS = 2;

    private static class MockDebugger implements Debugger {

        private static MockDebugFrame frame;

        public static synchronized MockDebugFrame getFrame() {
            if (frame == null) frame = new MockDebugFrame();
            return frame;
        }

        @Override
        public DebugFrame getFrame(Context cx, DebuggableScript fnOrScript) {
            return frame;
        }
    }

    private static class MockDebugFrame implements DebugFrame {

        private int breakPointsHit = 0;

        public int getBreakPointsHit() {
            return breakPointsHit;
        }

        public synchronized void incrementBreakPointsHit() {
            ++breakPointsHit;
        }

        @Override
        public void onLineChange(Context cx, int line) {
            // decide to break on a line and evaluate a command similar to what GlideScriptDebugger
            // does
            if (line == BREAKPOINT_LINE_INSIDE_FUNC || line == BREAKPOINT_LINE_OUTSIDE_FUNC) {
                incrementBreakPointsHit();
            }
        }
    }

    @Test
    public void testThis() {
        MockDebugger debugger = new MockDebugger();
        MockDebugFrame frame = MockDebugger.getFrame();
        Context cx = Context.enter();
        Scriptable scope = cx.initSafeStandardObjects();
        try {
            cx.setDebugger(debugger, cx);
            cx.setGeneratingSource(true);
            cx.setGeneratingDebug(true);
            cx.setInterpretedMode(true);
            Script script = cx.compileString(TEST_SCRIPT, "this-klass", 0, null);
            Object res = script.exec(cx, scope, scope);
            Assert.assertNotNull(res);
            if (frame.getBreakPointsHit() != NUM_BREAKPOINTS)
                Assert.fail("Breakpoints weren't hit");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Context.exit();
        }
    }
}
