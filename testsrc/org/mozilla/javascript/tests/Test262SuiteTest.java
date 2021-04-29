/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mozilla.javascript.drivers.TestUtils.JS_FILE_FILTER;
import static org.mozilla.javascript.drivers.TestUtils.recursiveListFilesHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.drivers.TestUtils;
import org.mozilla.javascript.tools.SourceReader;
import org.mozilla.javascript.tools.shell.ShellContextFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

@RunWith(Parameterized.class)
public class Test262SuiteTest {

    static final int[] OPT_LEVELS;

    private static final File testDir = new File("test262/test");
    private static final String testHarnessDir = "test262/harness/";
    private static final String testProperties = "testsrc/test262.properties";

    static Map<Integer, Map<String, Script>> HARNESS_SCRIPT_CACHE = new HashMap<>();
    static Map<File, Integer> EXCLUDED_TESTS = new LinkedHashMap<>();

    static ShellContextFactory CTX_FACTORY = new ShellContextFactory();

    static final Set<String> UNSUPPORTED_FEATURES =
            new HashSet<>(
                    Arrays.asList(
                            "Atomics",
                            "IsHTMLDDA",
                            "Promise.prototype.finally",
                            "Proxy",
                            "Reflect",
                            "Reflect.construct",
                            "Reflect.set",
                            "Reflect.setPrototypeOf",
                            "SharedArrayBuffer",
                            "async-functions",
                            "async-iteration",
                            "class",
                            "class-fields-private",
                            "class-fields-public",
                            "computed-property-names",
                            "cross-realm",
                            "default-arg",
                            "default-parameters",
                            "new.target",
                            "object-rest",
                            "regexp-dotall",
                            "regexp-lookbehind",
                            "regexp-named-groups",
                            "regexp-unicode-property-escapes",
                            "super",
                            "String.prototype.matchAll",
                            "Symbol.matchAll",
                            "tail-call-optimization",
                            "json-superset",
                            "u180e"));

    static {
        // Reduce the number of tests that we run by a factor of three...
        String overriddenLevel = getOverriddenLevel();
        if (overriddenLevel != null) {
            OPT_LEVELS = new int[] {Integer.parseInt(overriddenLevel)};
        } else {
            OPT_LEVELS = new int[] {-1, 0, 9};
        }
    }

    private static String getOverriddenLevel() {
        String optLevel = System.getenv("TEST_262_OPTLEVEL");
        if (optLevel == null) {
            optLevel = System.getProperty("TEST_OPTLEVEL");
        }
        return optLevel;
    }

    @BeforeClass
    public static void setUpClass() {
        for (int optLevel : OPT_LEVELS) {
            HARNESS_SCRIPT_CACHE.put(optLevel, new HashMap<>());
        }

        CTX_FACTORY.setLanguageVersion(Context.VERSION_ES6);
        TestUtils.setGlobalContextFactory(CTX_FACTORY);
    }

    @AfterClass
    public static void tearDownClass() {
        TestUtils.setGlobalContextFactory(null);

        for (Map.Entry<File, Integer> entry : EXCLUDED_TESTS.entrySet()) {
            if (entry.getValue() == 0) {
                System.out.println(
                        String.format(
                                "Test is marked as failing but it does not: %s", entry.getKey()));
            }
        }
    }

    private static final Pattern EXCLUDE_PATTERN = Pattern.compile("!\\s*(.+)");

    private final String testFilePath;
    private final int optLevel;
    private final boolean useStrict;
    private final Test262Case testCase;
    private final boolean fails;

    public Test262SuiteTest(
            String testFilePath,
            int optLevel,
            boolean useStrict,
            Test262Case testCase,
            boolean fails) {
        this.testFilePath = testFilePath;
        this.optLevel = optLevel;
        this.useStrict = useStrict;
        this.testCase = testCase;
        this.fails = fails;
    }

    private Scriptable buildScope(Context cx) throws IOException {
        Scriptable scope = cx.initSafeStandardObjects();
        for (String harnessFile : testCase.harnessFiles) {
            if (!HARNESS_SCRIPT_CACHE.get(optLevel).containsKey(harnessFile)) {
                String harnessPath = testHarnessDir + harnessFile;
                try (Reader reader = new FileReader(harnessPath)) {
                    HARNESS_SCRIPT_CACHE
                            .get(optLevel)
                            .put(harnessFile, cx.compileReader(reader, harnessPath, 1, null));
                }
            }
            HARNESS_SCRIPT_CACHE.get(optLevel).get(harnessFile).exec(cx, scope);
        }
        return scope;
    }

    private static String extractJSErrorName(RhinoException ex) {
        if (ex instanceof EvaluatorException) {
            // there's no universal format to EvaluatorException's
            // for now, just assume that it's a SyntaxError
            return "SyntaxError";
        }

        String exceptionName = ex.details();
        if (exceptionName.contains(":")) {
            exceptionName = exceptionName.substring(0, exceptionName.indexOf(":"));
        }
        return exceptionName;
    }

    @Test
    public void test262Case() {
        Context cx = Context.enter();
        cx.setOptimizationLevel(optLevel);
        cx.setGeneratingDebug(true);
        try {
            Scriptable scope;
            try {
                scope = buildScope(cx);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to build a scope with the harness files.", ex);
            }

            String str = testCase.source;
            int line = 1;
            if (useStrict) {
                str = "\"use strict\";\n" + str;
                line--;
            }

            boolean failedEarly = true;
            try {
                Script caseScript = cx.compileString(str, testFilePath, line, null);

                failedEarly = false; // not after this line
                caseScript.exec(cx, scope);

                if (testCase.isNegative()) {
                    fail(
                            String.format(
                                    "Failed a negative test. Expected error: %s (at phase '%s')",
                                    testCase.expectedError,
                                    testCase.hasEarlyError ? "early" : "runtime"));
                }

                if (fails) {
                    Integer count = EXCLUDED_TESTS.get(testCase.file);
                    if (count != null) {
                        count -= 1;
                        EXCLUDED_TESTS.put(testCase.file, count);
                    }
                }
            } catch (RhinoException ex) {
                if (fails) {
                    return;
                }

                if (!testCase.isNegative()) {
                    fail(String.format("%s%n%s", ex.getMessage(), ex.getScriptStackTrace()));
                }

                String errorName = extractJSErrorName(ex);

                if (testCase.hasEarlyError && !failedEarly) {
                    fail(
                            String.format(
                                    "Expected an early error: %s, got: %s in the runtime",
                                    testCase.expectedError, errorName));
                }

                assertEquals(ex.details(), testCase.expectedError, errorName);
            } catch (Exception ex) {
                if (fails) {
                    return;
                }
                throw ex;
            } catch (AssertionError ex) {
                if (fails) {
                    return;
                }
                throw ex;
            }
        } finally {
            Context.exit();
        }
    }

    private static void addTestFiles(List<File> testFiles, Set<File> failingFiles)
            throws IOException {
        List<File> dirFiles = new LinkedList<File>();

        try (Scanner scanner = new Scanner(new File(testProperties))) {
            int lineNo = 0;
            String line = null;
            while (line != null || scanner.hasNextLine()) {
                // Note, here line could be not null when it
                // wasn't handled on the previous iteration
                if (line == null) {
                    line = scanner.nextLine().trim();
                    lineNo++;
                }

                if (line.isEmpty() || line.startsWith("#")) {
                    line = null; // consume the line
                    continue;
                }

                File target = new File(testDir, line);
                if (!target.exists()) {
                    if (line.startsWith("!")) {
                        throw new RuntimeException(
                                "Unexpected exclusion '" + line + "' at the line #" + lineNo);
                    }
                    throw new FileNotFoundException(
                            "File "
                                    + target.getAbsolutePath()
                                    + " declared at line #"
                                    + lineNo
                                    + "('"
                                    + line
                                    + "') doesn't exist");
                }

                if (target.isFile()) {
                    testFiles.add(target);
                } else if (target.isDirectory()) {
                    String curDirectory = line;
                    recursiveListFilesHelper(target, JS_FILE_FILTER, dirFiles);

                    // start handling exclusions that could follow
                    while (scanner.hasNextLine()) {
                        line = scanner.nextLine().trim();
                        lineNo++;

                        if (line.isEmpty() || line.startsWith("#")) {
                            line = null; // consume the line
                            continue;
                        }

                        Matcher m = EXCLUDE_PATTERN.matcher(line);
                        if (!m.matches()) {
                            // stop an exclusion handling loop
                            break;
                        }

                        String excludeSubstr = m.group(1);
                        int excludeCount = 0;
                        for (File file : dirFiles) {
                            String path = file.getPath().replaceAll("\\\\", "/");
                            if (path.endsWith(excludeSubstr)) {
                                failingFiles.add(file);
                                excludeCount++;
                            }
                        }
                        if (excludeCount == 0) {
                            System.err.format(
                                    "WARN: Exclusion '%s' at line #%d doesn't exclude anything%n",
                                    excludeSubstr, lineNo);
                        }
                        // exclusion handled
                        line = null;
                    }

                    testFiles.addAll(dirFiles);
                    dirFiles.clear();

                    if (line != null && !line.equals(curDirectory)) {
                        // saw a different line and it isn't an exclusion,
                        // so it wasn't handled, let the main loop deal with it
                        continue;
                    }
                }

                // this line was handled
                line = null;
            }
        }
    }

    @Parameters(name = "js={0}, opt={1}, strict={2}")
    public static Collection<Object[]> test262SuiteValues() throws IOException {
        List<Object[]> result = new ArrayList<>();

        List<File> testFiles = new LinkedList<File>();
        Set<File> failingFiles = new HashSet<File>();
        addTestFiles(testFiles, failingFiles);

        fileLoop:
        for (File testFile : testFiles) {
            Test262Case testCase;
            try {
                testCase = Test262Case.fromSource(testFile);
            } catch (YAMLException ex) {
                throw new RuntimeException(
                        "Error while parsing metadata of " + testFile.getPath(), ex);
            }

            // all the reasons not to execute this file
            // even if it's not excluded in the config:
            // 1. it requires/tests unsupported features
            for (String feature : testCase.features) {
                if (UNSUPPORTED_FEATURES.contains(feature)) {
                    continue fileLoop;
                }
            }
            // 2. it runs in an unsupported environment
            if (testCase.hasFlag("module") || testCase.hasFlag("async")) {
                continue;
            }

            String caseShortPath = testDir.toPath().relativize(testFile.toPath()).toString();
            for (int optLevel : OPT_LEVELS) {
                boolean markedAsFailing = failingFiles.contains(testFile);
                if (!testCase.hasFlag("onlyStrict") || testCase.hasFlag("raw")) {
                    result.add(
                            new Object[] {
                                caseShortPath, optLevel, false, testCase, markedAsFailing
                            });
                    if (markedAsFailing) {
                        Integer count = EXCLUDED_TESTS.computeIfAbsent(testCase.file, k -> 0);
                        count += 1;
                        EXCLUDED_TESTS.put(testCase.file, count);
                    }
                }
                if (!testCase.hasFlag("noStrict") && !testCase.hasFlag("raw")) {
                    result.add(
                            new Object[] {
                                caseShortPath, optLevel, true, testCase, markedAsFailing
                            });
                    if (markedAsFailing) {
                        Integer count = EXCLUDED_TESTS.computeIfAbsent(testCase.file, k -> 0);
                        count += 1;
                        EXCLUDED_TESTS.put(testCase.file, count);
                    }
                }
            }
        }
        return result;
    }

    private static class Test262Case {
        private static final Yaml YAML = new Yaml();

        private final File file;
        private final String source;

        private final String expectedError;
        private final boolean hasEarlyError;

        private final Set<String> flags;
        private final Set<String> harnessFiles;
        private final Set<String> features;

        Test262Case(
                File file,
                String source,
                Set<String> harnessFiles,
                String expectedError,
                boolean hasEarlyError,
                Set<String> flags,
                Set<String> features) {

            this.file = file;
            this.source = source;
            this.harnessFiles = harnessFiles;
            this.expectedError = expectedError;
            this.hasEarlyError = hasEarlyError;
            this.flags = flags;
            this.features = features;
        }

        boolean hasFlag(String flag) {
            return flags.contains(flag);
        }

        boolean isNegative() {
            return expectedError != null;
        }

        @SuppressWarnings("unchecked")
        static Test262Case fromSource(File testFile) throws IOException {
            String testSource =
                    (String) SourceReader.readFileOrUrl(testFile.getPath(), true, "UTF-8");

            Set<String> harnessFiles = new HashSet<>();

            String metadataStr =
                    testSource.substring(
                            testSource.indexOf("/*---") + 5, testSource.indexOf("---*/"));
            Map<String, Object> metadata = (Map<String, Object>) YAML.load(metadataStr);

            if (metadata.containsKey("includes")) {
                harnessFiles.addAll((List<String>) metadata.get("includes"));
            }

            String expectedError = null;
            boolean isEarly = false;
            if (metadata.containsKey("negative")) {
                Map<String, String> negative = (Map<String, String>) metadata.get("negative");
                expectedError = negative.get("type");
                isEarly = "early".equals(negative.get("phase"));
            }

            Set<String> flags = new HashSet<>();
            if (metadata.containsKey("flags")) {
                flags.addAll((Collection<String>) metadata.get("flags"));
            }

            Set<String> features = new HashSet<>();
            if (metadata.containsKey("features")) {
                features.addAll((Collection<String>) metadata.get("features"));
            }

            if (!flags.contains("raw")) {
                // present by default harness files
                harnessFiles.add("assert.js");
                harnessFiles.add("sta.js");
            } else if (!harnessFiles.isEmpty()) {
                System.err.format(
                        "WARN: case '%s' is flagged as 'raw' but also has defined includes%n",
                        testFile.getPath());
            }

            return new Test262Case(
                    testFile, testSource, harnessFiles, expectedError, isEarly, flags, features);
        }
    }
}
