/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.io.CharArrayWriter;
import java.io.FilenameFilter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.mozilla.javascript.config.RhinoConfig;

/** The class of exceptions thrown by the JavaScript engine. */
public abstract class RhinoException extends RuntimeException {
    private static final Pattern JAVA_STACK_PATTERN = Pattern.compile("_c_(.*)_\\d+");

    RhinoException() {
        Evaluator e = Context.createInterpreter();
        if (e != null) e.captureStackInfo(this);
    }

    RhinoException(String details) {
        super(details);
        Evaluator e = Context.createInterpreter();
        if (e != null) e.captureStackInfo(this);
    }

    @Override
    public final String getMessage() {
        String details = details();
        if (sourceName == null || lineNumber <= 0) {
            return details;
        }
        StringBuilder buf = new StringBuilder(details);
        buf.append(" (");
        buf.append(sourceName);
        if (lineNumber > 0) {
            buf.append('#');
            buf.append(lineNumber);
        }
        buf.append(')');
        return buf.toString();
    }

    public String details() {
        return super.getMessage();
    }

    /**
     * Get the uri of the script source containing the error, or null if that information is not
     * available.
     */
    public final String sourceName() {
        return sourceName;
    }

    /**
     * Initialize the uri of the script source containing the error.
     *
     * @param sourceName the uri of the script source responsible for the error. It should not be
     *     <code>null</code>.
     * @throws IllegalStateException if the method is called more then once.
     */
    public final void initSourceName(String sourceName) {
        if (sourceName == null) throw new IllegalArgumentException();
        if (this.sourceName != null) throw new IllegalStateException();
        this.sourceName = sourceName;
    }

    /** Returns the line number of the statement causing the error, or zero if not available. */
    public final int lineNumber() {
        return lineNumber;
    }

    /**
     * Initialize the line number of the script statement causing the error.
     *
     * @param lineNumber the line number in the script source. It should be positive number.
     * @throws IllegalStateException if the method is called more then once.
     */
    public final void initLineNumber(int lineNumber) {
        if (lineNumber <= 0) throw new IllegalArgumentException(String.valueOf(lineNumber));
        if (this.lineNumber > 0) throw new IllegalStateException();
        this.lineNumber = lineNumber;
    }

    /** The column number of the location of the error, or zero if unknown. */
    public final int columnNumber() {
        return columnNumber;
    }

    /**
     * Initialize the column number of the script statement causing the error.
     *
     * @param columnNumber the column number in the script source. It should be positive number.
     * @throws IllegalStateException if the method is called more then once.
     */
    public final void initColumnNumber(int columnNumber) {
        if (columnNumber <= 0) throw new IllegalArgumentException(String.valueOf(columnNumber));
        if (this.columnNumber > 0) throw new IllegalStateException();
        this.columnNumber = columnNumber;
    }

    /** The source text of the line causing the error, or null if unknown. */
    public final String lineSource() {
        return lineSource;
    }

    /**
     * Initialize the text of the source line containing the error.
     *
     * @param lineSource the text of the source line responsible for the error. It should not be
     *     <code>null</code>.
     * @throws IllegalStateException if the method is called more then once.
     */
    public final void initLineSource(String lineSource) {
        if (lineSource == null) throw new IllegalArgumentException();
        if (this.lineSource != null) throw new IllegalStateException();
        this.lineSource = lineSource;
    }

    final void recordErrorOrigin(
            String sourceName, int lineNumber, String lineSource, int columnNumber) {
        // XXX: for compatibility allow for now -1 to mean 0
        if (lineNumber == -1) {
            lineNumber = 0;
        }

        if (sourceName != null) {
            initSourceName(sourceName);
        }
        if (lineNumber != 0) {
            initLineNumber(lineNumber);
        }
        if (lineSource != null) {
            initLineSource(lineSource);
        }
        if (columnNumber != 0) {
            initColumnNumber(columnNumber);
        }
    }

    private String generateStackTrace() {
        // Get stable reference to work properly with concurrent access
        CharArrayWriter writer = new CharArrayWriter();
        super.printStackTrace(new PrintWriter(writer));
        String origStackTrace = writer.toString();
        Evaluator e = Context.createInterpreter();
        if (e != null) return e.getPatchedStack(this, origStackTrace);
        return null;
    }

    /**
     * Get a string representing the script stack of this exception. If optimization is enabled,
     * this includes java stack elements whose source and method names suggest they have been
     * generated by the Rhino script compiler.
     *
     * @return a script stack dump
     * @since 1.6R6
     */
    public String getScriptStackTrace() {
        return getScriptStackTrace(NativeError.DEFAULT_STACK_LIMIT, null);
    }

    /**
     * Get a string representing the script stack of this exception. If optimization is enabled,
     * this includes java stack elements whose source and method names suggest they have been
     * generated by the Rhino script compiler. The optional "limit" parameter limits the number of
     * stack frames returned. The "functionName" parameter will exclude any stack frames "below" the
     * specified function on the stack.
     *
     * @param limit the number of stack frames returned
     * @param functionName the name of a function on the stack -- frames below it will be ignored
     * @return a script stack dump
     * @since 1.8.0
     */
    public String getScriptStackTrace(int limit, String functionName) {
        ScriptStackElement[] stack = getScriptStack(limit, functionName);
        return formatStackTrace(stack, details());
    }

    static String formatStackTrace(ScriptStackElement[] stack, String message) {
        StringBuilder buffer = new StringBuilder();
        String lineSeparator = SecurityUtilities.getSystemProperty("line.separator");

        if ((stackStyle == StackStyle.V8) && !"null".equals(message)) {
            // V8 Actually puts the error message at the top of "stack."
            buffer.append(message);
            buffer.append(lineSeparator);
        }

        for (ScriptStackElement elem : stack) {
            switch (stackStyle) {
                case MOZILLA:
                    elem.renderMozillaStyle(buffer);
                    buffer.append(lineSeparator);
                    break;
                case MOZILLA_LF:
                    elem.renderMozillaStyle(buffer);
                    buffer.append('\n');
                    break;
                case V8:
                    elem.renderV8Style(buffer);
                    buffer.append(lineSeparator);
                    break;
                case RHINO:
                    elem.renderJavaStyle(buffer);
                    buffer.append(lineSeparator);
                    break;
            }
        }
        return buffer.toString();
    }

    /**
     * Get a string representing the script stack of this exception.
     *
     * @param filter ignored
     * @return a script stack dump
     * @since 1.6R6
     * @deprecated the filter argument is ignored as we are able to recognize script stack elements
     *     by our own. Use #getScriptStackTrace() instead.
     */
    @Deprecated
    public String getScriptStackTrace(FilenameFilter filter) {
        return getScriptStackTrace();
    }

    /**
     * Get the script stack of this exception as an array of {@link ScriptStackElement}s. If
     * optimization is enabled, this includes java stack elements whose source and method names
     * suggest they have been generated by the Rhino script compiler.
     *
     * @return the script stack for this exception
     * @since 1.7R3
     */
    public ScriptStackElement[] getScriptStack() {
        return getScriptStack(-1, null);
    }

    /**
     * Get the script stack of this exception as an array of {@link ScriptStackElement}s. If
     * optimization is enabled, this includes java stack elements whose source and method names
     * suggest they have been generated by the Rhino script compiler.
     *
     * @param limit the number of stack frames returned, or -1 for unlimited
     * @param hideFunction the name of a function on the stack -- frames below it will be ignored,
     *     or null
     * @return the script stack for this exception
     * @since 1.8.0
     */
    public ScriptStackElement[] getScriptStack(int limit, String hideFunction) {
        List<ScriptStackElement> list = new ArrayList<>();
        ScriptStackElement[][] interpreterStack = null;
        if (interpreterStackInfo != null) {
            Evaluator interpreter = Context.createInterpreter();
            if (interpreter instanceof Interpreter)
                interpreterStack = ((Interpreter) interpreter).getScriptStackElements(this);
        }

        int interpreterStackIndex = 0;
        StackTraceElement[] stack = getStackTrace();
        int count = 0;
        boolean printStarted = (hideFunction == null);

        // Pattern to recover function name from java method name -
        // see Codegen.getBodyMethodName()
        // kudos to Marc Guillemot for coming up with this
        for (StackTraceElement e : stack) {
            String fileName = e.getFileName();
            if (e.getMethodName().startsWith("_c_")
                    && e.getLineNumber() > -1
                    && (fileName == null || !fileName.endsWith(".java"))) {
                String methodName = e.getMethodName();
                Matcher match = JAVA_STACK_PATTERN.matcher(methodName);
                // the method representing the main script is always "_c_script_0" -
                // at least we hope so
                methodName =
                        !"_c_script_0".equals(methodName) && match.find() ? match.group(1) : null;

                if (!printStarted && hideFunction.equals(methodName)) {
                    printStarted = true;
                } else if (printStarted && ((limit < 0) || (count < limit))) {
                    String fn = fileName == null ? "(unknown)" : fileName;
                    list.add(new ScriptStackElement(fn, methodName, e.getLineNumber()));
                    count++;
                }

            } else if ("org.mozilla.javascript.Interpreter".equals(e.getClassName())
                    && "interpretLoop".equals(e.getMethodName())
                    && interpreterStack != null
                    && interpreterStack.length > interpreterStackIndex) {

                for (ScriptStackElement elem : interpreterStack[interpreterStackIndex++]) {
                    if (!printStarted && hideFunction.equals(elem.functionName)) {
                        printStarted = true;
                    } else if (printStarted && ((limit < 0) || (count < limit))) {
                        list.add(elem);
                        count++;
                    }
                }
            }
        }
        return list.toArray(new ScriptStackElement[0]);
    }

    @Override
    public void printStackTrace(PrintWriter s) {
        if (interpreterStackInfo == null) {
            super.printStackTrace(s);
        } else {
            s.print(generateStackTrace());
        }
    }

    @Override
    public void printStackTrace(PrintStream s) {
        if (interpreterStackInfo == null) {
            super.printStackTrace(s);
        } else {
            s.print(generateStackTrace());
        }
    }

    /**
     * Returns true if subclasses of <code>RhinoException</code> use the Mozilla/Firefox style of
     * rendering script stacks (<code>functionName()@fileName:lineNumber</code>) instead of Rhino's
     * own Java-inspired format (<code>    at fileName:lineNumber (functionName)</code>).
     *
     * @return true if stack is rendered in Mozilla/Firefox style
     * @see ScriptStackElement
     * @since 1.7R3
     */
    public static boolean usesMozillaStackStyle() {
        return (stackStyle == StackStyle.MOZILLA);
    }

    /**
     * Tell subclasses of <code>RhinoException</code> whether to use the Mozilla/Firefox style of
     * rendering script stacks (<code>functionName()@fileName:lineNumber</code>) instead of Rhino's
     * own Java-inspired format (<code>    at fileName:lineNumber (functionName)</code>). Use
     * "setStackStyle" to select between more than just the "Mozilla" and "Rhino" formats.
     *
     * @param flag whether to render stacks in Mozilla/Firefox style
     * @see ScriptStackElement
     * @since 1.7R3
     */
    public static void useMozillaStackStyle(boolean flag) {
        stackStyle = (flag ? StackStyle.MOZILLA : StackStyle.RHINO);
    }

    /**
     * Specify the stack style to use from between three different formats: "Rhino" (the default),
     * "Mozilla", and "V8." See StackStyle for information about each.
     *
     * @param style the style to select -- an instance of the StackStyle class
     * @see StackStyle
     * @since 1.8.0
     */
    public static void setStackStyle(StackStyle style) {
        stackStyle = style;
    }

    /** Return the current stack style in use. Return the current stack style in use. */
    public static StackStyle getStackStyle() {
        return stackStyle;
    }

    private static final long serialVersionUID = 1883500631321581169L;

    // Just for testing!
    private static StackStyle stackStyle = RhinoConfig.get("rhino.stack.style", StackStyle.RHINO);

    private String sourceName;
    private int lineNumber;
    private String lineSource;
    private int columnNumber;

    Object interpreterStackInfo;
    int[] interpreterLineData;
}
