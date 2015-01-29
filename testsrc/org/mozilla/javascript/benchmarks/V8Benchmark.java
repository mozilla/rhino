/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mozilla.javascript.benchmarks;

import org.junit.AfterClass;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.tools.shell.Global;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;

public class V8Benchmark
{
    public static final String TEST_DIR = "testsrc/benchmarks/v8-benchmarks-v6";
    public static final String TEST_SRC = TEST_DIR + "/run.js";
    public static final String PLOT_FILE = "build/v8.csv";

    private static final HashMap<Integer, String> results = new HashMap<Integer, String>();

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
            root.put("TEST_DIR", root, TEST_DIR);

            String result = (String)cx.evaluateReader(root, rdr, TEST_SRC, 1, null);
            results.put(optLevel, result);
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

    @AfterClass
    public static void plotResults()
        throws IOException
    {
        FileOutputStream plotOut = new FileOutputStream(PLOT_FILE);
        PrintWriter plotWriter = new PrintWriter(new OutputStreamWriter(plotOut));

        plotWriter.println("V8 Benchmark Opt 0,V8 Benchmark Opt 9");
        plotWriter.println(results.get(0) + ',' + results.get(9));
        plotWriter.close();
    }
}
