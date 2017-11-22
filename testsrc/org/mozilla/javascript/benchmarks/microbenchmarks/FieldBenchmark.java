package org.mozilla.javascript.benchmarks.microbenchmarks;

import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import com.carrotsearch.junitbenchmarks.BenchmarkRule;
import com.carrotsearch.junitbenchmarks.annotation.BenchmarkMethodChart;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.shell.Global;

import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

@BenchmarkOptions(concurrency=1, warmupRounds=250, benchmarkRounds=500)
@BenchmarkMethodChart(filePrefix="fieldbenchmark")
@RunWith(Parameterized.class)
public class FieldBenchmark
{
    public static final int ITERATIONS = 100000;

    static final Random rand = new Random();

    private int stringKeys;
    private int intKeys;
    private int optLevel;

    private Scriptable scope;

    private Scriptable strings;
    private Scriptable ints;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {10, 0, 9}, {100, 0, 9}, {0, 10, 9}, {0, 100, 9}
        });
    }

    public FieldBenchmark(int strings, int ints, int opt)
    {
        this.stringKeys = strings;
        this.intKeys = ints;
        this.optLevel = opt;
    }

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

    @Rule
    public TestRule benchmarkRun = new BenchmarkRule();

    @Before
    public void create()
            throws IOException {
        Context cx = Context.enter();
        try {
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
        } finally {
            Context.exit();
        }
    }

    @Test
    public void createFields() {
        Context cx = Context.enter();
        try {
            Function create = (Function) ScriptableObject.getProperty(scope, "createObject");
            create.call(cx, scope, null, new Object[]{ITERATIONS, strings, ints});
        } finally {
            Context.exit();
        }
    }

    @Test
    public void accessFields() {
        Context cx = Context.enter();
        try {
            Function create = (Function) ScriptableObject.getProperty(scope, "createObject");
            Object o = create.call(cx, scope, null, new Object[]{1, strings, ints});
            Function access = (Function) ScriptableObject.getProperty(scope, "accessObject");
            access.call(cx, scope, null, new Object[]{ITERATIONS, o, strings, ints});
        } finally {
            Context.exit();
        }
    }

    @Test
    public void iterateFields() {
        Context cx = Context.enter();
        try {
            Function create = (Function) ScriptableObject.getProperty(scope, "createObject");
            Object o = create.call(cx, scope, null, new Object[]{1, strings, ints});
            Function iterate = (Function) ScriptableObject.getProperty(scope, "iterateObject");
            iterate.call(cx, scope, null, new Object[]{ITERATIONS, o});
        } finally {
            Context.exit();
        }
    }

    @Test
    public void ownKeysFields() {
        Context cx = Context.enter();
        try {
            Function create = (Function) ScriptableObject.getProperty(scope, "createObject");
            Object o = create.call(cx, scope, null, new Object[]{1, strings, ints});
            Function iterate = (Function) ScriptableObject.getProperty(scope, "iterateOwnKeysObject");
            iterate.call(cx, scope, null, new Object[]{ITERATIONS, o});
        } finally {
            Context.exit();
        }
    }

    @Test
    public void deleteFields() {
        Context cx = Context.enter();
        try {
            Function create = (Function) ScriptableObject.getProperty(scope, "createObject");
            Object o = create.call(cx, scope, null, new Object[]{1, strings, ints});
            Function delete = (Function) ScriptableObject.getProperty(scope, "deleteObject");
            delete.call(cx, scope, null, new Object[]{ITERATIONS, o, strings, ints});
        } finally {
            Context.exit();
        }
    }
}
