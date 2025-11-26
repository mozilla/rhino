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
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.drivers.TestUtils;
import org.mozilla.javascript.tools.SourceReader;
import org.mozilla.javascript.tools.shell.ShellContextFactory;
import org.mozilla.javascript.typedarrays.NativeArrayBuffer;
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

    static final Map<String, Script> HARNESS_SCRIPT_CACHE = new ConcurrentHashMap<>();
    static final Map<Test262Case, TestResultTracker> RESULT_TRACKERS = new LinkedHashMap<>();

    static ShellContextFactory CTX_FACTORY =
            new ShellContextFactory() {
                protected boolean hasFeature(Context cx, int featureIndex) {
                    if (Context.FEATURE_INTL_402 == featureIndex) {
                        return true;
                    }

                    return super.hasFeature(cx, featureIndex);
                }
            };

    static final Set<String> UNSUPPORTED_FEATURES =
            new HashSet<>(
                    Arrays.asList(
                            "Atomics",
                            "IsHTMLDDA",
                            "async-functions",
                            "async-iteration",
                            "class",
                            "class-fields-private",
                            "class-fields-public",
                            "new.target",
                            "resizable-arraybuffer",
                            "SharedArrayBuffer",
                            "tail-call-optimization",
                            "Temporal",
                            "upsert",
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
            Test262SuitePropertiesBuilder builder =
                    new Test262SuitePropertiesBuilder(testDir.toPath());
            for (Entry<Test262Case, TestResultTracker> entry : RESULT_TRACKERS.entrySet()) {
                builder.addTest(entry.getKey(), entry.getValue());
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(testProperties))) {
                builder.write(writer, statsEnabled, rollUpEnabled);
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

            proto.defineProperty(scope, "gc", 0, $262::gc);
            proto.defineProperty(scope, "createRealm", 0, $262::createRealm);
            proto.defineProperty(scope, "evalScript", 1, $262::evalScript);
            proto.defineProperty(scope, "detachArrayBuffer", 0, $262::detachArrayBuffer);

            proto.defineProperty(cx, scope, "global", $262::getGlobal, null, DONTENUM | READONLY);
            proto.defineProperty(cx, scope, "agent", $262::getAgent, null, DONTENUM | READONLY);

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
            return ((TopLevel) scriptable.getParentScope()).getGlobalThis();
        }

        public static $262 createRealm(
                Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            ScriptableObject realm = (ScriptableObject) cx.initSafeStandardObjects(new TopLevel());
            return install(realm, thisObj.getPrototype());
        }

        public static Object detachArrayBuffer(
                Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            Scriptable buf = ScriptRuntime.toObject(scope, args[0]);
            if (buf instanceof NativeArrayBuffer) {
                ((NativeArrayBuffer) buf).detach();
            }
            return Undefined.instance;
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
        ScriptableObject scope = (ScriptableObject) cx.initSafeStandardObjects(new TopLevel());

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
            harnessScript.exec(cx, scope, scope);
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
            // Ensure maximum compatibility, including future strict mode and "const" checks
            cx.setLanguageVersion(Context.VERSION_ECMASCRIPT);
            cx.setGeneratingDebug(true);

            boolean failedEarly = false;
            try {
                Scriptable scope = buildScope(cx, testCase, testMode == TestMode.INTERPRETED);
                String str = testCase.source;
                int line = 1;
                if (useStrict) {
                    str = "\"use strict\";\n" + str;
                    line--;
                }

                failedEarly = true;
                Script caseScript = cx.compileString(str, testFilePath, line, null);

                failedEarly = false; // not after this line
                caseScript.exec(cx, scope, scope);

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
            } catch (RuntimeException ex) {
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
            return flags != null && flags.contains(flag);
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
        private final Set<String> modes = new HashSet<>();
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

                if (tc.features != null) {
                    for (String feature : tc.features) {
                        if (UNSUPPORTED_FEATURES.contains(feature)) {
                            feats.add(feature);
                        }
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
            ArrayList<String> res = new ArrayList<>(modes);
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

    private static class Test262SuitePropertiesBuilder {
        private Path testDir;
        private DirectoryNode rootNode;

        Test262SuitePropertiesBuilder(Path testDir) {
            this.testDir = testDir;
            rootNode = new DirectoryNode(Path.of(""));
        }

        void addTest(
                Test262SuiteTest.Test262Case testCase,
                Test262SuiteTest.TestResultTracker resultTracker) {
            Path testFilePath = testDir.relativize(testCase.file.toPath());
            if (testCase.file.isDirectory()) {
                List<File> excludedFiles = new ArrayList<>();
                TestUtils.recursiveListFilesHelper(testCase.file, JS_FILE_FILTER, excludedFiles);
                buildNodeTree(testFilePath, (p) -> new ExcludeNode(p, excludedFiles.size()), true);
                return;
            }

            boolean isFailure = resultTracker.getResult(testCase) != null;
            DirectoryNode parentNode =
                    buildNodeTree(
                            testFilePath,
                            (p) -> new TestNode(p, testCase, resultTracker),
                            isFailure);
            parentNode.count(isFailure);
        }

        void write(Writer writer, boolean statsEnabled, boolean rollUpEnabled) throws IOException {
            writer.write(
                    "# This is a configuration file for Test262SuiteTest.java. See ./README.md for more info about this file\n");
            rootNode.writeChildNodes(writer, null, statsEnabled, rollUpEnabled, false);
        }

        private DirectoryNode buildNodeTree(
                Path testFilePath, Function<Path, Node> mappingFunction, boolean isFailure) {
            DirectoryNode parentNode = rootNode;
            Path nodePath = Path.of("");
            int i = 0;
            while (testFilePath.getNameCount() - 1 > i) {
                nodePath = Paths.get(nodePath.toString(), testFilePath.getName(i).toString());
                i++;

                parentNode =
                        (DirectoryNode)
                                parentNode.computeIfAbsent(nodePath, (p) -> new DirectoryNode(p));
                parentNode.deepCount(isFailure);
            }
            parentNode.computeIfAbsent(testFilePath, mappingFunction);
            return parentNode;
        }
    }

    private abstract static class Node {
        static final String INDENT = "    ";
        private Path path;

        Node(Path path) {
            this.path = path;
        }

        Path getPath() {
            return path;
        }

        abstract void write(
                Writer writer, DirectoryNode refParent, boolean statsEnabled, boolean rollUpEnabled)
                throws IOException;

        protected String buildRelativePath(Path parent, Path path) {
            return normalizePath(parent.relativize(path));
        }

        protected String normalizePath(Path path) {
            return path.toString().replace("\\", "/");
        }
    }

    private static class DirectoryNode extends Node {
        private LinkedHashMap<Path, Node> childNodes;
        private long testDeepCount;
        private long failureDeepCount;
        private long testCount;
        private long failureCount;

        DirectoryNode(Path path) {
            super(path);
            childNodes = new LinkedHashMap<>();
        }

        Node computeIfAbsent(Path path, Function<Path, Node> mappingFunction) {
            return childNodes.computeIfAbsent(path, mappingFunction);
        }

        void deepCount(boolean isFailure) {
            testDeepCount++;
            if (isFailure) {
                failureDeepCount++;
            }
        }

        void count(boolean isFailure) {
            testCount++;
            if (isFailure) {
                failureCount++;
            }
        }

        @Override
        void write(
                Writer writer, DirectoryNode refParent, boolean statsEnabled, boolean rollUpEnabled)
                throws IOException {
            int pathNameCount = 1;
            String normalizedPath = normalizePath(getPath());
            if ("language/expressions".equals(normalizedPath)
                    || "language/statements".equals(normalizedPath)) {
                pathNameCount = 2;
            }
            if (refParent == null) {
                if (testCount == 0
                        && failureDeepCount > 0
                        && getPath().getNameCount() == pathNameCount) {
                    writeChildNodes(writer, null, statsEnabled, rollUpEnabled, false);
                    return;
                }

                writer.write('\n');
                writer.write(normalizePath(getPath()));
                writer.write(statsText(statsEnabled, testDeepCount, failureDeepCount));
                writer.write('\n');

                writeChildNodes(writer, this, statsEnabled, rollUpEnabled, false);
                return;
            }

            if (failureDeepCount == 0) {
                return;
            }

            if (rollUpEnabled && testCount > 1 && failureCount == testCount) {
                writer.write(INDENT);
                writer.write(buildRelativePath(refParent.getPath(), getPath()));
                writer.write(statsText(statsEnabled, testCount, failureCount));
                writer.write('\n');

                writeChildNodes(writer, refParent, statsEnabled, rollUpEnabled, true);
                return;
            }

            writeChildNodes(writer, refParent, statsEnabled, rollUpEnabled, false);
        }

        private void writeChildNodes(
                Writer writer,
                DirectoryNode refParent,
                boolean statsEnabled,
                boolean rollUpEnabled,
                boolean onlyDirectories)
                throws IOException {
            for (Node node : childNodes.values()) {
                if (!onlyDirectories || node instanceof DirectoryNode) {
                    node.write(writer, refParent, statsEnabled, rollUpEnabled);
                }
            }
        }

        static String statsText(boolean statsEnabled, long tests, long failures) {
            if (!statsEnabled) {
                return "";
            }

            String output = " " + failures + "/" + tests;
            if (failures == tests) {
                return output + " (100.0%)";
            }

            double failurePercentage = 0d;
            if (tests > 0) {
                failurePercentage = failures * 100d / tests;
                failurePercentage = Math.round(failurePercentage * 100) / 100d;
            }

            return output + " (" + failurePercentage + "%)";
        }
    }

    private static final class ExcludeNode extends DirectoryNode {
        long excludedFilesCount;

        ExcludeNode(Path path, int excludedFilesCount) {
            super(path);
            this.excludedFilesCount = excludedFilesCount;
        }

        @Override
        void write(
                Writer writer, DirectoryNode refParent, boolean statsEnabled, boolean rollUpEnabled)
                throws IOException {
            writer.write("\n~");
            writer.write(normalizePath(getPath()));
            writer.write(statsText(statsEnabled, excludedFilesCount, excludedFilesCount));
            writer.write('\n');
        }
    }

    private static final class TestNode extends Node {
        private Test262SuiteTest.Test262Case testCase;
        private Test262SuiteTest.TestResultTracker resultTracker;

        TestNode(
                Path path,
                Test262SuiteTest.Test262Case testCase,
                Test262SuiteTest.TestResultTracker resultTracker) {
            super(path);

            this.testCase = testCase;
            this.resultTracker = resultTracker;
        }

        @Override
        void write(
                Writer writer, DirectoryNode refParent, boolean statsEnabled, boolean rollUpEnabled)
                throws IOException {
            String testResult = resultTracker.getResult(testCase);
            if (testResult == null) {
                return;
            }

            writer.write(INDENT);
            writer.write(buildRelativePath(refParent.getPath(), getPath()));
            if (statsEnabled && !testResult.isEmpty()) {
                writer.write(" ");
                writer.write(testResult);
            }
            if (resultTracker.comment != null && !resultTracker.comment.isEmpty()) {
                writer.write(" ");
                writer.write(resultTracker.comment);
            }
            writer.write('\n');
        }
    }
}
