package org.mozilla.javascript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FunctionCompileSpecTest {
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
    public void testCompileFunctionWithBuilder() {
        Function func =
                cx.compileFunction(
                        FunctionCompileSpec.fromSource(
                                        "function add(a, b) { return a + b; }", scope)
                                .sourceName("test.js")
                                .lineno(1)
                                .build());

        assertNotNull(func);
        Object result =
                func.call(
                        cx,
                        scope,
                        ScriptableObject.getTopLevelScope(scope).getGlobalThis(),
                        new Object[] {5, 7});
        assertEquals(12, ((Number) result).intValue());
    }

    @Test
    public void testCompileFunctionFromReader() throws Exception {
        String code = "function mul(a, b) { return a * b; }";

        Function func =
                cx.compileFunction(
                        FunctionCompileSpec.fromReader(new StringReader(code), scope)
                                .sourceName("mul.js")
                                .build());

        Object result =
                func.call(
                        cx,
                        scope,
                        ScriptableObject.getTopLevelScope(scope).getGlobalThis(),
                        new Object[] {6, 7});
        assertEquals(42, ((Number) result).intValue());
    }

    @Test
    public void testBuilderDefaults() {
        FunctionCompileSpec spec = FunctionCompileSpec.fromSource("function f() {}", scope).build();

        assertEquals("function f() {}", spec.getSource());
        assertSame(scope, spec.getScope());
        assertNull(spec.getSourceName());
        assertEquals(0, spec.getLineno());
        assertNull(spec.getSecurityDomain());
        assertNull(spec.getCompiler());
        assertNull(spec.getCompilationErrorReporter());
        assertNull(spec.getCompilerEnvironsProcessor());
    }

    @Test
    public void testNegativeLinenoNormalized() {
        FunctionCompileSpec spec =
                FunctionCompileSpec.fromSource("function f() {}", scope).lineno(-5).build();

        assertEquals(0, spec.getLineno());
    }

    @Test
    public void testScopeIsRequiredAtFromSource() {
        var ex =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> FunctionCompileSpec.fromSource("function f() {}", null));
        assertTrue(ex.getMessage().contains("scope is required"));
    }

    @Test
    public void testScopeIsRequiredAtFromReader() {
        var ex =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                FunctionCompileSpec.fromReader(
                                        new StringReader("function f() {}"), null));
        assertTrue(ex.getMessage().contains("scope is required"));
    }

    @Test
    public void testRecordCompactConstructorRejectsNullScope() {
        var ex =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> FunctionCompileSpec.fromSource("function f() {}", null).build());
        assertTrue(ex.getMessage().contains("scope is required"));
    }

    @Test
    public void testLegacyCompileFunctionMatchesSpec() {
        Function oldWay =
                cx.compileFunction(
                        scope, "function add(a, b) { return a + b; }", "old.js", 1, null);
        Function newWay =
                cx.compileFunction(
                        FunctionCompileSpec.fromSource(
                                        "function add(a, b) { return a + b; }", scope)
                                .sourceName("new.js")
                                .lineno(1)
                                .build());

        Object[] args = new Object[] {1, 2};
        Object oldResult =
                oldWay.call(
                        cx, scope, ScriptableObject.getTopLevelScope(scope).getGlobalThis(), args);
        Object newResult =
                newWay.call(
                        cx, scope, ScriptableObject.getTopLevelScope(scope).getGlobalThis(), args);
        assertEquals(((Number) oldResult).intValue(), ((Number) newResult).intValue());
        assertEquals(3, ((Number) newResult).intValue());
    }

    @Test
    public void testNonFunctionSourceThrows() {
        var ex =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                cx.compileFunction(
                                        FunctionCompileSpec.fromSource("var x = 1;", scope)
                                                .sourceName("not-a-function.js")
                                                .build()));
        assertTrue(
                ex.getMessage()
                        .contains("compileFunction only accepts source with single JS function"),
                "message should explain the constraint, was: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("var x = 1;"), "message should include the source");
    }

    @Test
    public void testFunctionFollowedByEmptyStatementsIsAccepted() {
        // The "first child is FUNCTION" check explicitly tolerates trailing empty statements.
        Function func =
                cx.compileFunction(
                        FunctionCompileSpec.fromSource("function f() { return 1; };;;", scope)
                                .sourceName("trailing.js")
                                .build());
        Object result =
                func.call(
                        cx,
                        scope,
                        ScriptableObject.getTopLevelScope(scope).getGlobalThis(),
                        new Object[0]);
        assertEquals(1, ((Number) result).intValue());
    }

    @Test
    public void testCompilerEnvironsProcessorIsInvoked() {
        AtomicBoolean called = new AtomicBoolean(false);
        AtomicReference<CompilerEnvirons> received = new AtomicReference<>();

        Function func =
                cx.compileFunction(
                        FunctionCompileSpec.fromSource("function f() { return 7; }", scope)
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

        Object result =
                func.call(
                        cx,
                        scope,
                        ScriptableObject.getTopLevelScope(scope).getGlobalThis(),
                        new Object[0]);
        assertEquals(7, ((Number) result).intValue());
    }
}
