package org.mozilla.javascript;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context.EvaluationMethod;
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

    private static final String GENERATOR_SCRIPT =
            """
                function *f() {
                    for (var i = 0; i < 2; i++) {
                        yield i;
                    }
                }

                var iter = f.apply('hello');
                var n = iter.next();
                while (!n.done) {
                    n = iter.next();
                }""";

    static final int NUM_BREAKPOINTS = 10;
    static final int NUM_FRAMES = 3;

    private static class MockDebugger implements Debugger {

        private MockDebugFrame frame;

        public synchronized MockDebugFrame getFrame() {
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

        private ArrayList<String> events = new ArrayList<>();

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
            events.add(String.format("Enter %s", thisObj));
        }

        @Override
        public void onExit(Context cx, boolean byThrow, Object resultOrException) {
            events.add("Exit");
        }
    }

    @Test
    public void testThis() {
        MockDebugger debugger = new MockDebugger();
        MockDebugFrame frame = debugger.getFrame();
        Context cx = Context.enter();
        TopLevel scope = cx.initSafeStandardObjects();
        try {
            cx.setDebugger(debugger, cx);
            cx.setGeneratingSource(true);
            cx.setGeneratingDebug(true);
            cx.setEvaluationMethod(EvaluationMethod.Interpreter);
            Script script = cx.compileString(TEST_SCRIPT, "this-klass", 0, null);
            Object res = script.exec(cx, scope, scope.getGlobalThis());
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

    @Test
    public void testGenerator() {
        MockDebugger debugger = new MockDebugger();
        MockDebugFrame frame = debugger.getFrame();
        Context cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_ES6);
        TopLevel scope = cx.initSafeStandardObjects();
        try {
            cx.setDebugger(debugger, cx);
            cx.setGeneratingSource(true);
            cx.setGeneratingDebug(true);
            cx.setEvaluationMethod(EvaluationMethod.Interpreter);
            Script script = cx.compileString(GENERATOR_SCRIPT, "this-klass", 0, null);
            script.exec(cx, scope, scope.getGlobalThis());
            Assertions.assertEquals(10, frame.events.size());
            Assertions.assertEquals("Enter", frame.events.get(0).substring(0, 5));
            Assertions.assertEquals("Enter hello", frame.events.get(1));
            Assertions.assertEquals("Exit", frame.events.get(2));
            Assertions.assertEquals("Enter hello", frame.events.get(3));
            Assertions.assertEquals("Exit", frame.events.get(4));
            Assertions.assertEquals("Enter hello", frame.events.get(5));
            Assertions.assertEquals("Exit", frame.events.get(6));
            Assertions.assertEquals("Enter hello", frame.events.get(7));
            Assertions.assertEquals("Exit", frame.events.get(8));
            Assertions.assertEquals("Exit", frame.events.get(9));
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Context.exit();
        }
    }
}
