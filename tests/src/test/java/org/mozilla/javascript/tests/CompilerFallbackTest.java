/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mozilla.classfile.ClassFileWriter.ClassSizeException;
import org.mozilla.javascript.CompilationResult;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Evaluator;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.FunctionCompileSpec;
import org.mozilla.javascript.JSFunction;
import org.mozilla.javascript.JSScript;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptCompileSpec;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.VarScope;
import org.mozilla.javascript.ast.ScriptNode;

/**
 * When the compiler throws {@link ClassSizeException} (e.g. method bytecode exceeds 64K), {@code
 * Context.compileImpl} must transparently fall back to the interpreter and produce a working
 * Script/Function.
 */
public class CompilerFallbackTest {
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
    public void scriptCompileFallsBackToInterpreterOnClassSizeException() {
        Script script =
                cx.compileScript(
                        ScriptCompileSpec.fromSource("1 + 2 + 3")
                                .sourceName("fallback.js")
                                .compiler(new AlwaysFailingEvaluator())
                                .build());

        assertNotNull(script);
        Object result = script.exec(cx, scope, scope.getGlobalThis());
        assertEquals(6, ((Number) result).intValue());
    }

    @Test
    public void functionCompileFallsBackToInterpreterOnClassSizeException() {
        Function func =
                cx.compileFunction(
                        FunctionCompileSpec.fromSource(
                                        "function add(a, b) { return a + b; }", scope)
                                .sourceName("fallback-fn.js")
                                .compiler(new AlwaysFailingEvaluator())
                                .build());

        assertNotNull(func);
        Object result = func.call(cx, scope, scope.getGlobalThis(), new Object[] {3, 4});
        assertEquals(7, ((Number) result).intValue());
    }

    /**
     * Stand-in for a {@link org.mozilla.javascript.optimizer.Codegen} that always hits a class-file
     * size limit during compilation. After the throw, {@code Context.compileImpl} should swap to a
     * fresh interpreter and complete compilation.
     */
    private static final class AlwaysFailingEvaluator implements Evaluator {
        @Override
        public CompilationResult<JSScript> compileScript(
                CompilerEnvirons compilerEnv, ScriptNode tree, String rawSource) {
            throw new ClassSizeException("simulated: method too big");
        }

        @Override
        public CompilationResult<JSFunction> compileFunction(
                CompilerEnvirons compilerEnv, ScriptNode tree, String rawSource) {
            throw new ClassSizeException("simulated: method too big");
        }

        @Override
        public Function createFunctionObject(
                Context cx,
                VarScope scope,
                CompilationResult<JSFunction> compiled,
                Object staticSecurityDomain) {
            throw new AssertionError(
                    "createFunctionObject must not be called on the failing compiler after fallback");
        }

        @Override
        public Script createScriptObject(
                CompilationResult<JSScript> compiled, Object staticSecurityDomain) {
            throw new AssertionError(
                    "createScriptObject must not be called on the failing compiler after fallback");
        }

        @Override
        public void captureStackInfo(RhinoException ex) {
            throw new AssertionError("should not have been called");
        }

        @Override
        public String getSourcePositionFromStack(Context cx, int[] linep) {
            throw new AssertionError("should not have been called");
        }

        @Override
        public String getPatchedStack(RhinoException ex, String nativeStackTrace) {
            throw new AssertionError("should not have been called");
        }

        @Override
        public List<String> getScriptStack(RhinoException ex) {
            throw new AssertionError("should not have been called");
        }
    }
}
