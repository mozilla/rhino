/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.ast;

import java.util.ArrayList;
import java.util.List;
import org.mozilla.javascript.EvaluatorException;

/**
 * An error reporter that gathers the errors and warnings for later display. This a useful {@link
 * org.mozilla.javascript.ErrorReporter} when the {@link org.mozilla.javascript.CompilerEnvirons} is
 * set to ide-mode (for IDEs).
 *
 * @author Steve Yegge
 */
public class ErrorCollector implements IdeErrorReporter {

    private List<ParseProblem> errors = new ArrayList<>();

    /**
     * This is not called during AST generation. {@link #warning(String,String,int,int)} is used
     * instead.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public void warning(
            String message, String sourceName, int line, String lineSource, int lineOffset) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public void warning(String message, String sourceName, int offset, int length) {
        errors.add(
                new ParseProblem(ParseProblem.Type.Warning, message, sourceName, offset, length));
    }

    /**
     * This is not called during AST generation. {@link #warning(String,String,int,int)} is used
     * instead.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public void error(
            String message, String sourceName, int line, String lineSource, int lineOffset) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public void error(String message, String sourceName, int fileOffset, int length) {
        errors.add(
                new ParseProblem(ParseProblem.Type.Error, message, sourceName, fileOffset, length));
    }

    /** {@inheritDoc} */
    @Override
    public EvaluatorException runtimeError(
            String message, String sourceName, int line, String lineSource, int lineOffset) {
        throw new UnsupportedOperationException();
    }

    /** Returns the list of errors and warnings produced during parsing. */
    public List<ParseProblem> getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(errors.size() * 100);
        for (ParseProblem pp : errors) {
            sb.append(pp.toString()).append("\n");
        }
        return sb.toString();
    }
}
