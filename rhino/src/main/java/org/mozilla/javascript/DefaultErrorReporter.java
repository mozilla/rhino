/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * This is the default error reporter for JavaScript.
 *
 * @author Norris Boyd
 */
class DefaultErrorReporter implements ErrorReporter {
    static final DefaultErrorReporter instance = new DefaultErrorReporter();

    private boolean forEval;
    private ErrorReporter chainedReporter;

    private DefaultErrorReporter() {}

    static ErrorReporter forEval(ErrorReporter reporter) {
        DefaultErrorReporter r = new DefaultErrorReporter();
        r.forEval = true;
        r.chainedReporter = reporter;
        return r;
    }

    @Override
    public void warning(
            String message, String sourceURI, int line, String lineText, int lineOffset) {
        if (chainedReporter != null) {
            chainedReporter.warning(message, sourceURI, line, lineText, lineOffset);
        } else {
            // Do nothing
        }
    }

    @Override
    public void error(String message, String sourceURI, int line, String lineText, int lineOffset) {
        if (forEval) {
            throw ScriptRuntime.constructError(
                    "SyntaxError", message, sourceURI, line, lineText, lineOffset);
        }
        if (chainedReporter != null) {
            chainedReporter.error(message, sourceURI, line, lineText, lineOffset);
        } else {
            throw runtimeError(message, sourceURI, line, lineText, lineOffset);
        }
    }

    @Override
    public EvaluatorException runtimeError(
            String message, String sourceURI, int line, String lineText, int lineOffset) {
        if (chainedReporter != null) {
            return chainedReporter.runtimeError(message, sourceURI, line, lineText, lineOffset);
        }
        return new EvaluatorException(message, sourceURI, line, lineText, lineOffset);
    }
}
