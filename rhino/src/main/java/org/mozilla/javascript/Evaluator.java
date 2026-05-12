/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.List;
import org.mozilla.javascript.ast.ScriptNode;

/** Abstraction of evaluation, which can be implemented either by an interpreter or compiler. */
public interface Evaluator {

    /**
     * Compile a script from intermediate representation tree into an executable form.
     *
     * @param compilerEnv Compiler environment
     * @param tree parse tree
     * @param rawSource the source code
     * @return a result that can be passed to {@link #createScriptObject}
     */
    CompilationResult<JSScript> compileScript(
            CompilerEnvirons compilerEnv, ScriptNode tree, String rawSource);

    /**
     * Compile a function from intermediate representation tree into an executable form.
     *
     * @param compilerEnv Compiler environment
     * @param tree parse tree
     * @param rawSource the source code
     * @return a result that can be passed to {@link #createFunctionObject}
     */
    CompilationResult<JSFunction> compileFunction(
            CompilerEnvirons compilerEnv, ScriptNode tree, String rawSource);

    /**
     * Create a function object.
     *
     * @param cx Current context
     * @param scope scope of the function
     * @param compiled the result returned by {@link #compileFunction}
     * @param staticSecurityDomain security domain
     * @return Function object that can be called
     */
    Function createFunctionObject(
            Context cx,
            VarScope scope,
            CompilationResult<JSFunction> compiled,
            Object staticSecurityDomain);

    /**
     * Create a script object.
     *
     * @param compiled the result returned by {@link #compileScript}
     * @param staticSecurityDomain security domain
     * @return Script object that can be evaluated
     */
    Script createScriptObject(CompilationResult<JSScript> compiled, Object staticSecurityDomain);

    /**
     * Capture stack information from the given exception.
     *
     * @param ex an exception thrown during execution
     */
    public void captureStackInfo(RhinoException ex);

    /**
     * Get the source position information by examining the stack.
     *
     * @param cx Context
     * @param linep Array object of length &gt;= 1; getSourcePositionFromStack will assign the line
     *     number to linep[0].
     * @return the name of the file or other source container
     */
    public String getSourcePositionFromStack(Context cx, int[] linep);

    /**
     * Given a native stack trace, patch it with script-specific source and line information
     *
     * @param ex exception
     * @param nativeStackTrace the native stack trace
     * @return patched stack trace
     */
    public String getPatchedStack(RhinoException ex, String nativeStackTrace);

    /**
     * Get the script stack for the given exception
     *
     * @param ex exception from execution
     * @return list of strings for the stack trace
     */
    public List<String> getScriptStack(RhinoException ex);
}
