package org.mozilla.javascript;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.debug.DebugFrame;
import org.mozilla.javascript.debug.DebuggableScript;
import org.mozilla.javascript.debug.Debugger;

public class DebuggerTest {
    private static final String TEST_SCRIPT =
            "function ScriptDebuggerTest() {\n"
                    + "  var a = 'foo';\n"
                    + "  var b = 'bar';\n"
                    + "  this._sInternalAttribute = 'foobar';\n"
                    + "}\n"
                    + "\n"
                    + "ScriptDebuggerTest.prototype.debug = function() {\n"
                    + "  var x = 'foo';\n"
                    + "  var y = 'bar';\n"
                    + "  return 'Debug: ' + this._sInternalAttribute;\n"
                    + "};\n"
                    + "\n"
                    + "new ScriptDebuggerTest().debug()";

    static final int NUM_BREAKPOINTS = 10;
    static final int NUM_FRAMES = 3;

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
        private ArrayList<Set<String>> frameIds = new ArrayList<>();

        public int getBreakPointsHit() {
            return breakPointsHit;
        }

        public ArrayList<Set<String>> getFrameIds() {
            return frameIds;
        }

        public synchronized void incrementBreakPointsHit() {
            ++breakPointsHit;
        }

        @Override
        public void onLineChange(Context cx, int line) {
            incrementBreakPointsHit();
        }

        @Override
        public void onEnter(Context cx, VarScope activation, Scriptable thisObj, Object[] args) {
            var names = new HashSet<String>();
            for (var id : activation.getIds()) {
                names.add(id.toString());
            }
            frameIds.add(names);
        }
    }

    @Test
    public void testThis() {
        MockDebugger debugger = new MockDebugger();
        MockDebugFrame frame = MockDebugger.getFrame();
        Context cx = Context.enter();
        TopLevel scope = cx.initSafeStandardObjects();
        try {
            cx.setDebugger(debugger, cx);
            cx.setGeneratingSource(true);
            cx.setGeneratingDebug(true);
            cx.setInterpretedMode(true);
            Script script = cx.compileString(TEST_SCRIPT, "this-klass", 0, null);
            Object res = script.exec(cx, scope, scope);
            Assertions.assertNotNull(res);
            System.err.println(res.toString());
            Assertions.assertEquals(
                    NUM_BREAKPOINTS,
                    frame.getBreakPointsHit(),
                    "The expected number of breakpoints were not hit");
            Assertions.assertEquals(
                    NUM_FRAMES,
                    frame.getFrameIds().size(),
                    "The expected number of frames were not entered");
            Assertions.assertEquals(
                    Set.of("ScriptDebuggerTest"),
                    frame.getFrameIds().get(0),
                    "The top frame to be {`ScriptDebuggerTest`}");
            Assertions.assertEquals(
                    Set.of("a", "b"),
                    frame.getFrameIds().get(1),
                    "The second frame to be {`a`, 'b'}");
            Assertions.assertEquals(
                    Set.of("x", "y"),
                    frame.getFrameIds().get(2),
                    "The second frame to be {`x`, 'y'}");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Context.exit();
        }
    }
}
