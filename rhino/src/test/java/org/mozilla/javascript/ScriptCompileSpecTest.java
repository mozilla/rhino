package org.mozilla.javascript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ScriptCompileSpecTest {
    private Context cx;
    private TopLevel scope;

    @BeforeEach
    public void setUp() {
        cx = Context.enter();
        scope = cx.initStandardObjects();
    }

    @AfterEach
    public void tearDown() {
        Context.exit();
    }

    @Test
    public void testCompileScriptWithBuilder() {
        Script script =
                cx.compileScript(
                        ScriptCompileSpec.fromSource("var x = 42; x;")
                                .sourceName("test.js")
                                .lineno(1)
                                .build());

        assertNotNull(script);
        Object result =
                script.exec(cx, scope, ScriptableObject.getTopLevelScope(scope).getGlobalThis());
        assertEquals(42, ((Number) result).intValue());
    }

    @Test
    public void testCompilerEnvironsProcessorIsInvoked() {
        AtomicBoolean called = new AtomicBoolean(false);
        AtomicReference<CompilerEnvirons> received = new AtomicReference<>();

        Script script =
                cx.compileScript(
                        ScriptCompileSpec.fromSource("var z = 20; z;")
                                .sourceName("processed.js")
                                .compilerEnvironsProcessor(
                                        env -> {
                                            called.set(true);
                                            received.set(env);
                                            env.setGeneratingSource(true);
                                        })
                                .build());

        assertTrue(called.get(), "compilerEnvironsProcessor should be invoked");
        assertNotNull(received.get(), "processor should receive a CompilerEnvirons instance");
        assertTrue(
                received.get().isGeneratingSource(),
                "mutation in processor should take effect on the env passed to it");
        assertNotNull(script);
    }

    @Test
    public void testBuilderDefaults() {
        ScriptCompileSpec spec = ScriptCompileSpec.fromSource("1").build();

        assertEquals("1", spec.getSource());
        assertNull(spec.getSourceName());
        assertEquals(0, spec.getLineno());
        assertNull(spec.getSecurityDomain());
        assertNull(spec.getCompiler());
        assertNull(spec.getCompilationErrorReporter());
        assertNull(spec.getCompilerEnvironsProcessor());
    }

    @Test
    public void testNegativeLinenoNormalized() {
        ScriptCompileSpec spec = ScriptCompileSpec.fromSource("1").lineno(-5).build();

        assertEquals(0, spec.getLineno());
    }

    @Test
    public void testCompileFromReader() throws Exception {
        String code =
                "var data = [1, 2, 3, 4, 5];\n"
                        + "data.reduce(function(a, b) { return a + b; }, 0);";

        Script script =
                cx.compileScript(
                        ScriptCompileSpec.fromReader(new StringReader(code))
                                .sourceName("array-sum.js")
                                .lineno(1)
                                .build());

        Object result =
                script.exec(cx, scope, ScriptableObject.getTopLevelScope(scope).getGlobalThis());
        assertEquals(15, ((Number) result).intValue());
    }

    @Test
    public void testErrorReportsSourceNameAndLineno() {
        EvaluatorException ex =
                assertThrows(
                        EvaluatorException.class,
                        () ->
                                cx.compileScript(
                                        ScriptCompileSpec.fromSource("var x = ;")
                                                .sourceName("buggy-script.js")
                                                .lineno(42)
                                                .build()));
        assertEquals("buggy-script.js", ex.sourceName());
        assertEquals(42, ex.lineNumber());
    }

    @Test
    public void testCustomCompilationErrorReporterReceivesError() {
        AtomicReference<String> reportedSource = new AtomicReference<>();
        AtomicReference<Integer> reportedLine = new AtomicReference<>();
        AtomicBoolean errorCalled = new AtomicBoolean(false);

        ErrorReporter capturing =
                new ErrorReporter() {
                    @Override
                    public void warning(
                            String message,
                            String sourceName,
                            int line,
                            String lineSource,
                            int lineOffset) {
                        throw new AssertionError("should not be called");
                    }

                    @Override
                    public void error(
                            String message,
                            String sourceName,
                            int line,
                            String lineSource,
                            int lineOffset) {
                        errorCalled.set(true);
                        reportedSource.set(sourceName);
                        reportedLine.set(line);
                        throw new EvaluatorException(message, sourceName, line);
                    }

                    @Override
                    public EvaluatorException runtimeError(
                            String message,
                            String sourceName,
                            int line,
                            String lineSource,
                            int lineOffset) {
                        throw new AssertionError("should not be called");
                    }
                };

        assertThrows(
                EvaluatorException.class,
                () ->
                        cx.compileScript(
                                ScriptCompileSpec.fromSource("var x = ;")
                                        .sourceName("custom.js")
                                        .lineno(7)
                                        .compilationErrorReporter(capturing)
                                        .build()));

        assertTrue(errorCalled.get(), "custom error reporter should be invoked");
        assertEquals("custom.js", reportedSource.get());
        assertEquals(7, reportedLine.get());
    }

    @Test
    public void testSecurityDomainWithoutControllerThrows() {
        Object securityDomain = new Object();
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        cx.compileScript(
                                ScriptCompileSpec.fromSource("1")
                                        .securityDomain(securityDomain)
                                        .build()));
    }

    @Test
    public void testLegacyCompileStringMatchesSpec() {
        Script oldWay = cx.compileString("var a = 1; a + 2;", "old.js", 1, null);
        Script newWay =
                cx.compileScript(
                        ScriptCompileSpec.fromSource("var a = 1; a + 2;")
                                .sourceName("new.js")
                                .lineno(1)
                                .build());

        Object oldResult =
                oldWay.exec(cx, scope, ScriptableObject.getTopLevelScope(scope).getGlobalThis());
        Object newResult =
                newWay.exec(cx, scope, ScriptableObject.getTopLevelScope(scope).getGlobalThis());
        assertEquals(((Number) oldResult).intValue(), ((Number) newResult).intValue());
        assertEquals(3, ((Number) newResult).intValue());
    }

    @Test
    public void testEvaluateScriptUsesProvidedScope() {
        scope.put("seed", scope, 10);
        Object result =
                cx.evaluateScript(
                        ScriptCompileSpec.fromSource("seed * 4;").sourceName("eval.js").build(),
                        scope);

        assertEquals(40, ((Number) result).intValue());
    }

    @Test
    public void testEvaluateLegacyMatchesSpec() {
        String src = "var a = 5; a * a;";
        Object oldWay = cx.evaluateString(scope, src, "old.js", 1, null);
        Object newWay =
                cx.evaluateScript(
                        ScriptCompileSpec.fromSource(src).sourceName("new.js").lineno(1).build(),
                        scope);

        assertEquals(oldWay, newWay);
        assertEquals(25, ((Number) newWay).intValue());
    }
}
