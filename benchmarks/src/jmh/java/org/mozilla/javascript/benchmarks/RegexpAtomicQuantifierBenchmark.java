package org.mozilla.javascript.benchmarks;

import java.util.concurrent.TimeUnit;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.openjdk.jmh.annotations.*;

@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(1)
@Warmup(iterations = 2, time = 2)
@Measurement(iterations = 3, time = 2)
public class RegexpAtomicQuantifierBenchmark {

    @State(Scope.Thread)
    public static class RegExpState {
        Context cx;
        Scriptable scope;
        Callable testFunc;

        @Setup(Level.Trial)
        public void setup() {
            cx = Context.enter();
            cx.setLanguageVersion(Context.VERSION_ES6);
            scope = cx.initStandardObjects();

            StringBuilder sb = new StringBuilder(5000);
            for (int i = 0; i < 5000; i++) {
                sb.append('\u03B1');
            }
            String input = sb.toString() + "X";

            ScriptableObject.putProperty(scope, "input", input);
            cx.evaluateString(scope, "var re = /\\p{Script=Greek}+$/u;", "setup", 1, null);
            cx.evaluateString(scope, "function test() { return re.test(input); }", "func", 1, null);
            testFunc = (Callable) ScriptableObject.getProperty(scope, "test");
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            Context.exit();
        }
    }

    @Benchmark
    public Object greekPropertyEscape(RegExpState state) {
        return state.testFunc.call(state.cx, state.scope, null, new Object[0]);
    }
}
