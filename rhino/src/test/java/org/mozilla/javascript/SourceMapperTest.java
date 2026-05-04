/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.debug.DebugFrame;
import org.mozilla.javascript.debug.DebuggableScript;
import org.mozilla.javascript.debug.Debugger;
import org.mozilla.javascript.sourcemap.Position;
import org.mozilla.javascript.sourcemap.SourceMapper;
import org.mozilla.javascript.testutils.Utils;

class SourceMapperTest {

    /**
     * Maps target line N to source line {@code 100 + N} and column M to {@code 200 + M}, using a
     * fixed source path. Returns null for any target line in {@code skipLines} so we can exercise
     * the skip-on-null path.
     */
    private static final class TestMapper implements SourceMapper {
        private static final String SOURCE_PATH = "original.js";

        private final Set<Integer> skipLines;
        private final String originalSource;
        private final List<String> originalLines;

        TestMapper(String originalSource, Integer... skipLines) {
            this.originalSource = originalSource;
            this.originalLines =
                    originalSource == null
                            ? List.of()
                            : Arrays.asList(originalSource.split("\n", -1));
            this.skipLines = new HashSet<>(Arrays.asList(skipLines));
        }

        @Override
        public Position mapPosition(int targetLine, int targetColumn) {
            if (skipLines.contains(targetLine)) return null;
            return new Position(SOURCE_PATH, 100 + targetLine, 200 + targetColumn);
        }

        @Override
        public String getPrimarySourceContent() {
            return originalSource;
        }

        @Override
        public String getSourceLineText(String sourcePath, int lineNumber) {
            if (!SOURCE_PATH.equals(sourcePath)) return null;
            // mapPosition emits source lines starting at 101 (target line 1 → 101). Honor the
            // same offset here so the source space is internally consistent.
            int idx = lineNumber - 101;
            if (idx < 0 || idx >= originalLines.size()) return null;
            return originalLines.get(idx);
        }
    }

    // ---- interpreter-only icode tests ----

    @Test
    void interpreterIcodeLineRemapped() {
        Utils.runWithMode(
                cx -> {
                    SourceMapper mapper = new TestMapper("// original");
                    String dump = captureIcodeDump(cx, "var x = 42; x;\n", mapper);

                    assertTrue(
                            dump.contains("LINE : 101"),
                            "expected mapped line 101 in icode dump but got:\n" + dump);
                    assertFalse(
                            dump.contains(" LINE : 1\n"),
                            "expected unmapped LINE : 1 to be absent from:\n" + dump);
                    return null;
                },
                true);
    }

    @Test
    void interpreterSkipsLineWhenMapperReturnsNull() {
        Utils.runWithMode(
                cx -> {
                    SourceMapper mapper = new TestMapper("// original\n// line 2\n", 2);
                    String dump = captureIcodeDump(cx, "var x = 1;\nvar y = 2;\n", mapper);

                    assertTrue(
                            dump.contains("LINE : 101"),
                            "expected line 101 in dump but got:\n" + dump);
                    assertFalse(
                            dump.contains("LINE : 102"),
                            "expected no entry for skipped line 2 in dump:\n" + dump);
                    assertFalse(
                            dump.contains(" LINE : 2\n"),
                            "expected no raw target line 2 in dump:\n" + dump);
                    return null;
                },
                true);
    }

    @Test
    void interpreterNoMapperLeavesIcodeUnchanged() {
        Utils.runWithMode(
                cx -> {
                    String dumpWithoutMapper = captureIcodeDump(cx, "var x = 42;\n", null);
                    assertTrue(
                            dumpWithoutMapper.contains("LINE : 1"),
                            "no-mapper dump should report the raw line 1");
                    return null;
                },
                true);
    }

    // ---- mode-agnostic tests ----

    @Test
    void stackTraceUsesMappedLine() {
        Utils.runWithAllModes(
                cx -> {
                    TopLevel scope = cx.initStandardObjects();
                    SourceMapper mapper = new TestMapper("throw 'err';\n");
                    Script script =
                            cx.compileScript(
                                    ScriptCompileSpec.fromSource("throw 'err';\n")
                                            .sourceName("transpiled.js")
                                            .lineno(1)
                                            .sourceMapper(mapper)
                                            .build());

                    RhinoException ex =
                            assertThrows(
                                    RhinoException.class,
                                    () -> script.exec(cx, scope, scope.getGlobalThis()));
                    assertEquals(101, ex.lineNumber(), "stack-trace line should be remapped");
                    return null;
                });
    }

    @Test
    void skippedLineFallsBackToPreviousMappedLine() {
        Utils.runWithAllModes(
                cx -> {
                    TopLevel scope = cx.initStandardObjects();
                    // Line 1 maps to 101, line 2 has no mapping. Throw on line 2 — expect line 101.
                    SourceMapper mapper = new TestMapper("var x = 1;\nthrow 'err';\n", 2);
                    Script script =
                            cx.compileScript(
                                    ScriptCompileSpec.fromSource("var x = 1;\nthrow 'err';\n")
                                            .sourceName("transpiled.js")
                                            .lineno(1)
                                            .sourceMapper(mapper)
                                            .build());

                    RhinoException ex =
                            assertThrows(
                                    RhinoException.class,
                                    () -> script.exec(cx, scope, scope.getGlobalThis()));
                    assertEquals(
                            101,
                            ex.lineNumber(),
                            "unmapped line should fall back to last mapped line");
                    return null;
                });
    }

    @Test
    void parserErrorsUseMappedPosition() {
        Utils.runWithAllModes(
                cx -> {
                    SourceMapper mapper = new TestMapper("let x = 'unterminated;\n");
                    RecordingErrorReporter reporter = new RecordingErrorReporter();
                    cx.setErrorReporter(reporter);

                    assertThrows(
                            EvaluatorException.class,
                            () ->
                                    cx.compileScript(
                                            ScriptCompileSpec.fromSource("var x = 'unterminated;\n")
                                                    .sourceName("transpiled.js")
                                                    .lineno(1)
                                                    .sourceMapper(mapper)
                                                    .build()));

                    assertFalse(reporter.errors.isEmpty(), "expected at least one error");
                    ReportedError err = reporter.errors.get(0);
                    assertEquals(101, err.line, "error line should be remapped");
                    assertEquals(
                            "let x = 'unterminated;",
                            err.lineSource,
                            "lineSource should come from the mapper");
                    return null;
                });
    }

    @Test
    void compileFunctionStackTraceUsesMappedLine() {
        Utils.runWithAllModes(
                cx -> {
                    TopLevel scope = cx.initStandardObjects();
                    SourceMapper mapper = new TestMapper("throw 'err';");

                    Function fn =
                            cx.compileFunction(
                                    FunctionCompileSpec.fromSource(
                                                    "function f() { throw 'err'; }", scope)
                                            .sourceName("transpiled.js")
                                            .lineno(1)
                                            .sourceMapper(mapper)
                                            .build());

                    RhinoException ex =
                            assertThrows(
                                    RhinoException.class,
                                    () -> fn.call(cx, scope, scope.getGlobalThis(), new Object[0]));
                    assertEquals(101, ex.lineNumber(), "function-path line should be remapped");
                    return null;
                });
    }

    // ---- debugger tests, interpreter-only  ----

    @Test
    void debuggerReceivesOriginalSource() {
        Utils.runWithMode(
                cx -> {
                    String original = "// the original source\nfoo();\n";
                    SourceMapper mapper = new TestMapper(original);

                    RecordingDebugger debugger = new RecordingDebugger();
                    cx.setDebugger(debugger, null);
                    try {
                        cx.compileScript(
                                ScriptCompileSpec.fromSource("bar();\n")
                                        .sourceName("transpiled.js")
                                        .lineno(1)
                                        .sourceMapper(mapper)
                                        .build());
                    } finally {
                        cx.setDebugger(null, null);
                    }

                    assertFalse(debugger.sources.isEmpty(), "debugger should have been notified");
                    assertEquals(original, debugger.sources.iterator().next());
                    return null;
                },
                true);
    }

    @Test
    void debuggerFallsBackToTranspiledSourceWhenOriginalUnavailable() {
        Utils.runWithMode(
                cx -> {
                    SourceMapper mapper = new TestMapper(null);

                    RecordingDebugger debugger = new RecordingDebugger();
                    cx.setDebugger(debugger, null);
                    try {
                        cx.compileScript(
                                ScriptCompileSpec.fromSource("bar();\n")
                                        .sourceName("transpiled.js")
                                        .lineno(1)
                                        .sourceMapper(mapper)
                                        .build());
                    } finally {
                        cx.setDebugger(null, null);
                    }

                    assertEquals(
                            "bar();\n",
                            debugger.sources.iterator().next(),
                            "should fall back to the transpiled source");
                    return null;
                },
                true);
    }

    // ---- spec/builder unit tests ----

    @Test
    void scriptCompileSpecExposesSourceMapper() {
        SourceMapper mapper = new TestMapper("");
        ScriptCompileSpec spec = ScriptCompileSpec.fromSource("1").sourceMapper(mapper).build();
        assertEquals(mapper, spec.getSourceMapper());

        ScriptCompileSpec defaultSpec = ScriptCompileSpec.fromSource("1").build();
        assertNull(defaultSpec.getSourceMapper(), "sourceMapper should default to null");
    }

    @Test
    void functionCompileSpecExposesSourceMapper() {
        Utils.runWithMode(
                cx -> {
                    TopLevel scope = cx.initStandardObjects();
                    SourceMapper mapper = new TestMapper("");
                    FunctionCompileSpec spec =
                            FunctionCompileSpec.fromSource("function f() {}", scope)
                                    .sourceMapper(mapper)
                                    .build();
                    assertEquals(mapper, spec.getSourceMapper());
                    return null;
                },
                true);
    }

    // ---- realistic source-map integration test ----

    @Test
    void minifiedSourceMap() {
        // Simulates a two-line original file bundled onto one minified line:
        //   original line 1: "var x = 1;"   -> minified cols 1-8
        //   original line 2: "throw ..."    -> minified col 9 onward
        String originalSource = "var x = 1;\nthrow new Error(\"oops\");\n";
        String minifiedSource = "var x=1;throw new Error(\"oops\");\n";
        SourceMapper mapper =
                new SourceMapper() {
                    @Override
                    public Position mapPosition(int targetLine, int targetColumn) {
                        if (targetLine != 1) return null;
                        return targetColumn < 9
                                ? new Position(1, targetColumn)
                                : new Position(2, targetColumn - 8);
                    }

                    @Override
                    public String getOriginalSource() {
                        return originalSource;
                    }

                    @Override
                    public String getSourceLineText(int sourceLine) {
                        if (sourceLine == 1) return "var x = 1;";
                        if (sourceLine == 2) return "throw new Error(\"oops\");";
                        return null;
                    }
                };

        Utils.runWithAllModes(
                cx -> {
                    TopLevel scope = cx.initStandardObjects();
                    Script script =
                            cx.compileScript(
                                    ScriptCompileSpec.fromSource(minifiedSource)
                                            .sourceName("app.min.js")
                                            .lineno(1)
                                            .sourceMapper(mapper)
                                            .build());

                    RhinoException ex =
                            assertThrows(
                                    RhinoException.class,
                                    () -> script.exec(cx, scope, scope.getGlobalThis()));
                    assertEquals(
                            2,
                            ex.lineNumber(),
                            "throw at minified col 9 should map to original line 2");
                    return null;
                });
    }

    @Test
    void transpiledSourceMap() {
        // A computed-property literal ({["x"]: 1}) can't run on IE11, so a transpiler splits the
        // one original line into two: object creation + property assignment.  The throw lands on
        // transpiled line 2 but must be reported as original line 1.
        String originalSource = "var obj = {[\"x\"]: 1}; throw \"done\";\n";
        String transpiledSource = "var obj = {};\nobj[\"x\"] = 1; throw \"done\";\n";
        SourceMapper mapper =
                new SourceMapper() {
                    @Override
                    public Position mapPosition(int targetLine, int targetColumn) {
                        return new Position(1, targetColumn); // both lines originate from line 1
                    }

                    @Override
                    public String getOriginalSource() {
                        return originalSource;
                    }

                    @Override
                    public String getSourceLineText(int sourceLine) {
                        return sourceLine == 1 ? "var obj = {[\"x\"]: 1}; throw \"done\";" : null;
                    }
                };

        Utils.runWithAllModes(
                cx -> {
                    TopLevel scope = cx.initStandardObjects();
                    Script script =
                            cx.compileScript(
                                    ScriptCompileSpec.fromSource(transpiledSource)
                                            .sourceName("app.transpiled.js")
                                            .lineno(1)
                                            .sourceMapper(mapper)
                                            .build());

                    RhinoException ex =
                            assertThrows(
                                    RhinoException.class,
                                    () -> script.exec(cx, scope, scope.getGlobalThis()));
                    assertEquals(
                            1,
                            ex.lineNumber(),
                            "throw on transpiled line 2 should map back to original line 1");
                    return null;
                });
    }

    private String captureIcodeDump(Context cx, String source, SourceMapper mapper) {
        ScriptCompileSpec.Builder builder =
                ScriptCompileSpec.fromSource(source).sourceName("test").lineno(1);
        if (mapper != null) {
            builder.sourceMapper(mapper);
        }

        ScriptCompileSpec spec = builder.build();
        return InterpreterIcodeCapture.capture(() -> cx.compileScript(spec));
    }

    private static final class ReportedError {
        private final int line;
        private final String lineSource;

        private ReportedError(int line, String lineSource) {
            this.line = line;
            this.lineSource = lineSource;
        }
    }

    private static final class RecordingErrorReporter implements ErrorReporter {
        final List<ReportedError> errors = new ArrayList<>();

        @Override
        public void warning(
                String message, String sourceName, int line, String lineSource, int lineOffset) {
            throw new AssertionError("should not have happened");
        }

        @Override
        public void error(
                String message, String sourceName, int line, String lineSource, int lineOffset) {
            errors.add(new ReportedError(line, lineSource));
        }

        @Override
        public EvaluatorException runtimeError(
                String message, String sourceName, int line, String lineSource, int lineOffset) {
            return new EvaluatorException(message, sourceName, line, lineSource, lineOffset);
        }
    }

    private static final class RecordingDebugger implements Debugger {
        final Set<String> sources = new LinkedHashSet<>();

        @Override
        public void handleCompilationDone(Context cx, DebuggableScript fnOrScript, String source) {
            sources.add(source);
        }

        @Override
        public DebugFrame getFrame(Context cx, DebuggableScript fnOrScript) {
            return null;
        }
    }
}
