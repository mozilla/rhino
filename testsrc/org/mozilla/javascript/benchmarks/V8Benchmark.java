package org.mozilla.javascript.benchmarks;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.tools.shell.Global;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class V8Benchmark
{
    public static final String TEST_SRC = "run.js";

    private static PrintWriter output;

    @BeforeClass
    public static void openResults()
        throws IOException
    {
        output = new PrintWriter(new FileWriter("../../../build/v8benchmark.csv"));
    }

    @AfterClass
    public static void closeResults()
        throws IOException
    {
        output.close();
    }

    private void runTest(int optLevel)
        throws IOException
    {
        FileInputStream srcFile = new FileInputStream(TEST_SRC);
        InputStreamReader rdr = new InputStreamReader(srcFile, "utf8");

        try {
            Context cx = Context.enter();
            cx.setLanguageVersion(Context.VERSION_1_8);
            cx.setOptimizationLevel(optLevel);
            Global root = new Global(cx);
            root.put("RUN_NAME", root, "V8-Benchmark-" + optLevel);
            Object result = cx.evaluateReader(root, rdr, TEST_SRC, 1, null);
            output.println("V8 Benchmark Opt " + optLevel + ',' + result.toString());
        } finally {
            rdr.close();
            srcFile.close();
        }
    }

    @Test
    public void testOptLevel9()
        throws IOException
    {
        runTest(9);
    }

    @Test
    public void testOptLevel0()
        throws IOException
    {
        runTest(0);
    }
}
