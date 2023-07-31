/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mozilla.javascript.drivers.TestUtils.JS_FILE_FILTER;
import static org.mozilla.javascript.drivers.TestUtils.recursiveListFilesHelper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.drivers.TestUtils;
import org.mozilla.javascript.tools.SourceReader;
import org.mozilla.javascript.tools.shell.ShellContextFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

@RunWith(Parameterized.class)
public class Test262SuiteTest {

    /**
     * The test source code must not be modified in any way, and the test must be executed just once
     * (in non-strict mode, only).
     */
    private static final String FLAG_RAW = "raw";

    /** The test must be executed just once--in strict mode, only. */
    private static final String FLAG_ONLY_STRICT = "onlyStrict";

    /** The test must be executed just once--in non-strict mode, only. */
    private static final String FLAG_NO_STRICT = "noStrict";

    static final int[] OPT_LEVELS;

    private static final File testDir = new File("test262/test");
    private static final String testHarnessDir = "test262/harness/";
    private static final String testProperties;

    private static final boolean updateTest262Properties;
    private static final boolean rollUpEnabled;
    private static final boolean statsEnabled;
    private static final boolean includeUnsupported;

    static Map<Integer, Map<String, Script>> HARNESS_SCRIPT_CACHE = new HashMap<>();
    static Map<Test262Case, TestResultTracker> RESULT_TRACKERS = new LinkedHashMap<>();

    static ShellContextFactory CTX_FACTORY = new ShellContextFactory();

    static final Set<String> UNSUPPORTED_FEATURES =
            new HashSet<>(
                    Arrays.asList(
                            "Atomics",
                            "IsHTMLDDA",
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
                            "u180e"));

    static {
        String propFile = System.getProperty("test262properties");
        testProperties =
                propFile != null && !propFile.equals("") ? propFile : "testsrc/test262.properties";

        String updateProps = System.getProperty("updateTest262properties");

        if (updateProps != null) {
            updateTest262Properties = true;

            switch (updateProps) {
                case "all":
                    rollUpEnabled = statsEnabled = includeUnsupported = true;
                    break;
                case "none":
                    rollUpEnabled = statsEnabled = includeUnsupported = false;
                    break;
                default:
                    rollUpEnabled = updateProps.isEmpty() || updateProps.indexOf("rollup") != -1;
                    statsEnabled = updateProps.isEmpty() || updateProps.indexOf("stats") != -1;
                    includeUnsupported =
                            updateProps.isEmpty() || updateProps.indexOf("unsupported") != -1;
            }

            if (getOverriddenLevel() != null) {
                System.out.println(
                        "Ignoring custom optLevels because the updateTest262Properties param is set");
            }

            OPT_LEVELS = Utils.DEFAULT_OPT_LEVELS;
        } else {
            updateTest262Properties = rollUpEnabled = statsEnabled = includeUnsupported = false;

            // Reduce the number of tests that we run by a factor of three...
            String overriddenLevel = getOverriddenLevel();
            if (overriddenLevel != null) {
                OPT_LEVELS = new int[] {Integer.parseInt(overriddenLevel)};
            } else {
                OPT_LEVELS = Utils.DEFAULT_OPT_LEVELS;
            }
        }
    }

    private static String getOverriddenLevel() {
        String optLevel = System.getProperty("TEST_OPTLEVEL");

        if (optLevel == null || optLevel.equals("")) {
            optLevel = System.getenv("TEST_262_OPTLEVEL");
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

        for (Entry<Test262Case, TestResultTracker> entry : RESULT_TRACKERS.entrySet()) {
            if (entry.getKey().file.isFile()) {
                TestResultTracker tt = entry.getValue();

                if (tt.expectedFailure && tt.expectationsMet()) {
                    System.out.println(
                            String.format(
                                    "Test is marked as failing but it does not: %s",
                                    entry.getKey().file));
                }
            }
        }

        if (updateTest262Properties) {
            // Regenerate .properties file
            try {
                Path previousReportingDir = null;
                Path currentReportingDir;
                List<String> failures = new ArrayList<String>();
                int testCount = 0;
                Path previousTestFileParentPath =
                        testDir.toPath(); // tracks the current directory for which files are
                // processed
                int rollUpCount = 0;
                int rolledUpFailureCount = 0;

                // Converting to an array, so a regular loop over an array can be used,
                // as there's the need to peek the next entry
                Test262Case[] testCases = new Test262Case[RESULT_TRACKERS.size()];
                RESULT_TRACKERS.keySet().toArray(testCases);

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(testProperties))) {
                    writer.write(
                            "# This is a configuration file for Test262SuiteTest.java. See ./README.md for more info about this file\n");

                    for (int j = 0; j < testCases.length; j++) {
                        File testFile = testCases[j].file;
                        TestResultTracker tt = RESULT_TRACKERS.get(testCases[j]);

                        boolean ontoNextReportingDir = false;
                        String testResult = null;

                        Path testFilePath = testFile.toPath();
                        // hardcoded for just language/expression and language/statements
                        // to be split out on a deeper level
                        int reportDepth =
                                testFilePath.getNameCount() > 3
                                                && testFilePath
                                                        .getName(2)
                                                        .toString()
                                                        .equals("language")
                                                && (testFilePath
                                                                .getName(3)
                                                                .toString()
                                                                .equals("expressions")
                                                        || testFilePath
                                                                .getName(3)
                                                                .toString()
                                                                .equals("statements"))
                                        ? 5
                                        : Math.min(4, testFilePath.getNameCount());
                        currentReportingDir = testFilePath.subpath(0, reportDepth);

                        if (previousReportingDir == null) {
                            previousReportingDir = currentReportingDir;
                        } else if (!currentReportingDir.startsWith(previousReportingDir)
                                || testFile.isDirectory()) {
                            ontoNextReportingDir = true;
                        }

                        // Determine if switching to another directory and if so whether all files
                        // in
                        // the
                        // previous directory failed
                        // If so, dont list all failing files, but list only the folder path
                        if (rollUpEnabled
                                && (!testFilePath.startsWith(previousTestFileParentPath)
                                        || !testFilePath
                                                .getParent()
                                                .equals(previousTestFileParentPath))) {
                            if (!previousReportingDir.equals(previousTestFileParentPath)
                                    && rollUpCount > 1) {
                                failures.add(
                                        "    "
                                                + currentReportingDir
                                                        .relativize(previousTestFileParentPath)
                                                        .toString()
                                                        .replace("\\", "/")
                                                + (statsEnabled
                                                        ? " "
                                                                + rollUpCount
                                                                + "/"
                                                                + rollUpCount
                                                                + " (100.0%)"
                                                        : ""));
                                rolledUpFailureCount += rollUpCount - 1;

                                for (; rollUpCount > 0; rollUpCount--) {
                                    failures.remove(failures.size() - 2);
                                }
                            }

                            previousTestFileParentPath = testFilePath.getParent();
                            rollUpCount = 0;
                        }

                        if (!testFile.isDirectory()) {
                            testResult = tt.getResult(OPT_LEVELS, testCases[j]);

                            if (testResult == null) {
                                // At least one passing test in currentParent directory, so prevent
                                // rollUp
                                rollUpCount = -1;
                            } else {
                                if (rollUpCount != -1) rollUpCount++;

                                testResult =
                                        "    "
                                                + currentReportingDir
                                                        .relativize(testFilePath)
                                                        .toString()
                                                        .replace("\\", "/")
                                                + (statsEnabled && testResult != ""
                                                        ? " " + testResult
                                                        : "");
                                if (tt.comment != null && !tt.comment.isEmpty()) {
                                    testResult += " " + tt.comment;
                                }
                            }

                            // Making sure the last folder gets properly logged
                            if (j == testCases.length - 1) {
                                if (testResult != null) {
                                    failures.add(testResult);
                                }
                                testCount++;
                                ontoNextReportingDir = true;
                            }
                        }

                        if (ontoNextReportingDir) {
                            int failureCount = rolledUpFailureCount + failures.size();
                            Double failurePercentage =
                                    testCount == 0 ? 0 : ((double) failureCount * 100 / testCount);

                            writer.write('\n');
                            writer.write(
                                    previousReportingDir
                                                    .subpath(2, previousReportingDir.getNameCount())
                                                    .toString()
                                                    .replace("\\", "/")
                                            + (statsEnabled
                                                    ? " "
                                                            + failureCount
                                                            + "/"
                                                            + testCount
                                                            + " ("
                                                            + new BigDecimal(
                                                                            failurePercentage
                                                                                    .toString())
                                                                    .setScale(
                                                                            2, RoundingMode.HALF_UP)
                                                                    .doubleValue()
                                                            + "%)"
                                                    : ""));
                            writer.write('\n');

                            if (failurePercentage != 0 && failurePercentage != 100) {
                                writer.write(
                                        failures.stream()
                                                .map(Object::toString)
                                                .collect(Collectors.joining("\n")));
                                writer.write('\n');
                            }

                            previousReportingDir = currentReportingDir;
                            failures.clear();
                            testCount = rolledUpFailureCount = 0;
                        }

                        if (testFile.isDirectory()) {
                            String message =
                                    "~"
                                            + currentReportingDir
                                                    .subpath(2, currentReportingDir.getNameCount())
                                                    .toString()
                                                    .replace("\\", "/");

                            if (tt.comment != null && !tt.comment.isEmpty()) {
                                message += " " + tt.comment;
                            }
                            writer.write('\n');
                            writer.write(message);
                            writer.write('\n');

                            // Consume testcases belonging to a skipped directory
                            while (testCases.length > j + 1
                                    && testCases[j + 1].file.isFile()
                                    && testCases[j + 1].file.getParentFile().equals(testFile)) {
                                TestResultTracker tt2 = RESULT_TRACKERS.get(testCases[j + 1]);

                                testResult =
                                        "    "
                                                + currentReportingDir
                                                        .relativize(testCases[j + 1].file.toPath())
                                                        .toString()
                                                        .replace("\\", "/");
                                if (tt2.comment != null && !tt2.comment.isEmpty()) {
                                    testResult += " " + tt2.comment;
                                }
                                writer.write(testResult);
                                writer.write('\n');
                                j++;
                            }

                            previousReportingDir = null;
                            continue;
                        }

                        if (testResult != null) {
                            failures.add(testResult);
                        }
                        testCount++;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * Regex breakdown:
     * Group 1:
     * 	topLevel folder exclusion marker - ~
     *  OR
     *  comment marker: optional whitespace + (! OR #) + optional whitespace - (?:\\s*)(?:!|#)(?:\\s*)
     *  OR
     *  preceding whitespace indicating subsolder/file: whitespace - \\s+
     * Group 2:
     * 	folder/filePath - (\\S+)
     * Group 3 (non-capturing):
     *  stats for directories (2/8 (25%)) OR failure info (strict OR non-strict OR non-interpreted OR {...}) for .js files >
     * Group 4
     *  optionally comment
     */
    private static final Pattern LINE_SPLITTER =
            Pattern.compile(
                    "(~|(?:\\s*)(?:!|#)(?:\\s*)|\\s+)?(\\S+)(?:[^\\S\\r\\n]+(?:strict|non-strict|non-interpreted|\\d+/\\d+ \\(\\d+(?:\\.\\d+)?%%\\)|\\{(?:non-strict|strict|unsupported): \\[.*\\],?\\}))?[^\\S\\r\\n]*(.*)");

    private final String testFilePath;
    private final int optLevel;
    private final boolean useStrict;
    private final Test262Case testCase;
    private final boolean markedAsFailing;

    /** @see https://github.com/tc39/test262/blob/main/INTERPRETING.md#host-defined-functions */
    public static class $262 {
        private ScriptableObject scope;

        static $262 install(ScriptableObject scope) {
            $262 instance = new $262(scope);

            scope.put("$262", scope, instance);
            scope.setAttributes("$262", ScriptableObject.DONTENUM);

            return instance;
        }

        $262(ScriptableObject scope) {
            this.scope = scope;
        }

        @JSFunction
        public void gc() {
            System.gc();
        }

        @JSFunction
        public Object evalScript(String source) {
            try (Context cx = Context.enter()) {
                return cx.evaluateString(this.scope, source, "<evalScript>", 1, null);
            }
        }

        @JSGetter
        public Object getGlobal() {
            return this.scope;
        }

        @JSFunction
        public $262 createRealm() {
            try (Context cx = Context.enter()) {
                ScriptableObject realm = cx.initSafeStandardObjects();

                return $262.install(realm);
            }
        }

        @JSFunction
        public void detachArrayBuffer() {
            throw new UnsupportedOperationException(
                    "$262.detachArrayBuffer() method not yet implemented");
        }

        @JSGetter
        public Object getAgent() {
            throw new UnsupportedOperationException("$262.agent property not yet implemented");
        }
    }

    public Test262SuiteTest(
            String testFilePath,
            int optLevel,
            boolean useStrict,
            Test262Case testCase,
            boolean markedAsFailing) {
        this.testFilePath = testFilePath;
        this.optLevel = optLevel;
        this.useStrict = useStrict;
        this.testCase = testCase;
        this.markedAsFailing = markedAsFailing;
    }

    private Scriptable buildScope(Context cx) throws IOException {
        ScriptableObject scope = cx.initSafeStandardObjects();

        for (String harnessFile : testCase.harnessFiles) {
            if (!HARNESS_SCRIPT_CACHE.get(optLevel).containsKey(harnessFile)) {
                String harnessPath = testHarnessDir + harnessFile;
                try (Reader reader = new FileReader(harnessPath)) {
                    String script = Kit.readReader(reader);

                    // fix for missing features in Rhino
                    if ("compareArray.js".equalsIgnoreCase(harnessFile)) {
                        script =
                                script.replace(
                                        "assert.compareArray = function(actual, expected, message = '')",
                                        "assert.compareArray = function(actual, expected, message)");
                    }

                    HARNESS_SCRIPT_CACHE
                            .get(optLevel)
                            .put(harnessFile, cx.compileString(script, harnessPath, 1, null));
                }
            }
            HARNESS_SCRIPT_CACHE.get(optLevel).get(harnessFile).exec(cx, scope);
        }

        $262.install(scope);

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
        try (Context cx = Context.enter()) {
            cx.setOptimizationLevel(optLevel);
            cx.setGeneratingDebug(true);

            boolean failedEarly = false;
            try {
                Scriptable scope;
                try {
                    scope = buildScope(cx);
                } catch (Exception ex) {
                    throw new RuntimeException(
                            "Failed to build a scope with the harness files.", ex);
                }

                String str = testCase.source;
                int line = 1;
                if (useStrict) {
                    str = "\"use strict\";\n" + str;
                    line--;
                }

                failedEarly = true;
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

                TestResultTracker tracker = RESULT_TRACKERS.get(testCase);
                if (tracker != null) {
                    tracker.passes(optLevel, useStrict);
                }
            } catch (RhinoException ex) {
                if (!testCase.isNegative()) {
                    if (markedAsFailing) return;

                    fail(String.format("%s%n%s", ex.getMessage(), ex.getScriptStackTrace()));
                }

                String errorName = extractJSErrorName(ex);

                if (testCase.hasEarlyError && !failedEarly) {
                    if (markedAsFailing) return;

                    fail(
                            String.format(
                                    "Expected an early error: %s, got: %s in the runtime",
                                    testCase.expectedError, errorName));
                }

                try {
                    assertEquals(ex.details(), testCase.expectedError, errorName);
                } catch (AssertionError aex) {
                    if (markedAsFailing) return;

                    throw aex;
                }

                TestResultTracker tracker = RESULT_TRACKERS.get(testCase);
                if (tracker != null) {
                    tracker.passes(optLevel, useStrict);
                }
            } catch (Exception ex) {
                // enable line below to print out stacktraces of unexpected exceptions
                // disabled for now because too many exceptions are throw
                // Unexpected non-Rhino-Exception here, so print the exception so it stands out
                // ex.printStackTrace();

                // Ignore the failed assertion if the test is marked as failing
                if (markedAsFailing) return;

                throw ex;
            } catch (AssertionError ex) {
                // Ignore the failed assertion if the test is marked as failing
                if (markedAsFailing) return;

                throw ex;
            }
        }
    }

    private static void addTestFiles(List<File> testFiles, Map<File, String> filesExpectedToFail)
            throws IOException {
        List<File> topLevelFolderContents = new LinkedList<File>();
        File topLevelFolder = null;
        boolean excludeTopLevelFolder = false;

        int lineNo = 0;
        String line;
        String path;
        String comment;

        try (Scanner scanner = new Scanner(new File(testProperties))) {
            while (scanner.hasNextLine()) {
                line = scanner.nextLine();
                Matcher splitLine = LINE_SPLITTER.matcher(line);
                lineNo++;

                if (!splitLine.matches()) {
                    if (line.length() > 0) {
                        System.err.format(
                                "WARN: Unexpected content '%s' at line #%d%n", line, lineNo);
                    }

                    continue;
                }

                path = splitLine.group(2);
                comment = splitLine.group(3);

                if (splitLine.group(1) == null || splitLine.group(1).equals("~")) {
                    // apparent topLevel folder match

                    topLevelFolder = new File(testDir, path);

                    if (!topLevelFolder.exists()) {
                        throw new RuntimeException(
                                "Non-existing '" + path + "' at the line #" + lineNo);
                    } else if (!topLevelFolder.isDirectory()) {
                        throw new RuntimeException(
                                "Unexpected file '"
                                        + path
                                        + "' at the top level at the line #"
                                        + lineNo);
                    }

                    excludeTopLevelFolder =
                            splitLine.group(1) != null && splitLine.group(1).equals("~");

                    topLevelFolderContents.clear();
                    recursiveListFilesHelper(
                            topLevelFolder, JS_FILE_FILTER, topLevelFolderContents);

                    if (updateTest262Properties) {
                        // Make sure files are always sorted the same way, alphabetically, with
                        // subdirectories first
                        // as to make sure that the output is stable when (re)generating the
                        // test262.properties file
                        topLevelFolderContents.sort(
                                (f1, f2) -> { // return -1: before, 0: equal, 1: after
                                    String p1 = f1.getParent();
                                    String p2 = f2.getParent();

                                    // making sure files come after subdirectories
                                    if (!p1.equals(p2)
                                            && (p1.startsWith(p2) || p2.startsWith(p1))) {
                                        return p1.startsWith(p2) ? -1 : 1;
                                    }

                                    return f1.toString()
                                            .replaceFirst("\\.js$", "")
                                            .compareToIgnoreCase(
                                                    f2.toString().replaceFirst("\\.js$", ""));
                                });
                    }

                    if (excludeTopLevelFolder) {
                        // Adding just the folder itself, needed when regenerating the .properties
                        // file
                        testFiles.add(topLevelFolder);
                        filesExpectedToFail.put(topLevelFolder, comment);
                    } else {
                        testFiles.addAll(topLevelFolderContents);
                    }

                    continue;
                } else if (splitLine.group(1).trim().length() > 0) {
                    // comments

                    continue;
                } else if (topLevelFolder == null) {
                    throw new RuntimeException(
                            "Gotten to file '"
                                    + splitLine.group(2)
                                    + "' at the line #"
                                    + lineNo
                                    + " without encountering a top level");
                }

                boolean fileFound = false;

                // Now onto the files and folders listed under the topLevel folder
                if (path.endsWith(".js")) {
                    for (File file : topLevelFolderContents) {
                        if (topLevelFolder
                                .toPath()
                                .relativize(file.toPath())
                                .toString()
                                .replaceAll("\\\\", "/")
                                .equals(path)) {
                            filesExpectedToFail.put(file, comment);

                            if (excludeTopLevelFolder) {
                                /* adding paths listed in the .properties file under the topLevel folder marked to skip
                                 * to testFiles, in order to be able to not loose then when regenerate the .properties file
                                 *
                                 * Want to keep track of these files as they apparently failed at the time when the directory was marked to be skipped
                                 */
                                testFiles.add(file);
                            }
                            fileFound = true;
                        }
                    }

                    if (!fileFound) {
                        System.err.format(
                                "WARN: Exclusion '%s' at line #%d doesn't exclude anything%n",
                                path, lineNo);
                    }
                } else {
                    File subFolder = new File(topLevelFolder, path);

                    if (!subFolder.exists()) {
                        System.err.format(
                                "WARN: Exclusion '%s' at line #%d doesn't exclude anything%n",
                                path, lineNo);
                    }

                    topLevelFolderContents.stream()
                            .forEach(
                                    file -> {
                                        if (file.toPath().getParent().equals(subFolder.toPath())) {
                                            filesExpectedToFail.put(file, null);
                                        }
                                    });
                }
            }
        }
    }

    @Parameters(name = "js={0}, opt={1}, strict={2}")
    public static Collection<Object[]> test262SuiteValues() throws IOException {
        List<Object[]> result = new ArrayList<>();
        File skipDir = null;

        List<File> testFiles = new LinkedList<File>();
        Map<File, String> failingFiles = new HashMap<File, String>();
        addTestFiles(testFiles, failingFiles);

        fileLoop:
        for (File testFile : testFiles) {
            String caseShortPath = testDir.toPath().relativize(testFile.toPath()).toString();
            boolean markedAsFailing = failingFiles.containsKey(testFile);
            String comment = markedAsFailing ? failingFiles.get(testFile) : null;

            // add dummy tracker, just in case the .properties file needs to be (re)generated
            if (testFile.isDirectory()) skipDir = testFile;

            if (skipDir != null) {
                if (!testFile.toPath().startsWith(skipDir.toPath())) {
                    skipDir = null;
                } else {
                    TestResultTracker tracker =
                            RESULT_TRACKERS.computeIfAbsent(
                                    new Test262Case(testFile, null, null, null, false, null, null),
                                    k -> new TestResultTracker(comment));
                    tracker.setExpectations(-2, true, false, false, true);
                    continue;
                }
            }

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
                    if (includeUnsupported) {
                        TestResultTracker tracker =
                                RESULT_TRACKERS.computeIfAbsent(
                                        testCase, k -> new TestResultTracker(comment));
                        tracker.setExpectations(
                                -2,
                                true,
                                testCase.hasFlag(FLAG_ONLY_STRICT),
                                testCase.hasFlag(FLAG_NO_STRICT),
                                true);
                    }

                    continue fileLoop;
                }
            }
            // 2. it runs in an unsupported environment
            if (testCase.hasFlag("module") || testCase.hasFlag("async")) {
                if (includeUnsupported) {
                    TestResultTracker tracker =
                            RESULT_TRACKERS.computeIfAbsent(
                                    testCase, k -> new TestResultTracker(comment));
                    tracker.setExpectations(
                            -2,
                            true,
                            testCase.hasFlag(FLAG_ONLY_STRICT),
                            testCase.hasFlag(FLAG_NO_STRICT),
                            true);
                }

                continue;
            }

            for (int optLevel : OPT_LEVELS) {
                if (!testCase.hasFlag(FLAG_ONLY_STRICT) || testCase.hasFlag(FLAG_RAW)) {
                    result.add(
                            new Object[] {
                                caseShortPath, optLevel, false, testCase, markedAsFailing
                            });
                    TestResultTracker tracker =
                            RESULT_TRACKERS.computeIfAbsent(
                                    testCase, k -> new TestResultTracker(comment));
                    tracker.setExpectations(
                            optLevel,
                            false,
                            testCase.hasFlag(FLAG_ONLY_STRICT),
                            testCase.hasFlag(FLAG_NO_STRICT),
                            markedAsFailing);
                }

                if (!testCase.hasFlag(FLAG_NO_STRICT) && !testCase.hasFlag(FLAG_RAW)) {
                    result.add(
                            new Object[] {
                                caseShortPath, optLevel, true, testCase, markedAsFailing
                            });
                    TestResultTracker tracker =
                            RESULT_TRACKERS.computeIfAbsent(
                                    testCase, k -> new TestResultTracker(comment));
                    tracker.setExpectations(
                            optLevel,
                            true,
                            testCase.hasFlag(FLAG_ONLY_STRICT),
                            testCase.hasFlag(FLAG_NO_STRICT),
                            markedAsFailing);
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
        private final List<String> harnessFiles;
        private final Set<String> features;

        Test262Case(
                File file,
                String source,
                List<String> harnessFiles,
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

            List<String> harnessFiles = new ArrayList<>();

            Map<String, Object> metadata;

            if (testSource.indexOf("/*---") != -1) {
                String metadataStr =
                        testSource.substring(
                                testSource.indexOf("/*---") + 5, testSource.indexOf("---*/"));
                metadata = (Map<String, Object>) YAML.load(metadataStr);
            } else {
                System.err.format(
                        "WARN: file '%s' doesnt contain /*--- ... ---*/ directive",
                        testFile.getPath());
                metadata = new HashMap<String, Object>();
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

            if (flags.contains(FLAG_RAW) && metadata.containsKey("includes")) {
                System.err.format(
                        "WARN: case '%s' is flagged as 'raw' but also has defined includes%n",
                        testFile.getPath());
            } else {
                // present by default harness files
                harnessFiles.add("assert.js");
                harnessFiles.add("sta.js");

                if (metadata.containsKey("includes")) {
                    harnessFiles.addAll((List<String>) metadata.get("includes"));
                }
            }

            return new Test262Case(
                    testFile, testSource, harnessFiles, expectedError, isEarly, flags, features);
        }
    }

    private static class TestResultTracker {
        private Set<Integer> strictOptLevel = new HashSet<>();
        private Set<Integer> nonStrictOptLevel = new HashSet<>();
        private boolean onlyStrict;
        private boolean noStrict;
        private boolean expectedFailure;
        private String comment;

        TestResultTracker(String comment) {
            this.comment = comment;
        }

        public void setExpectations(
                int optLevel,
                boolean useStrict,
                boolean onlyStrict,
                boolean noStrict,
                boolean expectedFailure) {
            if (useStrict) {
                strictOptLevel.add(optLevel);
            } else {
                nonStrictOptLevel.add(optLevel);
            }
            this.onlyStrict = onlyStrict;
            this.noStrict = noStrict;
            this.expectedFailure = expectedFailure;
        }

        public boolean expectationsMet() {
            return strictOptLevel.size() + nonStrictOptLevel.size() == 0;
        }

        public void passes(int optLevel, boolean useStrict) {
            if (useStrict) {
                strictOptLevel.remove(optLevel);
            } else {
                nonStrictOptLevel.remove(optLevel);
            }
        }

        public String getResult(int[] optLevels, Test262Case tc) {
            // success on all optLevels in both strict and non-strict mode
            if (strictOptLevel.size() + nonStrictOptLevel.size() == 0) {
                return null;
            }

            // Test skipped due to dependencies on unsupported features/environment
            if (strictOptLevel.contains(-2)) {
                List<String> feats = new ArrayList<String>();

                for (String feature : tc.features) {
                    if (UNSUPPORTED_FEATURES.contains(feature)) {
                        feats.add(feature);
                    }
                }

                if (tc.hasFlag("module")) {
                    feats.add("module");
                }

                if (tc.hasFlag("async")) {
                    feats.add("async");
                }

                return "{unsupported: " + Arrays.toString(feats.toArray()) + "}";
            }

            // failure on all optLevels in both strict and non-strict mode
            if (strictOptLevel.size() == optLevels.length
                    && nonStrictOptLevel.size() == optLevels.length) {
                return "";
            }

            // all optLevels fail only in strict
            if (strictOptLevel.size() == optLevels.length && nonStrictOptLevel.size() == 0) {
                return "strict";
            }

            // all optLevels fail only in non-strict
            if (nonStrictOptLevel.size() == optLevels.length && strictOptLevel.size() == 0) {
                return "non-strict";
            }

            // success in interpreted optLevel, but failure in all other optLevels
            if ((noStrict
                            || (strictOptLevel.size() == optLevels.length - 1
                                    && !strictOptLevel.contains(-1)))
                    && (onlyStrict
                            || (nonStrictOptLevel.size() == optLevels.length - 1
                                    && !nonStrictOptLevel.contains(-1)))) {
                return "non-interpreted";
            }

            // mix of mode and optLevel successes and failures
            String result = "{";
            if (!noStrict && strictOptLevel.size() > 0) {
                result += "strict: " + Arrays.toString(strictOptLevel.toArray());
            }

            if (!onlyStrict && nonStrictOptLevel.size() > 0) {
                result += !noStrict && strictOptLevel.size() > 0 ? ", " : "";
                result += "non-strict: " + Arrays.toString(nonStrictOptLevel.toArray());
            }

            return result + "}";
        }
    }
}
