/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.sourcemap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Runs the <a href="https://github.com/tc39/source-map-tests">tc39/source-map-tests</a> suite
 * against {@link SourceMapV3}. The suite lives in the {@code tests/source-map-tests} git submodule
 * and must be initialised before these tests can run ({@code git submodule update --init}).
 *
 * <h2>Excludelist</h2>
 *
 * Cases that are known to fail are listed in {@code tests/testsrc/source-map-tests-excludelist.txt}
 * (one {@code name} value per line; blank lines and {@code #} comments are ignored). A listed case
 * is silently skipped. Two failure modes keep the list honest:
 *
 * <ul>
 *   <li>A case that is <em>on</em> the excludelist but currently <em>passes</em> fails the build,
 *       so stale entries are caught immediately.
 *   <li>A case that is <em>not</em> on the excludelist but currently <em>fails</em> also fails the
 *       build, so regressions are caught immediately.
 * </ul>
 *
 * <h2>Regenerating the excludelist</h2>
 *
 * To rewrite the excludelist to exactly the set of currently failing cases (e.g. after fixing bugs
 * or after the submodule is updated to a newer suite version), run:
 *
 * <pre>
 * ./gradlew :tests:test --tests SourceMapSpecSuiteTest -DupdateSourceMapTestsExcludelist=true
 * </pre>
 *
 * When {@code updateSourceMapTestsExcludelist} is set, all failures are suppressed and the file is
 * overwritten at the end of the run. Review the diff before committing.
 */
class SourceMapSpecSuiteTest {

    private static final Path SUITE_ROOT = Paths.get("source-map-tests");
    private static final Path TESTS_JSON = SUITE_ROOT.resolve("source-map-spec-tests.json");
    private static final Path RESOURCES = SUITE_ROOT.resolve("resources");
    private static final Path EXCLUDELIST =
            Paths.get("testsrc", "source-map-tests-excludelist.txt");

    private static final boolean UPDATE_MODE =
            System.getProperty("updateSourceMapTestsExcludelist") != null;

    private static Set<String> excludelist;
    private static final Set<String> currentlyFailing = ConcurrentHashMap.newKeySet();
    private static final Set<String> caseNames = new LinkedHashSet<>();

    @BeforeAll
    static void loadExcludelist() throws IOException {
        excludelist = new LinkedHashSet<>();
        if (Files.exists(EXCLUDELIST)) {
            for (String line : Files.readAllLines(EXCLUDELIST, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                excludelist.add(trimmed);
            }
        }
    }

    @TestFactory
    Collection<DynamicTest> specCases() throws IOException {
        if (!Files.exists(TESTS_JSON)) {
            return List.of(
                    DynamicTest.dynamicTest(
                            "missing-suite",
                            () ->
                                    fail(
                                            "tc39/source-map-tests submodule not initialized; run "
                                                    + "`git submodule update --init`")));
        }
        String json = Files.readString(TESTS_JSON, StandardCharsets.UTF_8);
        Object root = SourceMapJsonParser.parse(json);
        if (!(root instanceof Map)) fail("source-map-spec-tests.json must be a JSON object");
        @SuppressWarnings("unchecked")
        Map<String, Object> rootObj = (Map<String, Object>) root;
        Object testsValue = rootObj.get("tests");
        if (!(testsValue instanceof List))
            fail("source-map-spec-tests.json must have a 'tests' array");
        @SuppressWarnings("unchecked")
        List<Object> raw = (List<Object>) testsValue;

        List<DynamicTest> tests = new ArrayList<>();
        for (Object entry : raw) {
            if (!(entry instanceof Map)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> obj = (Map<String, Object>) entry;
            String name = (String) obj.get("name");
            if (name == null) continue;
            caseNames.add(name);
            tests.add(DynamicTest.dynamicTest(name, () -> runCase(obj)));
        }
        return tests;
    }

    private void runCase(Map<String, Object> obj) {
        String name = (String) obj.get("name");
        String sourceMapFile = (String) obj.get("sourceMapFile");
        Boolean expectedValid = (Boolean) obj.get("sourceMapIsValid");
        boolean isOnExcludelist = excludelist.contains(name);

        boolean passed;
        try {
            assertCase(name, sourceMapFile, expectedValid, obj);
            passed = true;
        } catch (Throwable t) {
            currentlyFailing.add(name);
            if (UPDATE_MODE || isOnExcludelist) {
                return; // suppress; recorded above
            }
            if (t instanceof RuntimeException) throw (RuntimeException) t;
            if (t instanceof AssertionError) throw (AssertionError) t;
            throw new RuntimeException(t);
        }
        if (passed && isOnExcludelist && !UPDATE_MODE) {
            fail("case '" + name + "' is on the excludelist but currently passes — remove it.");
        }
    }

    private void assertCase(
            String name, String sourceMapFile, Boolean expectedValid, Map<String, Object> obj)
            throws IOException {
        if (sourceMapFile == null || expectedValid == null) {
            fail("case '" + name + "' is missing sourceMapFile or sourceMapIsValid");
        }
        Path mapPath = RESOURCES.resolve(sourceMapFile);
        String json = Files.readString(mapPath, StandardCharsets.UTF_8);

        SourceMapV3 parsed = null;
        SourceMapException parseError = null;
        try {
            parsed = SourceMapV3.parse(json);
        } catch (SourceMapException e) {
            parseError = e;
        }

        if (Boolean.TRUE.equals(expectedValid)) {
            if (parsed == null) {
                fail("expected valid map but parse failed: " + parseError.getMessage());
            }
        } else {
            if (parsed != null) {
                fail("expected invalid map but parse succeeded");
            }
        }

        if (parsed != null) {
            @SuppressWarnings("unchecked")
            List<Object> actions = (List<Object>) obj.get("testActions");
            if (actions != null) {
                for (Object a : actions) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> action = (Map<String, Object>) a;
                    String type = (String) action.get("actionType");
                    if ("checkMapping".equals(type)) {
                        runCheckMapping(parsed, action);
                    } else if ("checkIgnoreList".equals(type)) {
                        runCheckIgnoreList(parsed, action);
                    } else {
                        fail("unsupported testActions type: " + type);
                    }
                }
            }
        }
    }

    private static void runCheckMapping(SourceMapV3 parsed, Map<String, Object> action) {
        int genLine = ((Long) action.get("generatedLine")).intValue() + 1;
        int genCol = ((Long) action.get("generatedColumn")).intValue() + 1;
        Position p = parsed.mapPosition(genLine, genCol);
        String origSource = (String) action.get("originalSource");
        Object origLine = action.get("originalLine");
        Object origCol = action.get("originalColumn");

        if (origSource == null && origLine == null && origCol == null) {
            // Action asserts that no mapping exists.
            if (p != null) {
                fail("expected no mapping at (" + genLine + "," + genCol + ") but got " + p);
            }
            return;
        }

        if (p == null) {
            fail("expected mapping at (" + genLine + "," + genCol + ") but got null");
        }
        if (origSource != null) {
            assertEquals(origSource, p.sourcePath(), "sourcePath mismatch");
        }
        if (origLine != null) {
            int expected = ((Long) origLine).intValue() + 1;
            assertEquals(expected, p.line(), "source line mismatch");
        }
        if (origCol != null) {
            int expected = ((Long) origCol).intValue() + 1;
            assertEquals(expected, p.column(), "source column mismatch");
        }
        if (action.containsKey("mappedName")) {
            String expectedName = (String) action.get("mappedName");
            assertEquals(
                    expectedName, parsed.getMappedName(genLine, genCol), "mappedName mismatch");
        }
    }

    private static void runCheckIgnoreList(SourceMapV3 parsed, Map<String, Object> action) {
        @SuppressWarnings("unchecked")
        List<Object> present = (List<Object>) action.get("present");
        if (present == null) {
            fail("checkIgnoreList missing 'present' field");
        }
        List<String> ignoreList = parsed.ignoreList();
        for (Object e : present) {
            String path = (String) e;
            if (!ignoreList.contains(path)) {
                fail("expected '" + path + "' to be in ignoreList, but got: " + ignoreList);
            }
        }
    }

    @AfterAll
    static void writeExcludelistIfRequested() throws IOException {
        if (!UPDATE_MODE) return;
        Set<String> sorted = new TreeSet<>(currentlyFailing);
        StringBuilder sb = new StringBuilder();
        sb.append("# Auto-generated; review diff before committing.\n");
        sb.append("# One source-map-spec-tests.json case \"name\" per line.\n");
        for (String n : sorted) sb.append(n).append('\n');
        Files.writeString(EXCLUDELIST, sb.toString(), StandardCharsets.UTF_8);
    }
}
