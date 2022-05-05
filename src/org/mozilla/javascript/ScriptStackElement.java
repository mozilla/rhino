/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.io.Serializable;

/**
 * This class represents an element on the script execution stack.
 *
 * @see RhinoException#getScriptStack()
 * @author Hannes Wallnoefer
 * @since 1.7R3
 */
public final class ScriptStackElement implements Serializable {

    private static final long serialVersionUID = -6416688260860477449L;

    public final String fileName;
    public final String functionName;
    public final int lineNumber;

    public ScriptStackElement(String fileName, String functionName, int lineNumber) {
        this.fileName = fileName;
        this.functionName = functionName;
        this.lineNumber = lineNumber;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        renderMozillaStyle(sb);
        return sb.toString();
    }

    /**
     * Render stack element in Java-inspired style: <code>    at fileName:lineNumber (functionName)
     * </code>
     *
     * @param sb the StringBuilder to append to
     */
    public void renderJavaStyle(StringBuilder sb) {
        sb.append("\tat ").append(fileName);
        if (lineNumber > -1) {
            sb.append(':').append(lineNumber);
        }
        if (functionName != null) {
            sb.append(" (").append(functionName).append(')');
        }
    }

    /**
     * Render stack element in Mozilla/Firefox style: <code>functionName()@fileName:lineNumber
     * </code>
     *
     * @param sb the StringBuilder to append to
     */
    public void renderMozillaStyle(StringBuilder sb) {
        if (functionName != null) {
            sb.append(functionName).append("()");
        }
        sb.append('@').append(fileName);
        if (lineNumber > -1) {
            sb.append(':').append(lineNumber);
        }
    }

    /**
     * Render stack element in V8 style: <code>
     *     at functionName (fileName:lineNumber:columnNumber)</code> or: <code>
     *     at fileName:lineNumber:columnNumber</code>
     *
     * @param sb the StringBuilder to append to
     */
    public void renderV8Style(StringBuilder sb) {
        sb.append("    at ");

        if ((functionName == null)
                || "anonymous".equals(functionName)
                || "undefined".equals(functionName)) {
            // Anonymous functions in V8 don't have names in the stack trace
            appendV8Location(sb);

        } else {
            sb.append(functionName).append(" (");
            appendV8Location(sb);
            sb.append(')');
        }
    }

    private void appendV8Location(StringBuilder sb) {
        sb.append(fileName).append(':');
        sb.append(lineNumber > -1 ? lineNumber : 0).append(":0");
    }
}
