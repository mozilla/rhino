/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1999.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1997-1999
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Norris Boyd
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU General Public License Version 2 or later (the "GPL"), in which
 * case the provisions of the GPL are applicable instead of those above. If
 * you wish to allow use of your version of this file only under the terms of
 * the GPL and not to allow others to use your version of this file under the
 * MPL, indicate your decision by deleting the provisions above and replacing
 * them with the notice and other provisions required by the GPL. If you do
 * not delete the provisions above, a recipient may use your version of this
 * file under either the MPL or the GPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.javascript;

/**
 * This is the default error reporter for JavaScript.
 *
 * @author Norris Boyd
 */
class DefaultErrorReporter implements ErrorReporter
{
    static final DefaultErrorReporter instance = new DefaultErrorReporter();

    private boolean forEval;
    private ErrorReporter chainedReporter;

    private DefaultErrorReporter() { }

    static ErrorReporter forEval(ErrorReporter reporter)
    {
        DefaultErrorReporter r = new DefaultErrorReporter();
        r.forEval = true;
        r.chainedReporter = reporter;
        return r;
    }

    public void warning(String message, String sourceURI, int line,
                        String lineText, int lineOffset)
    {
        if (chainedReporter != null) {
            chainedReporter.warning(
                message, sourceURI, line, lineText, lineOffset);
        } else {
            // Do nothing
        }
    }

    public void error(String message, String sourceURI, int line,
                      String lineText, int lineOffset)
    {
        if (forEval) {
            // Assume error message strings that start with "TypeError: "
            // should become TypeError exceptions. A bit of a hack, but we
            // don't want to change the ErrorReporter interface.
            // And the same for "ReferenceError"...
            // FIXME: This doesn't work for the french localized version!
            String error = "SyntaxError";
            final String TYPE_ERROR_NAME = "TypeError";
            final String REFERENCE_ERROR_NAME = "ReferenceError";
            final String DELIMETER = ": ";
            final String prefix = TYPE_ERROR_NAME + DELIMETER;
            if (message.startsWith(prefix)) {
                error = TYPE_ERROR_NAME;
                message = message.substring(prefix.length());
            } else {
                final String prefix2 = REFERENCE_ERROR_NAME + DELIMETER;
                if (message.startsWith(prefix2)) {
                    error = REFERENCE_ERROR_NAME;
                    message = message.substring(prefix2.length());
                }
            }
            throw ScriptRuntime.constructError(error, message, sourceURI,
                                               line, lineText, lineOffset);
        }
        if (chainedReporter != null) {
            chainedReporter.error(
                message, sourceURI, line, lineText, lineOffset);
        } else {
            throw runtimeError(
                message, sourceURI, line, lineText, lineOffset);
        }
    }

    public EvaluatorException runtimeError(String message, String sourceURI,
                                           int line, String lineText,
                                           int lineOffset)
    {
        if (chainedReporter != null) {
            return chainedReporter.runtimeError(
                message, sourceURI, line, lineText, lineOffset);
        } else {
            return new EvaluatorException(
                message, sourceURI, line, lineText, lineOffset);
        }
    }
}
