package org.mozilla.javascript.benchmarks;

import com.google.caliper.AfterExperiment;
import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.shell.Global;

import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

@SuppressWarnings("unused")
public class CaliperObjectBenchmark
{
    static final Random rand = new Random();

    static void runCode(Context cx, Scriptable scope, String fileName)
        throws IOException
    {
        FileReader rdr = new FileReader(fileName);
        try {
            cx.evaluateReader(scope, rdr, "test.js", 1, null);
        } finally {
            rdr.close();
        }
    }

    @SuppressWarnings("unused")
    public static class FieldAccess
    {
        @Param("10") int stringKeys;
        @Param("10") int intKeys;
        @Param("9") int optLevel;

        private Context cx;
        private Scriptable scope;

        private Scriptable strings;
        private Scriptable ints;

        @BeforeExperiment
        @SuppressWarnings("unused")
        void create()
            throws IOException
        {
            cx = Context.enter();
            cx.setOptimizationLevel(optLevel);
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

        @AfterExperiment
        @SuppressWarnings("unused")
        void close()
        {
            Context.exit();
        }

        @Benchmark
        @SuppressWarnings("unused")
        void createFields(int count)
        {
            Function create = (Function)ScriptableObject.getProperty(scope, "createObject");
            create.call(cx, scope, null, new Object[]{count, strings, ints});
        }

        @Benchmark
        @SuppressWarnings("unused")
        void accessFields(int count)
        {
            Function create = (Function)ScriptableObject.getProperty(scope, "createObject");
            Object o = create.call(cx, scope, null, new Object[]{1, strings, ints});
            Function access = (Function)ScriptableObject.getProperty(scope, "accessObject");
            access.call(cx, scope, null, new Object[] {count, o, strings, ints});
        }

        @Benchmark
        @SuppressWarnings("unused")
        void iterateFields(long count)
        {
            Function create = (Function)ScriptableObject.getProperty(scope, "createObject");
            Object o = create.call(cx, scope, null, new Object[]{1, strings, ints});
            Function iterate = (Function)ScriptableObject.getProperty(scope, "iterateObject");
            iterate.call(cx, scope, null, new Object[] {count, o});
        }

        @Benchmark
        @SuppressWarnings("unused")
        void ownKeysFields(long count)
        {
            Function create = (Function)ScriptableObject.getProperty(scope, "createObject");
            Object o = create.call(cx, scope, null, new Object[]{1, strings, ints});
            Function iterate = (Function)ScriptableObject.getProperty(scope, "iterateOwnKeysObject");
            iterate.call(cx, scope, null, new Object[] {count, o});
        }

        @Benchmark
        @SuppressWarnings("unused")
        void deleteFields(int count)
        {
            Function create = (Function)ScriptableObject.getProperty(scope, "createObject");
            Object o = create.call(cx, scope, null, new Object[]{1, strings, ints});
            Function delete = (Function)ScriptableObject.getProperty(scope, "deleteObject");
            delete.call(cx, scope, null, new Object[] {count, o, strings, ints});
        }
    }
}
