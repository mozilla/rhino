package org.mozilla.javascript.benchmarks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;

import org.junit.AfterClass;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.tools.shell.Global;

public class SunSpiderBenchmark
{
    public static final String TEST_SRC = "sunspider-0.9.1/run.js";

    private static final HashMap<Integer, String> results = new HashMap<Integer, String>();

    @AfterClass
    public static void writeResults()
        throws IOException
    {
        PrintWriter out = new PrintWriter(
            new FileWriter(new File(System.getProperty("rhino.benchmark.report"), "sunspider.csv"))
        );
        // Hard code the opt levels for now -- we will make it more generic when we need to
        out.println("Optimization 0,Optimization 9");
        out.println(results.get(0) + ',' + results.get(9));
        out.close();
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
            root.put("RUN_NAME", root, "SunSpider-" + optLevel);
            Object result = cx.evaluateReader(root, rdr, TEST_SRC, 1, null);
            results.put(optLevel, result.toString());
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
