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
    public static class MathState {
        Context cx;
        Scriptable scope;

        Function addConstantInts;
        Function addIntAndConstant;
        Function addTwoInts;
        Function addConstantFloats;
        Function addTwoFloats;
        Function addStringsInLoop;
        Function addMixedStrings;
        Function subtractInts;
        Function subtractFloats;
        Function subtractTwoFloats;
        Function bitwiseAnd;
        Function bitwiseOr;
        Function bitwiseLsh;
        Function bitwiseRsh;
        Function bitwiseSignedRsh;

        @Setup(Level.Trial)
        public void setup() throws IOException {
            cx = Context.enter();
            cx.setOptimizationLevel(9);
            cx.setLanguageVersion(Context.VERSION_ES6);
            scope = cx.initStandardObjects();

            try (FileReader rdr = new FileReader("testsrc/benchmarks/micro/math-benchmarks.js")) {
                cx.evaluateReader(scope, rdr, "math-benchmarks.js", 1, null);
            }
            addConstantInts = (Function) ScriptableObject.getProperty(scope, "addConstantInts");
            addIntAndConstant = (Function) ScriptableObject.getProperty(scope, "addIntAndConstant");
            addTwoInts = (Function) ScriptableObject.getProperty(scope, "addTwoInts");
            addConstantFloats = (Function) ScriptableObject.getProperty(scope, "addConstantFloats");
            addTwoFloats = (Function) ScriptableObject.getProperty(scope, "addTwoFloats");
            addStringsInLoop = (Function) ScriptableObject.getProperty(scope, "addStringsInLoop");
            addMixedStrings = (Function) ScriptableObject.getProperty(scope, "addMixedStrings");
            subtractInts = (Function) ScriptableObject.getProperty(scope, "subtractInts");
            subtractFloats = (Function) ScriptableObject.getProperty(scope, "subtractFloats");
            subtractTwoFloats = (Function) ScriptableObject.getProperty(scope, "subtractTwoFloats");
            bitwiseAnd = (Function) ScriptableObject.getProperty(scope, "bitwiseAnd");
            bitwiseOr = (Function) ScriptableObject.getProperty(scope, "bitwiseOr");
            bitwiseLsh = (Function) ScriptableObject.getProperty(scope, "bitwiseLsh");
            bitwiseRsh = (Function) ScriptableObject.getProperty(scope, "bitwiseRsh");
            bitwiseSignedRsh = (Function) ScriptableObject.getProperty(scope, "bitwiseSignedRsh");
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            cx.close();
        }
    }

    @Benchmark
    public Object addConstantInts(MathState state) {
        return state.addConstantInts.call(state.cx, state.scope, null, ScriptRuntime.emptyArgs);
    }

    @Benchmark
    public Object addIntAndConstant(MathState state) {
        return state.addIntAndConstant.call(state.cx, state.scope, null, ScriptRuntime.emptyArgs);
    }

    @Benchmark
    public Object addTwoInts(MathState state) {
        return state.addTwoInts.call(state.cx, state.scope, null, ScriptRuntime.emptyArgs);
    }

    @Benchmark
    public Object addConstantFloats(MathState state) {
        return state.addConstantFloats.call(state.cx, state.scope, null, ScriptRuntime.emptyArgs);
    }

    @Benchmark
    public Object addTwoFloats(MathState state) {
        return state.addTwoFloats.call(state.cx, state.scope, null, ScriptRuntime.emptyArgs);
    }

    @Benchmark
    public Object addStringsInLoop(MathState state) {
        return state.addStringsInLoop.call(state.cx, state.scope, null, ScriptRuntime.emptyArgs);
    }

    @Benchmark
    public Object addMixedStrings(MathState state) {
        return state.addMixedStrings.call(state.cx, state.scope, null, ScriptRuntime.emptyArgs);
    }

    @Benchmark
    public Object subtractInts(MathState state) {
        return state.subtractInts.call(state.cx, state.scope, null, ScriptRuntime.emptyArgs);
    }

    @Benchmark
    public Object subtractFloats(MathState state) {
        return state.subtractFloats.call(state.cx, state.scope, null, ScriptRuntime.emptyArgs);
    }

    @Benchmark
    public Object subtractTwoFloats(MathState state) {
        return state.subtractTwoFloats.call(state.cx, state.scope, null, ScriptRuntime.emptyArgs);
    }

    @Benchmark
    public Object bitwiseAnd(MathState state) {
        return state.bitwiseAnd.call(state.cx, state.scope, null, ScriptRuntime.emptyArgs);
    }

    @Benchmark
    public Object bitwiseOr(MathState state) {
        return state.bitwiseOr.call(state.cx, state.scope, null, ScriptRuntime.emptyArgs);
    }

    @Benchmark
    public Object bitwiseLsh(MathState state) {
        return state.bitwiseLsh.call(state.cx, state.scope, null, ScriptRuntime.emptyArgs);
    }

    @Benchmark
    public Object bitwiseRsh(MathState state) {
        return state.bitwiseRsh.call(state.cx, state.scope, null, ScriptRuntime.emptyArgs);
    }

    @Benchmark
    public Object bitwiseSignedRsh(MathState state) {
        return state.bitwiseSignedRsh.call(state.cx, state.scope, null, ScriptRuntime.emptyArgs);
    }
}
