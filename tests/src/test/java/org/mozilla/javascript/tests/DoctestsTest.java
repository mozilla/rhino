/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.drivers.TestUtils;
import org.mozilla.javascript.tools.shell.Global;

/**
 * Run doctests in folder testsrc/doctests.
 *
 * <p>A doctest is a test in the form of an interactive shell session; Rhino collects and runs the
 * inputs to the shell prompt and compares them to the expected outputs.
 *
 * @author Norris Boyd
 */
@RunWith(Parameterized.class)
public class DoctestsTest {
    static final String baseDirectory = "testsrc" + File.separator + "doctests";
    static final String doctestsExtension = ".doctest";
    String name;
    String source;
    int optimizationLevel;

    public DoctestsTest(String name, String source, int optimizationLevel) {
        this.name = name;
        this.source = source;
        this.optimizationLevel = optimizationLevel;
    }

    public static File[] getDoctestFiles() {
        return TestUtils.recursiveListFiles(
                new File(baseDirectory),
                new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        String name = f.getName();
                        return !name.contains("feature18enabled")
                                && name.endsWith(doctestsExtension);
                    }
                });
    }

    public static String loadFile(File f) throws IOException {
        int length = (int) f.length(); // don't worry about very long files
        char[] buf = new char[length];
        new FileReader(f).read(buf, 0, length);
        return new String(buf);
    }

    @Parameters(name = "{0} opt:{2}")
    public static Collection<Object[]> doctestValues() throws IOException {
        File[] doctests = getDoctestFiles();
        List<Object[]> result = new ArrayList<Object[]>();
        for (File f : doctests) {
            String contents = loadFile(f);
            result.add(new Object[] {f.getName(), contents, -1});
            result.add(new Object[] {f.getName(), contents, 0});
            result.add(new Object[] {f.getName(), contents, 9});
        }
        return result;
    }

    // move "@Parameters" to this method to test a single doctest
    public static Collection<Object[]> singleDoctest() throws IOException {
        List<Object[]> result = new ArrayList<Object[]>();
        File f = new File(baseDirectory, "Counter.doctest");
        String contents = loadFile(f);
        result.add(new Object[] {f.getName(), contents, -1});
        return result;
    }

    @Test
    public void runDoctest() throws Exception {
        ContextFactory factory = ContextFactory.getGlobal();
        try (Context cx = factory.enterContext()) {
            cx.setOptimizationLevel(optimizationLevel);
            Global global = new Global(cx);
            // global.runDoctest throws an exception on any failure
            int testsPassed = global.runDoctest(cx, global, source, name, 1);
            assertTrue(testsPassed > 0);
        } catch (Exception ex) {
            System.out.println(name + "(" + optimizationLevel + "): FAILED due to " + ex);
            throw ex;
        }
    }
}
