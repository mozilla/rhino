package org.mozilla.javascript.benchmarks;

import java.io.FileReader;
import java.io.IOException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.openjdk.jmh.annotations.*;

public class MathBenchmark {
    @State(Scope.Thread)
    public static class GenericState {
        Context cx;
        Scriptable scope;

        public void setup() throws IOException {
            cx = Context.enter();
            cx.setOptimizationLevel(9);
            cx.setLanguageVersion(Context.VERSION_ES6);
            scope = cx.initStandardObjects();

            try (FileReader rdr = new FileReader("testsrc/benchmarks/micro/math-benchmarks.js")) {
                cx.evaluateReader(scope, rdr, "math-benchmarks.js", 1, null);
            }
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            cx.close();
        }
    }

    @State(Scope.Thread)
    public static class AddIntsState extends GenericState {
        Function func;

        @Setup(Level.Trial)
        public void create() throws IOException {
            super.setup();
            func = (Function) ScriptableObject.getProperty(scope, "addInts");
        }
    }

    @Benchmark
    public Object addInts(AddIntsState state) {
        return state.func.call(state.cx, state.scope, null, ScriptRuntime.emptyArgs);
    }

    @State(Scope.Thread)
    public static class AddFloatsState extends GenericState {
        Function func;

        @Setup(Level.Trial)
        public void create() throws IOException {
            super.setup();
            func = (Function) ScriptableObject.getProperty(scope, "addFloats");
        }
    }

    @Benchmark
    public Object addFloats(AddFloatsState state) {
        return state.func.call(state.cx, state.scope, null, ScriptRuntime.emptyArgs);
    }

    @State(Scope.Thread)
    public static class AddTwoFloatsState extends GenericState {
        Function func;

        @Setup(Level.Trial)
        public void create() throws IOException {
            super.setup();
            func = (Function) ScriptableObject.getProperty(scope, "addTwoFloats");
        }
    }

    @Benchmark
    public Object addTwoFloats(AddTwoFloatsState state) {
        return state.func.call(state.cx, state.scope, null, ScriptRuntime.emptyArgs);
    }

    @State(Scope.Thread)
    public static class AddStringsInLoopState extends GenericState {
        Function func;

        @Setup(Level.Trial)
        public void create() throws IOException {
            super.setup();
            func = (Function) ScriptableObject.getProperty(scope, "addStringsInLoop");
        }
    }

    @Benchmark
    public Object addStringsInLoop(AddStringsInLoopState state) {
        return state.func.call(state.cx, state.scope, null, ScriptRuntime.emptyArgs);
    }

    @State(Scope.Thread)
    public static class AddMixedStringsState extends GenericState {
        Function func;

        @Setup(Level.Trial)
        public void create() throws IOException {
            super.setup();
            func = (Function) ScriptableObject.getProperty(scope, "addMixedStrings");
        }
    }

    @Benchmark
    public Object addMixedStrings(AddMixedStringsState state) {
        return state.func.call(state.cx, state.scope, null, ScriptRuntime.emptyArgs);
    }

    @State(Scope.Thread)
    public static class SubtractIntsState extends GenericState {
        Function func;

        @Setup(Level.Trial)
        public void create() throws IOException {
            super.setup();
            func = (Function) ScriptableObject.getProperty(scope, "subtractInts");
        }
    }

    @Benchmark
    public Object subtractInts(SubtractIntsState state) {
        return state.func.call(state.cx, state.scope, null, ScriptRuntime.emptyArgs);
    }

    @State(Scope.Thread)
    public static class SubtractFloatsState extends GenericState {
        Function func;

        @Setup(Level.Trial)
        public void create() throws IOException {
            super.setup();
            func = (Function) ScriptableObject.getProperty(scope, "subtractFloats");
        }
    }

    @Benchmark
    public Object subtractFloats(SubtractFloatsState state) {
        return state.func.call(state.cx, state.scope, null, ScriptRuntime.emptyArgs);
    }

    @State(Scope.Thread)
    public static class SubtractTwoFloatsState extends GenericState {
        Function func;

        @Setup(Level.Trial)
        public void create() throws IOException {
            super.setup();
            func = (Function) ScriptableObject.getProperty(scope, "subtractTwoFloats");
        }
    }

    @Benchmark
    public Object subtractTwoFloats(SubtractTwoFloatsState state) {
        return state.func.call(state.cx, state.scope, null, ScriptRuntime.emptyArgs);
    }

    @State(Scope.Thread)
    public static class AndState extends GenericState {
        Function func;

        @Setup(Level.Trial)
        public void create() throws IOException {
            super.setup();
            func = (Function) ScriptableObject.getProperty(scope, "bitwiseAnd");
        }
    }

    @Benchmark
    public Object bitwiseAnd(AndState state) {
        return state.func.call(state.cx, state.scope, null, ScriptRuntime.emptyArgs);
    }

    @State(Scope.Thread)
    public static class OrState extends GenericState {
        Function func;

        @Setup(Level.Trial)
        public void create() throws IOException {
            super.setup();
            func = (Function) ScriptableObject.getProperty(scope, "bitwiseOr");
        }
    }

    @Benchmark
    public Object bitwiseOr(OrState state) {
        return state.func.call(state.cx, state.scope, null, ScriptRuntime.emptyArgs);
    }

    @State(Scope.Thread)
    public static class LshState extends GenericState {
        Function func;

        @Setup(Level.Trial)
        public void create() throws IOException {
            super.setup();
            func = (Function) ScriptableObject.getProperty(scope, "bitwiseLsh");
        }
    }

    @Benchmark
    public Object bitwiseLsh(LshState state) {
        return state.func.call(state.cx, state.scope, null, ScriptRuntime.emptyArgs);
    }

    @State(Scope.Thread)
    public static class RshState extends GenericState {
        Function func;

        @Setup(Level.Trial)
        public void create() throws IOException {
            super.setup();
            func = (Function) ScriptableObject.getProperty(scope, "bitwiseRsh");
        }
    }

    @Benchmark
    public Object bitwiseRsh(RshState state) {
        return state.func.call(state.cx, state.scope, null, ScriptRuntime.emptyArgs);
    }

    @State(Scope.Thread)
    public static class SignedRshState extends GenericState {
        Function func;

        @Setup(Level.Trial)
        public void create() throws IOException {
            super.setup();
            func = (Function) ScriptableObject.getProperty(scope, "bitwiseSignedRsh");
        }
    }

    @Benchmark
    public Object bitwiseSignedRsh(SignedRshState state) {
        return state.func.call(state.cx, state.scope, null, ScriptRuntime.emptyArgs);
    }
}
