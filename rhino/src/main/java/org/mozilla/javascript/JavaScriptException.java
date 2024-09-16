/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// API class

package org.mozilla.javascript;

/**
 * Java reflection of JavaScript exceptions. Instances of this class are thrown by the JavaScript
 * 'throw' keyword.
 *
 * @author Mike McCabe
 */
public class JavaScriptException extends RhinoException {
    private static final long serialVersionUID = -7666130513694669293L;

    /**
     * @deprecated Use {@link WrappedException#WrappedException(Throwable)} to report exceptions in
     *     Java code.
     */
    @Deprecated
    public JavaScriptException(Object value) {
        this(value, "", 0);
    }

    /**
     * Create a JavaScript exception wrapping the given JavaScript value
     *
     * @param value the JavaScript value thrown.
     */
    public JavaScriptException(Object value, String sourceName, int lineNumber) {
        recordErrorOrigin(sourceName, lineNumber, null, 0);
        this.value = value;
        // try to extract the cause. Value can be either a (wrapped) java.lang.Throwable
        // or a NativeError, that may contain the causing javaException
        Object javaCause = value;
        if (value instanceof NativeError) {
            NativeError error = (NativeError) value;
            javaCause = error.get("javaException", error);
            // Fill in fileName and lineNumber automatically when not specified
            // explicitly, see Bugzilla issue #342807
            if (Context.getContext().hasFeature(Context.FEATURE_LOCATION_INFORMATION_IN_ERROR)) {
                if (!error.has("fileName", error)) {
                    error.put("fileName", error, sourceName);
                }
                if (!error.has("lineNumber", error)) {
                    error.put("lineNumber", error, Integer.valueOf(lineNumber));
                }
                // set stack property, see bug #549604
                error.setStackProvider(this);
            }
        }
        if (javaCause instanceof Wrapper) {
            javaCause = ((Wrapper) javaCause).unwrap();
        }
        if (javaCause instanceof Throwable) {
            this.initCause((Throwable) javaCause);
        }

        // generate details string when exception is first created,
        // since details() may be called later from a different thread
        // (e.g. when printing failed test results), which
        // would cause ScriptRuntime.toString to fail.
        this.details = getDetails();
    }

    @Override
    public String details() {
        return this.details;
    }

    public String getDetails() {
        if (value == null) {
            return "null";
        } else if (value instanceof NativeError) {
            return value.toString();
        }

        try {
            return ScriptRuntime.toString(value);
        } catch (RuntimeException rte) {
            // ScriptRuntime.toString may throw a RuntimeException
            if (value instanceof Scriptable) {
                return ScriptRuntime.defaultObjectToString((Scriptable) value);
            }
            return value.toString();
        }
    }

    /**
     * @return the value wrapped by this exception
     */
    public Object getValue() {
        return value;
    }

    /**
     * @deprecated Use {@link RhinoException#sourceName()} from the super class.
     */
    @Deprecated
    public String getSourceName() {
        return sourceName();
    }

    /**
     * @deprecated Use {@link RhinoException#lineNumber()} from the super class.
     */
    @Deprecated
    public int getLineNumber() {
        return lineNumber();
    }

    private Object value;
    private String details;
}
