package org.mozilla.javascript.benchmarks;

import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Context.EvaluationMethod;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.TopLevel;
import org.openjdk.jmh.annotations.*;

@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class ForInBenchmark {
    @State(Scope.Thread)
    public static class ForInState {
        Context cx;
        TopLevel scope;

        Function plainObject;
        Function instance;
        Function chain;
        Function array;

        @Param({"Interpreter", "Compiler"})
        public EvaluationMethod evalMethod;

        @Setup(Level.Trial)
        public void setup() throws IOException {
            cx = Context.enter();
            cx.setEvaluationMethod(evalMethod);
            cx.setLanguageVersion(Context.VERSION_ES6);
            scope = cx.initStandardObjects();

            try (FileReader rdr = new FileReader("testsrc/benchmarks/micro/for-in-benchmarks.js")) {
                cx.evaluateReader(scope, rdr, "for-in-benchmarks.js", 1, null);
            }
            plainObject = (Function) ScriptableObject.getProperty(scope, "forInPlainObject");
            instance = (Function) ScriptableObject.getProperty(scope, "forInInstance");
            chain = (Function) ScriptableObject.getProperty(scope, "forInChain");
            array = (Function) ScriptableObject.getProperty(scope, "forInArray");
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            cx.close();
        }

        Object call(Function f) {
            return f.call(cx, scope, null, new Object[0]);
        }
    }

    @Benchmark
    public Object plainObject(ForInState state) {
        return state.call(state.plainObject);
    }

    @Benchmark
    public Object instanceWithPrototypeMethods(ForInState state) {
        return state.call(state.instance);
    }

    @Benchmark
    public Object prototypeChain(ForInState state) {
        return state.call(state.chain);
    }

    @Benchmark
    public Object array(ForInState state) {
        return state.call(state.array);
    }
}
