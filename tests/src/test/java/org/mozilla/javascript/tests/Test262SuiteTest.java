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
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.SymbolKey;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.drivers.TestUtils;
import org.mozilla.javascript.tools.SourceReader;
import org.mozilla.javascript.tools.shell.ShellContextFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

@Execution(ExecutionMode.CONCURRENT)
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

    private static final File testDir = new File("test262/test");
    private static final String testHarnessDir = "test262/harness/";
    private static final String testProperties;

    private static final boolean updateTest262Properties;
    private static final boolean rollUpEnabled;
    private static final boolean statsEnabled;
    private static final boolean includeUnsupported;

    static Map<String, Script> HARNESS_SCRIPT_CACHE = new ConcurrentHashMap<>();
    static Map<Test262Case, TestResultTracker> RESULT_TRACKERS = new LinkedHashMap<>();

    static ShellContextFactory CTX_FACTORY = new ShellContextFactory();

    static final Set<String> UNSUPPORTED_FEATURES =
            new HashSet<>(
                    Arrays.asList(
                            "Atomics",
                            "IsHTMLDDA",
                            "SharedArrayBuffer",
                            "async-functions",
                            "async-iteration",
                            "class",
                            "class-fields-private",
                            "class-fields-public",
                            "default-arg",
                            "new.target",
                            "object-rest",
                            "regexp-dotall",
                            "regexp-lookbehind",
                            "regexp-named-groups",
                            "regexp-unicode-property-escapes",
                            "resizable-arraybuffer",
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
        } else {
            updateTest262Properties = rollUpEnabled = statsEnabled = includeUnsupported = false;
        }
    }

    @BeforeAll
    public static void setUpClass() {
        CTX_FACTORY.setLanguageVersion(Context.VERSION_ES6);
        TestUtils.setGlobalContextFactory(CTX_FACTORY);
    }

    @AfterAll
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
                List<String> failures = new ArrayList<>();
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
                                        : Math.min(
                                                4,
                                                testFilePath.getNameCount()
                                                        - (testFile.isDirectory() ? 0 : 1));
                        currentReportingDir = testFilePath.subpath(0, reportDepth);

                        if (previousReportingDir == null) {
                            previousReportingDir = currentReportingDir;
                        } else if (!currentReportingDir.startsWith(previousReportingDir)
                                || testFile.isDirectory()) {
                            ontoNextReportingDir = true;
                        }

                        // Determine if switching to another directory and if so whether all files
                        // in the previous directory failed
                        // If so, don't list all failing files, but list only the folder path
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
                            testResult = tt.getResult(testCases[j]);

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
                    "(~|(?:\\s*)(?:!|#)(?:\\s*)|\\s+)?(\\S+)(?:[^\\S\\r\\n]+"
                            + "(?:strict|non-strict|compiled-strict|compiled-non-strict|interpreted-strict|interpreted-non-strict|compiled|interpreted|"
                            + "\\d+/\\d+ \\(\\d+(?:\\.\\d+)?%%\\)|\\{(?:non-strict|strict|unsupported): \\[.*\\],?\\}))?[^\\S\\r\\n]*(.*)");

    /**
     * @see https://github.com/tc39/test262/blob/main/INTERPRETING.md#host-defined-functions
     */
    public static class $262 extends ScriptableObject {

        $262() {
            super();
        }

        $262(Scriptable scope, Scriptable prototype) {
            super(scope, prototype);
        }

        static $262 init(Context cx, Scriptable scope) {
            $262 proto = new $262();
            proto.setPrototype(getObjectPrototype(scope));
            proto.setParentScope(scope);

            proto.defineProperty(scope, "gc", 0, $262::gc, DONTENUM, DONTENUM | READONLY);
            proto.defineProperty(
                    scope, "createRealm", 0, $262::createRealm, DONTENUM, DONTENUM | READONLY);
            proto.defineProperty(
                    scope, "evalScript", 1, $262::evalScript, DONTENUM, DONTENUM | READONLY);
            proto.defineProperty(
                    scope,
                    "detachArrayBuffer",
                    0,
                    $262::detachArrayBuffer,
                    DONTENUM,
                    DONTENUM | READONLY);

            proto.defineProperty(cx, "global", $262::getGlobal, null, DONTENUM | READONLY);
            proto.defineProperty(cx, "agent", $262::getAgent, null, DONTENUM | READONLY);

            proto.defineProperty(SymbolKey.TO_STRING_TAG, "__262__", DONTENUM | READONLY);

            ScriptableObject.defineProperty(scope, "__262__", proto, DONTENUM);
            return proto;
        }

        static $262 install(ScriptableObject scope, Scriptable parentScope) {
            $262 instance = new $262(scope, parentScope);

            scope.put("$262", scope, instance);
            scope.setAttributes("$262", ScriptableObject.DONTENUM);

            return instance;
        }

        private static Object gc(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            System.gc();
            return Undefined.instance;
        }

        public static Object evalScript(
                Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            if (args.length == 0) {
                throw ScriptRuntime.throwError(cx, scope, "not enough args");
            }
            String source = Context.toString(args[0]);
            return cx.evaluateString(scope, source, "<evalScript>", 1, null);
        }

        public static Object getGlobal(Scriptable scriptable) {
            return scriptable.getParentScope();
        }

        public static $262 createRealm(
                Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            ScriptableObject realm = cx.initSafeStandardObjects();
            return install(realm, thisObj.getPrototype());
        }

        public static Object detachArrayBuffer(
                Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            throw new UnsupportedOperationException(
                    "$262.detachArrayBuffer() method not yet implemented");
        }

        public static Object getAgent(Scriptable scriptable) {
            throw new UnsupportedOperationException("$262.agent property not yet implemented");
        }

        @Override
        public String getClassName() {
            return "__262__";
        }
    }

    private Scriptable buildScope(Context cx, Test262Case testCase, boolean interpretedMode) {
        ScriptableObject scope = cx.initSafeStandardObjects();

        for (String harnessFile : testCase.harnessFiles) {
            String harnessKey = harnessFile + '-' + interpretedMode;
            Script harnessScript =
                    HARNESS_SCRIPT_CACHE.computeIfAbsent(
                            harnessKey,
                            k -> {
                                String harnessPath = testHarnessDir + harnessFile;
                                try (Reader reader = new FileReader(harnessPath)) {
                                    String script = Kit.readReader(reader);

                                    return cx.compileString(script, harnessPath, 1, null);
                                } catch (IOException ioe) {
                                    throw new RuntimeException(
                                            "Error reading test file " + harnessPath, ioe);
                                }
                            });
            harnessScript.exec(cx, scope);
        }

        $262 proto = $262.init(cx, scope);
        $262.install(scope, proto);
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

    @ParameterizedTest
    @MethodSource("test262SuiteValues")
    public void test262Case(
            String testFilePath,
            TestMode testMode,
            boolean useStrict,
            Test262Case testCase,
            boolean markedAsFailing) {
        try (Context cx = Context.enter()) {
            cx.setInterpretedMode(testMode == TestMode.INTERPRETED);
            cx.setGeneratingDebug(true);

            boolean failedEarly = false;
            try {
                Scriptable scope;
                try {
                    scope = buildScope(cx, testCase, testMode == TestMode.INTERPRETED);
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

                synchronized (RESULT_TRACKERS) {
                    TestResultTracker tracker = RESULT_TRACKERS.get(testCase);
                    if (tracker != null) {
                        tracker.passes(testMode, useStrict);
                    }
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

                synchronized (RESULT_TRACKERS) {
                    TestResultTracker tracker = RESULT_TRACKERS.get(testCase);
                    if (tracker != null) {
                        tracker.passes(testMode, useStrict);
                    }
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
        Map<String, File> fileLookup = new HashMap<>();
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
                    fileLookup.clear();
                    for (File file : topLevelFolderContents) {
                        fileLookup.put(
                                topLevelFolder
                                        .toPath()
                                        .relativize(file.toPath())
                                        .toString()
                                        .replaceAll("\\\\", "/"),
                                file);
                    }

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

                // Now onto the files and folders listed under the topLevel folder
                if (path.endsWith(".js")) {
                    File file = fileLookup.get(path);
                    if (file != null) {
                        filesExpectedToFail.put(file, comment);
                        if (excludeTopLevelFolder) {
                            /* adding paths listed in the .properties file under the topLevel folder marked to skip
                             * to testFiles, in order to be able to not loose then when regenerate the .properties file
                             *
                             * Want to keep track of these files as they apparently failed at the time when the directory was marked to be skipped
                             */
                            testFiles.add(file);
                        }
                    } else {
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
                    tracker.setExpectations(TestMode.SKIPPED, true, false, false, true);
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
                                TestMode.SKIPPED,
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
                            TestMode.SKIPPED,
                            true,
                            testCase.hasFlag(FLAG_ONLY_STRICT),
                            testCase.hasFlag(FLAG_NO_STRICT),
                            true);
                }

                continue;
            }

            for (TestMode testMode : new TestMode[] {TestMode.INTERPRETED, TestMode.COMPILED}) {
                if (!testCase.hasFlag(FLAG_ONLY_STRICT) || testCase.hasFlag(FLAG_RAW)) {
                    result.add(
                            new Object[] {
                                caseShortPath, testMode, false, testCase, markedAsFailing
                            });
                    TestResultTracker tracker =
                            RESULT_TRACKERS.computeIfAbsent(
                                    testCase, k -> new TestResultTracker(comment));
                    tracker.setExpectations(
                            testMode,
                            false,
                            testCase.hasFlag(FLAG_ONLY_STRICT),
                            testCase.hasFlag(FLAG_NO_STRICT),
                            markedAsFailing);
                }

                if (!testCase.hasFlag(FLAG_NO_STRICT) && !testCase.hasFlag(FLAG_RAW)) {
                    result.add(
                            new Object[] {
                                caseShortPath, testMode, true, testCase, markedAsFailing
                            });
                    TestResultTracker tracker =
                            RESULT_TRACKERS.computeIfAbsent(
                                    testCase, k -> new TestResultTracker(comment));
                    tracker.setExpectations(
                            testMode,
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

    private enum TestMode {
        INTERPRETED,
        COMPILED,
        SKIPPED,
    }

    private static class TestResultTracker {
        private Set<String> modes = new HashSet<>();
        private boolean onlyStrict;
        private boolean noStrict;
        private boolean expectedFailure;
        private String comment;

        TestResultTracker(String comment) {
            this.comment = comment;
        }

        private static String makeKey(TestMode mode, boolean useStrict) {
            return mode.name().toLowerCase() + '-' + (useStrict ? "strict" : "non-strict");
        }

        public void setExpectations(
                TestMode mode,
                boolean useStrict,
                boolean onlyStrict,
                boolean noStrict,
                boolean expectedFailure) {

            modes.add(makeKey(mode, useStrict));
            this.onlyStrict = onlyStrict;
            this.noStrict = noStrict;
            this.expectedFailure = expectedFailure;
        }

        public boolean expectationsMet() {
            return modes.isEmpty();
        }

        public void passes(TestMode mode, boolean useStrict) {
            modes.remove(makeKey(mode, useStrict));
        }

        public String getResult(Test262Case tc) {
            // success on all optLevels in both strict and non-strict mode
            if (modes.isEmpty()) {
                return null;
            }

            // Test skipped due to dependencies on unsupported features/environment
            if (modes.contains("skipped-strict")) {
                List<String> feats = new ArrayList<>();

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
            if (modes.size() == 4) {
                return "";
            }

            // simplify the output for some cases
            ArrayList res = new ArrayList<>(modes);
            if (res.contains("compiled-non-strict") && res.contains("interpreted-non-strict")) {
                res.remove("compiled-non-strict");
                res.remove("interpreted-non-strict");
                res.add("non-strict");
            }
            if (res.contains("compiled-strict") && res.contains("interpreted-strict")) {
                res.remove("compiled-strict");
                res.remove("interpreted-strict");
                res.add("strict");
            }
            if (res.contains("compiled-strict") && res.contains("compiled-non-strict")) {
                res.remove("compiled-strict");
                res.remove("compiled-non-strict");
                res.add("compiled");
            }
            if (res.contains("interpreted-strict") && res.contains("interpreted-non-strict")) {
                res.remove("interpreted-strict");
                res.remove("interpreted-non-strict");
                res.add("interpreted");
            }

            if (res.size() > 1) {
                return '{' + String.join(",", res) + '}';
            }
            return String.join(",", res);
        }
    }
}
