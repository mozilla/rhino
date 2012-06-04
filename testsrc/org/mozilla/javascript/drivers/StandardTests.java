/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.drivers;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Arrays;

import junit.framework.Assert;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.mozilla.javascript.tools.shell.ShellContextFactory;

/**
 * Executes the tests in the js/tests directory, much like jsDriver.pl does.
 * Excludes tests found in the js/tests/rhino-n.tests file.
 * @author Attila Szegedi
 * @version $Id: StandardTests.java,v 1.15 2009/07/21 17:39:05 nboyd%atg.com Exp $
 */
public class StandardTests extends TestSuite
{
    // Disable this suite in favor of
    // org.mozilla.javascript.tests.MozillaSuiteTest
    private static final boolean DISABLE = true;

	public static TestSuite suite() throws Exception
    {
        TestSuite suite = new TestSuite("Standard JavaScript tests");
        if (!DISABLE) {
            File testDir = null;
            if (System.getProperty("mozilla.js.tests") != null) {
                testDir = new File(System.getProperty("mozilla.js.tests"));
            } else {
                URL url = StandardTests.class.getResource(".");
                String path = url.getFile();
                int jsIndex = path.lastIndexOf("/js");
                if(jsIndex == -1)
                {
                    throw new IllegalStateException("You aren't running the tests from within the standard mozilla/js directory structure");
                }
                path = path.substring(0, jsIndex + 3).replace('/', File.separatorChar);
                path = path.replace("%20", " ");
                testDir = new File(path, "tests");
            }
            if(!testDir.isDirectory())
            {
                throw new FileNotFoundException(testDir + " is not a directory");
            }
            String[] excludes = TestUtils.loadTestsFromResource("/base.skip", null);
            String[] opt1Excludes = TestUtils.loadTestsFromResource("/opt1.skip", excludes);
            for(int i = -1; i < 2; ++i)
            {
                TestSuite optimizationLevelSuite = new TestSuite("Optimization level " + i);
                addSuites(optimizationLevelSuite, testDir, i == -1 ? excludes : opt1Excludes, i);
                suite.addTest(optimizationLevelSuite);
            }
        }
        return suite;
    }

    private static void addSuites(TestSuite topLevel, File testDir, String[] excludes, int optimizationLevel)
    {
        File[] subdirs = testDir.listFiles(ShellTest.DIRECTORY_FILTER);
        Arrays.sort(subdirs);
        for (int i = 0; i < subdirs.length; i++)
        {
            File subdir = subdirs[i];
            String name = subdir.getName();
            if (TestUtils.matches(excludes, name)) {
                continue;
            }
            TestSuite testSuite = new TestSuite(name);
            addCategories(testSuite, subdir, name + "/", excludes, optimizationLevel);
            topLevel.addTest(testSuite);
        }
    }

    private static void addCategories(TestSuite suite, File suiteDir, String prefix, String[] excludes, int optimizationLevel)
    {
        File[] subdirs = suiteDir.listFiles(ShellTest.DIRECTORY_FILTER);
        Arrays.sort(subdirs);
        for (int i = 0; i < subdirs.length; i++)
        {
            File subdir = subdirs[i];
            String name = subdir.getName();
            TestSuite testCategory = new TestSuite(name);
            addTests(testCategory, subdir, prefix + name + "/", excludes, optimizationLevel);
            suite.addTest(testCategory);
        }
    }

    private static void addTests(TestSuite suite, File suiteDir, String prefix, String[] excludes, int optimizationLevel)
    {
        File[] jsFiles = suiteDir.listFiles(ShellTest.TEST_FILTER);
        Arrays.sort(jsFiles);
        for (int i = 0; i < jsFiles.length; i++)
        {
            File jsFile = jsFiles[i];
            String name = jsFile.getName();
            if (!TestUtils.matches(excludes, prefix + name)) {
                suite.addTest(new JsTestCase(jsFile, optimizationLevel));
            }
        }
    }

    public static class JunitStatus extends ShellTest.Status {
        @Override
        public final void running(File jsFile) {
            //    do nothing
        }

        @Override
        public final void failed(String s) {
            Assert.fail(s);
        }

        @Override
        public final void exitCodesWere(int expected, int actual) {
            Assert.assertEquals("Unexpected exit code", expected, actual);
        }

        @Override
        public final void outputWas(String s) {
            // Do nothing; we don't want to see the output when running JUnit
            // tests.
        }

        @Override
        public final void threw(Throwable t) {
            Assert.fail(ShellTest.getStackTrace(t));
        }

        @Override
        public final void timedOut() {
            failed("Timed out.");
        }
    }

    public static final class JsTestCase extends TestCase
    {
        private final File jsFile;
        private final int optimizationLevel;

        JsTestCase(File jsFile, int optimizationLevel)
        {
            super(jsFile.getName() + (optimizationLevel == 1 ? "-compiled" : "-interpreted"));
            this.jsFile = jsFile;
            this.optimizationLevel = optimizationLevel;
        }

        @Override
        public int countTestCases()
        {
            return 1;
        }

        public static class ShellTestParameters extends ShellTest.Parameters {
            @Override
            public int getTimeoutMilliseconds() {
                if (System.getProperty("mozilla.js.tests.timeout") != null) {
                    return Integer.parseInt(System.getProperty("mozilla.js.tests.timeout"));
                }
                return 60000;
            }
        }

        @Override
        public void runBare() throws Exception
        {
            final ShellContextFactory shellContextFactory = new ShellContextFactory();
            shellContextFactory.setOptimizationLevel(optimizationLevel);
            ShellTestParameters params = new ShellTestParameters();
            ShellTest.run(shellContextFactory, jsFile, params, new JunitStatus());
        }
    }
}
