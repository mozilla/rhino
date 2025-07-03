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
public class GeneratorBenchmark {
    @State(Scope.Thread)
    public static class GeneratorState {
        Context cx;
        Scriptable scope;

        Function nativeGenerator;
        Function transpiledGenerator;
        Function noReturnGenerator;

        @Param({"false", "true"})
        public boolean interpreted;

        @Setup(Level.Trial)
        public void setup() throws IOException {
            cx = Context.enter();
            cx.setInterpretedMode(interpreted);
            cx.setLanguageVersion(Context.VERSION_ES6);
            scope = cx.initStandardObjects();

            try (FileReader rdr =
                    new FileReader("testsrc/benchmarks/micro/generator-benchmarks.js")) {
                cx.evaluateReader(scope, rdr, "generator-benchmarks.js", 1, null);
            }
            nativeGenerator = (Function) ScriptableObject.getProperty(scope, "nativeGenerator");
            transpiledGenerator =
                    (Function) ScriptableObject.getProperty(scope, "transpiledGenerator");
            noReturnGenerator = (Function) ScriptableObject.getProperty(scope, "noReturnGenerator");
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            cx.close();
        }
    }

    @Benchmark
    public Object nativeGenerator(GeneratorState state) {
        return state.nativeGenerator.call(state.cx, state.scope, null, ScriptRuntime.emptyArgs);
    }

    @Benchmark
    public Object transpiledGenerator(GeneratorState state) {
        return state.transpiledGenerator.call(state.cx, state.scope, null, ScriptRuntime.emptyArgs);
    }

    @Benchmark
    public Object noReturnGenerator(GeneratorState state) {
        return state.noReturnGenerator.call(state.cx, state.scope, null, ScriptRuntime.emptyArgs);
    }
}
