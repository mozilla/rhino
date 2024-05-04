package org.mozilla.javascript.benchmarks;

import java.io.FileReader;
import java.io.IOException;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.openjdk.jmh.annotations.*;

public class V8Benchmark {
    static Object[] emptyArgs = new Object[] {};

    abstract static class AbstractState {
        Context cx;
        Scriptable scope;

        Callable getFunc(String name) {
            Object f = ScriptableObject.getProperty(scope, name);
            if (!(f instanceof Callable)) {
                throw new RuntimeException("Benchmark function " + name + " not found");
            }
            return (Callable) f;
        }

        Callable getRunFunc(String name) {
            Callable grf = getFunc("getRunFunc");
            return (Callable) grf.call(cx, scope, scope, new Object[] {name});
        }

        void evaluateSource(Context cx, Scriptable scope, String fileName) {
            try (FileReader rdr = new FileReader(fileName)) {
                cx.evaluateReader(scope, rdr, fileName, 1, null);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }

        void initialize() {
            cx = Context.enter();
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setOptimizationLevel(9);
            scope = cx.initStandardObjects();
            evaluateSource(cx, scope, "testsrc/benchmarks/framework.js");
        }

        void cleanup() {
            Context.exit();
        }

        void runSetup() {
            Callable setup = getFunc("setup");
            setup.call(cx, scope, scope, new Object[] {});
        }

        void runCleanup() {
            Callable cleanup = getFunc("cleanup");
            cleanup.call(cx, scope, scope, emptyArgs);
        }
    }

    @State(Scope.Thread)
    public static class SplayState extends AbstractState {
        Callable splay;

        @Setup(Level.Trial)
        public void setUp() {
            initialize();
            evaluateSource(cx, scope, "testsrc/benchmarks/v8-benchmarks-v6/splay.js");
            runSetup();
            splay = getRunFunc("Splay");
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            runCleanup();
            cleanup();
        }
    }

    @Benchmark
    public Object splay(SplayState state) {
        return state.splay.call(state.cx, state.scope, state.scope, emptyArgs);
    }

    @State(Scope.Thread)
    public static class CryptoState extends AbstractState {
        Callable encrypt;
        Callable decrypt;

        @Setup(Level.Trial)
        public void setUp() {
            initialize();
            evaluateSource(cx, scope, "testsrc/benchmarks/v8-benchmarks-v6/crypto.js");
            runSetup();
            encrypt = getRunFunc("Encrypt");
            decrypt = getRunFunc("Decrypt");
            // We need to run encrypt once to set the encrypted value or decrypt will fail
            encrypt.call(cx, scope, scope, emptyArgs);
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            runCleanup();
            cleanup();
        }
    }

    @Benchmark
    public Object cryptoEncrpyt(CryptoState state) {
        return state.encrypt.call(state.cx, state.scope, state.scope, emptyArgs);
    }

    @Benchmark
    public Object cryptoDecrypt(CryptoState state) {
        return state.decrypt.call(state.cx, state.scope, state.scope, emptyArgs);
    }

    @State(Scope.Thread)
    public static class DeltaBlueState extends AbstractState {
        Callable db;

        @Setup(Level.Trial)
        public void setUp() {
            initialize();
            evaluateSource(cx, scope, "testsrc/benchmarks/v8-benchmarks-v6/deltablue.js");
            runSetup();
            db = getRunFunc("DeltaBlue");
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            runCleanup();
            cleanup();
        }
    }

    @Benchmark
    public Object deltaBlue(DeltaBlueState state) {
        return state.db.call(state.cx, state.scope, state.scope, emptyArgs);
    }

    @State(Scope.Thread)
    public static class RayTraceState extends AbstractState {
        Callable rt;

        @Setup(Level.Trial)
        public void setUp() {
            initialize();
            evaluateSource(cx, scope, "testsrc/benchmarks/v8-benchmarks-v6/raytrace.js");
            runSetup();
            rt = getRunFunc("RayTrace");
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            runCleanup();
            cleanup();
        }
    }

    @Benchmark
    public Object rayTrace(RayTraceState state) {
        return state.rt.call(state.cx, state.scope, state.scope, emptyArgs);
    }

    /* TODO not working right now
    @State(Scope.Thread)
    public static class RegExpState extends AbstractState {
      Callable re;

      @Setup(Level.Trial)
      public void setUp() {
        initialize();
        evaluateSource(cx, scope, "testsrc/benchmarks/v8-benchmarks-v6/regexp.js");
        runSetup();
        re = getRunFunc("RegExp");
      }

      @TearDown(Level.Trial)
      public void tearDown() {
        runCleanup();
        cleanup();
      }
    }

    @Benchmark
    public Object regExp(RegExpState state) {
      return state.re.call(state.cx, state.scope, state.scope, emptyArgs);
    }
     */

    @State(Scope.Thread)
    public static class RichardsState extends AbstractState {
        Callable r;

        @Setup(Level.Trial)
        public void setUp() {
            initialize();
            evaluateSource(cx, scope, "testsrc/benchmarks/v8-benchmarks-v6/richards.js");
            runSetup();
            r = getRunFunc("Richards");
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            runCleanup();
            cleanup();
        }
    }

    @Benchmark
    public Object richards(RichardsState state) {
        return state.r.call(state.cx, state.scope, state.scope, emptyArgs);
    }

    @State(Scope.Thread)
    public static class EarleyBoyerState extends AbstractState {
        Callable earley;
        Callable boyer;

        @Setup(Level.Trial)
        public void setUp() {
            initialize();
            evaluateSource(cx, scope, "testsrc/benchmarks/v8-benchmarks-v6/earley-boyer.js");
            runSetup();
            earley = getRunFunc("Earley");
            boyer = getRunFunc("Boyer");
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            runCleanup();
            cleanup();
        }
    }

    @Benchmark
    public Object earley(EarleyBoyerState state) {
        return state.earley.call(state.cx, state.scope, state.scope, emptyArgs);
    }

    @Benchmark
    public Object boyer(EarleyBoyerState state) {
        return state.boyer.call(state.cx, state.scope, state.scope, emptyArgs);
    }
}
