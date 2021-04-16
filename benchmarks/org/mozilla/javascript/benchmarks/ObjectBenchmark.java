package org.mozilla.javascript.benchmarks;

import java.io.FileReader;
import java.io.IOException;
import java.util.Random;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.shell.Global;
import org.openjdk.jmh.annotations.*;

public class ObjectBenchmark {
    static final Random rand = new Random();

    static final int intKeys = 1000;
    static final int stringKeys = 1000;
    // "count" should match "@OperationsPerInvocation" annotations
    static final int count = 1000;

    static void runCode(Context cx, Scriptable scope, String fileName) throws IOException {
        try (FileReader rdr = new FileReader(fileName)) {
            cx.evaluateReader(scope, rdr, "test.js", 1, null);
        }
    }

    @State(Scope.Thread)
    public static class FieldTestState {
        Context cx;
        Scriptable scope;
        Scriptable strings;
        Scriptable ints;

        @Setup(Level.Trial)
        @SuppressWarnings("unused")
        public void create() throws IOException {
            cx = Context.enter();
            cx.setOptimizationLevel(9);
            cx.setLanguageVersion(Context.VERSION_ES6);

            scope = new Global(cx);
            runCode(cx, scope, "testsrc/benchmarks/caliper/fieldTests.js");

            Object[] sarray = new Object[stringKeys];
            for (int i = 0; i < stringKeys; i++) {
                int len = rand.nextInt(49) + 1;
                char[] c = new char[len];
                for (int cc = 0; cc < len; cc++) {
                    c[cc] = (char) ('a' + rand.nextInt(25));
                }
                sarray[i] = new String(c);
            }
            strings = cx.newArray(scope, sarray);

            Object[] iarray = new Object[intKeys];
            for (int i = 0; i < intKeys; i++) {
                iarray[i] = rand.nextInt(10000);
            }
            ints = cx.newArray(scope, iarray);
        }

        @TearDown(Level.Trial)
        @SuppressWarnings("unused")
        public void close() {
            Context.exit();
        }
    }

    @Benchmark
    @OperationsPerInvocation(1000)
    @SuppressWarnings("unused")
    public void createFields(FieldTestState state) {
        Function create = (Function) ScriptableObject.getProperty(state.scope, "createObject");
        create.call(state.cx, state.scope, null, new Object[] {count, state.strings, state.ints});
    }

    @Benchmark
    @OperationsPerInvocation(1000)
    @SuppressWarnings("unused")
    public void accessFields(FieldTestState state) {
        Function create = (Function) ScriptableObject.getProperty(state.scope, "createObject");
        Object o =
                create.call(
                        state.cx, state.scope, null, new Object[] {1, state.strings, state.ints});
        Function access = (Function) ScriptableObject.getProperty(state.scope, "accessObject");
        access.call(
                state.cx, state.scope, null, new Object[] {count, o, state.strings, state.ints});
    }

    @Benchmark
    @OperationsPerInvocation(1000)
    @SuppressWarnings("unused")
    public void iterateFields(FieldTestState state) {
        Function create = (Function) ScriptableObject.getProperty(state.scope, "createObject");
        Object o =
                create.call(
                        state.cx, state.scope, null, new Object[] {1, state.strings, state.ints});
        Function iterate = (Function) ScriptableObject.getProperty(state.scope, "iterateObject");
        iterate.call(state.cx, state.scope, null, new Object[] {count, o});
    }

    @Benchmark
    @OperationsPerInvocation(1000)
    @SuppressWarnings("unused")
    public void ownKeysFields(FieldTestState state) {
        Function create = (Function) ScriptableObject.getProperty(state.scope, "createObject");
        Object o =
                create.call(
                        state.cx, state.scope, null, new Object[] {1, state.strings, state.ints});
        Function iterate =
                (Function) ScriptableObject.getProperty(state.scope, "iterateOwnKeysObject");
        iterate.call(state.cx, state.scope, null, new Object[] {count, o});
    }

    @Benchmark
    @OperationsPerInvocation(1000)
    @SuppressWarnings("unused")
    public void deleteFields(FieldTestState state) {
        Function create = (Function) ScriptableObject.getProperty(state.scope, "createObject");
        Object o =
                create.call(
                        state.cx, state.scope, null, new Object[] {1, state.strings, state.ints});
        Function delete = (Function) ScriptableObject.getProperty(state.scope, "deleteObject");
        delete.call(
                state.cx, state.scope, null, new Object[] {count, o, state.strings, state.ints});
    }
}
