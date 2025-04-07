package org.mozilla.javascript.benchmarks;

import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.openjdk.jmh.annotations.*;

@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class ThrowBenchmark {
    @State(Scope.Thread)
    public static class GeneratorState {
        Context cx;
        Scriptable scope;

        Function shallowThrow;
        Function mediumThrow;
        Function deepThrow;

        @Param({"false", "true"})
        public boolean interpreted;

        @Setup(Level.Trial)
        public void setup() throws IOException {
            cx = Context.enter();
            cx.setInterpretedMode(interpreted);
            cx.setLanguageVersion(Context.VERSION_ES6);
            scope = cx.initStandardObjects();

            try (FileReader rdr = new FileReader("testsrc/benchmarks/micro/throw-benchmarks.js")) {
                cx.evaluateReader(scope, rdr, "throw-benchmarks.js", 1, null);
            }
            shallowThrow = (Function) ScriptableObject.getProperty(scope, "shallowThrow");
            mediumThrow = (Function) ScriptableObject.getProperty(scope, "mediumThrow");
            deepThrow = (Function) ScriptableObject.getProperty(scope, "deepThrow");
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            cx.close();
        }
    }

    @Benchmark
    public Object shallowThrow(GeneratorState state) {
        return state.shallowThrow.call(state.cx, state.scope, null, ScriptRuntime.emptyArgs);
    }

    @Benchmark
    public Object mediumThrow(GeneratorState state) {
        return state.mediumThrow.call(state.cx, state.scope, null, ScriptRuntime.emptyArgs);
    }

    @Benchmark
    public Object deepThrow(GeneratorState state) {
        return state.deepThrow.call(state.cx, state.scope, null, ScriptRuntime.emptyArgs);
    }
}
