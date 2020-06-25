package org.mozilla.javascript.benchmarks;

import java.io.FileReader;
import java.io.IOException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

public class PropertyBenchmark {
    private static final String TEST_BASE = "testsrc/benchmarks/micro/";

    static abstract class AbstractState {
        Context cx;
        Scriptable scope;
        Script script;
        String fileName;

        AbstractState(String fileName) {
            this.fileName = TEST_BASE + fileName;
        }

        @Setup(Level.Trial)
        public void setUp() {
            cx = Context.enter();
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setOptimizationLevel(9);
            scope = cx.initStandardObjects();

            try (FileReader rdr = new FileReader(fileName)) {
                script = cx.compileReader(rdr, fileName, 1, null);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            Context.exit();
        }

        Object run() {
            return script.exec(cx, scope);
        }
    }

    @State(Scope.Thread)
    public static class PropState extends AbstractState {
        public PropState() {
            super("propaccess.js");
        }
    }

    @Benchmark
    public Object propAccess(PropState state) {
        return state.run();
    }
}
