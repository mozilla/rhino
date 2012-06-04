/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.testing;

import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

import junit.framework.Assert;

/**
 * <p>An error reporter for testing that verifies that messages reported to the
 * reporter are expected.</p>
 *
 * <p>Sample use</p>
 * <pre>
 * TestErrorReporter e =
 *   new TestErrorReporter(null, new String[] { "first warning" });
 * ...
 * assertTrue(e.hasEncounteredAllWarnings());
 * </pre>
 *
 * @author Pascal-Louis Perez
 */
public class TestErrorReporter extends Assert implements ErrorReporter {
    private final String[] errors;
    private final String[] warnings;
    private int errorsIndex = 0;
    private int warningsIndex = 0;

    public TestErrorReporter(String[] errors, String[] warnings) {
        this.errors = errors;
        this.warnings = warnings;
    }

    public void error(String message, String sourceName, int line,
                      String lineSource, int lineOffset) {
        if (errors != null && errorsIndex < errors.length) {
            assertEquals(errors[errorsIndex++], message);
        } else {
            fail("extra error: " + message);
        }
    }

    public void warning(String message, String sourceName, int line,
                        String lineSource, int lineOffset) {
        if (warnings != null && warningsIndex < warnings.length) {
            assertEquals(warnings[warningsIndex++], message);
        } else {
            fail("extra warning: " + message);
        }
    }

    public EvaluatorException runtimeError(
        String message, String sourceName, int line, String lineSource,
        int lineOffset) {
        throw new UnsupportedOperationException();
    }

   /**
    * Returns whether all warnings were reported to this reporter.
    */
    public boolean hasEncounteredAllWarnings() {
        return (warnings == null) ?
            warningsIndex == 0 :
            warnings.length == warningsIndex;
    }

   /**
    * Returns whether all errors were reported to this reporter.
    */
    public boolean hasEncounteredAllErrors() {
        return (errors == null) ?
            errorsIndex == 0 :
            errors.length == errorsIndex;
    }
}