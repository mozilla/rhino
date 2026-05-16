/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Context.EvaluationMethod;
import org.mozilla.javascript.drivers.JsTestsBase;
import org.mozilla.javascript.drivers.ShellTest;
import org.mozilla.javascript.drivers.TestUtils;
import org.mozilla.javascript.testutils.Sharding;
import org.mozilla.javascript.testutils.TestSource;
import org.mozilla.javascript.tools.shell.ShellContextFactory;

/**
 * This JUnit suite runs the Mozilla test suite (in mozilla.org CVS at /mozilla/js/tests).
 *
 * <p>Not all tests in the suite are run. Since the mozilla.org tests are designed and maintained
 * for the SpiderMonkey engine, tests in the suite may not pass due to feature set differences and
 * known bugs. To make sure that this unit test is stable in the midst of changes to the mozilla.org
 * suite, we maintain a list of passing tests in files opt-1.tests, opt0.tests, and opt9.tests. This
 * class also implements the ability to run skipped tests, see if any pass, and print out a script
 * to modify the *.tests files. (This approach doesn't handle breaking changes to existing passing
 * tests, but in practice that has been very rare.)
 *
 * @author Norris Boyd
 * @author Attila Szegedi
 */
public class MozillaSuiteTest {
    private File jsFile;
    private EvaluationMethod evaluationMethod;

    public MozillaSuiteTest() {}

    public void initMozillaSuiteTest(File jsFile, EvaluationMethod method) {
        this.jsFile = jsFile;
        this.evaluationMethod = method;
        ShellTest.cacheFramework();
    }

    private static File getTestDir() throws IOException {
        File testDir = null;
        if (System.getProperty("mozilla.js.tests") != null) {
            testDir = new File(System.getProperty("mozilla.js.tests"));
        } else {
            testDir = new File(TestSource.resolveDirectory("testsrc/tests/test.sh"));
            if (!testDir.exists()) {
                URL url = JsTestsBase.class.getResource(".");
                String path = url.getFile();

                // support running from eclipse
                if (new File(path + "../../../../../testsrc/tests").exists()) {
                    testDir = new File(path + "../../../../../testsrc/tests").getCanonicalFile();
                } else {
                    int jsIndex = path.lastIndexOf("/js");
                    if (jsIndex == -1) {
                        throw new IllegalStateException(
                                "You aren't running the tests "
                                        + "from within the standard mozilla/js directory structure");
                    }
                    path = path.substring(0, jsIndex + 3).replace('/', File.separatorChar);
                    path = path.replace("%20", " ");
                    testDir = new File(path, "tests");
                }
            }
        }
        if (!testDir.isDirectory()) {
            throw new FileNotFoundException(testDir + " is not a directory");
        }
        return testDir;
    }

    private static String getTestFilename(EvaluationMethod method) {
        return method != EvaluationMethod.Compiler ? "interpreted.tests" : "compiled.tests";
    }

    private static List<File> getTestFiles(EvaluationMethod method) throws IOException {
        File testDir = getTestDir();
        String[] tests = TestUtils.loadTestsFromResource("/" + getTestFilename(method));
        if (tests.length == 0) {
            throw new IOException(
                    "No Mozilla Suite tests found in "
                            + testDir
                            + ". Check mozilla.js.tests property");
        }

        Arrays.sort(tests);

        var shards = Sharding.getSharding();
        ArrayList<File> files = new ArrayList<>();
        for (int i = 0; i < tests.length; i++) {
            if (shards == null || i % shards.total == shards.index) {
                files.add(new File(testDir, tests[i]));
            }
        }
        return files;
    }

    public static Collection<Object[]> mozillaSuiteValues() throws IOException {
        List<Object[]> result = new ArrayList<Object[]>();
        for (var em : Context.EvaluationMethod.values()) {
            var tests = getTestFiles(em);
            for (File f : tests) {
                result.add(new Object[] {f, em});
            }
        }
        return result;
    }

    private static class ShellTestParameters extends ShellTest.Parameters {
        @Override
        public int getTimeoutMilliseconds() {
            if (System.getProperty("mozilla.js.tests.timeout") != null) {
                return Integer.parseInt(System.getProperty("mozilla.js.tests.timeout"));
            }
            return 10000;
        }
    }

    private static class JunitStatus extends ShellTest.Status {
        File file;

        @Override
        public final void running(File jsFile) {
            // remember file in case we fail
            file = jsFile;
        }

        @Override
        public final void failed(String s) {
            // Include test source in message, this is the only way
            // to locate the test in a Parameterized JUnit test
            String msg = "In \"" + file + "\":" + System.getProperty("line.separator") + s;
            System.out.println(msg);
            Assertions.fail(msg);
        }

        @Override
        public final void exitCodesWere(int expected, int actual) {
            Assertions.assertEquals(expected, actual, "Unexpected exit code");
        }

        @Override
        public final void outputWas(String s) {
            // Do nothing; we don't want to see the output when running JUnit
            // tests.
        }

        @Override
        public final void threw(Throwable t) {
            Assertions.fail(ShellTest.getStackTrace(t));
        }

        @Override
        public final void timedOut(long timeoutMillis) {
            failed("Timed out (timeout = " + timeoutMillis + "(");
        }
    }

    @MethodSource("mozillaSuiteValues")
    @ParameterizedTest(name = "{index}, js={0}, evaluationMethod={1}")
    public void runMozillaTest(File jsFile, EvaluationMethod method) throws Exception {
        initMozillaSuiteTest(jsFile, method);
        // System.out.println("Test \"" + jsFile + "\" running under optimization level " +
        // optimizationLevel);
        final ShellContextFactory shellContextFactory = new ShellContextFactory();
        shellContextFactory.setEvaluationMethod(evaluationMethod);
        shellContextFactory.setLanguageVersion(Context.VERSION_1_8);
        ShellTestParameters params = new ShellTestParameters();
        JunitStatus status = new JunitStatus();
        ShellTest.runNoFork(shellContextFactory, jsFile, params, status);
    }

    /**
     * The main class will run all the test files that are *not* covered in the *.tests files, and
     * print out a list of all the tests that pass.
     */
    public static void main(String[] args) throws IOException {
        try (PrintStream out = new PrintStream("fix-tests-files.sh")) {
            try {
                for (var evalMethod : EvaluationMethod.values()) {
                    File testDir = getTestDir();
                    File[] allTests =
                            TestUtils.recursiveListFiles(
                                    testDir,
                                    new FileFilter() {
                                        @Override
                                        public boolean accept(File pathname) {
                                            return ShellTest.DIRECTORY_FILTER.accept(pathname)
                                                    || ShellTest.TEST_FILTER.accept(pathname);
                                        }
                                    });
                    HashSet<File> diff = new HashSet<File>(Arrays.asList(allTests));
                    var testFiles = getTestFiles(evalMethod);
                    diff.removeAll(testFiles);
                    ArrayList<String> skippedPassed = new ArrayList<String>();
                    int absolutePathLength = testDir.getAbsolutePath().length() + 1;
                    for (File testFile : diff) {
                        try {
                            new MozillaSuiteTest().runMozillaTest(testFile, evalMethod);
                            // strip off testDir
                            String canonicalized =
                                    testFile.getAbsolutePath().substring(absolutePathLength);
                            canonicalized = canonicalized.replace('\\', '/');
                            skippedPassed.add(canonicalized);
                        } catch (Throwable t) {
                            // failed, so skip
                        }
                    }
                    // "skippedPassed" now contains all the tests that are currently
                    // skipped but now pass. Print out shell commands to update the
                    // appropriate *.tests file.
                    if (skippedPassed.size() > 0) {
                        out.println("cat >> " + getTestFilename(evalMethod) + " <<EOF");
                        String[] sorted = skippedPassed.toArray(new String[0]);
                        Arrays.sort(sorted);
                        for (int j = 0; j < sorted.length; j++) {
                            out.println(sorted[j]);
                        }
                        out.println("EOF");
                    }
                }
                System.out.println("Done.");
            } finally {
                out.close();
            }
        }
    }
}
