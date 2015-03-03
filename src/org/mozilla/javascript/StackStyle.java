/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * This class is an old-style (before enums were supported) way to support three and only three
 * stack trace formats in the product.
 */

public class StackStyle
{
    private static final int FORMAT_RHINO = 0;
    private static final int FORMAT_MOZILLA = 1;
    private static final int FORMAT_V8 = 2;

    /**
     * This is the default stack trace style in Rhino, which is like Java:
     * <code>    at fileName:lineNumber (functionName)</code>
     */
    public static final StackStyle RHINO = new StackStyle(FORMAT_RHINO);

    /**
     * This stack trace style comes from the old Mozilla code:
     * <code>functionName()@fileName:lineNumber</code>
     */
    public static final StackStyle MOZILLA = new StackStyle(FORMAT_MOZILLA);

    /**
     * This stack trace style matches that output from V8, either:
     * <code>    at functionName (fileName:lineNumber:columnNumber)</code>
     * or, for anonymous functions:
     * <code>    at fileName:lineNumber:columnNumber</code>
     */
    public static final StackStyle V8 = new StackStyle(FORMAT_V8);

    private final int format;

    private StackStyle(int fmt)
    {
        this.format = fmt;
    }

    public static StackStyle getStyle(String name)
    {
        if ("Mozilla".equalsIgnoreCase(name)) {
            return StackStyle.MOZILLA;
        } else if ("V8".equalsIgnoreCase(name)) {
            return StackStyle.V8;
        } else {
            return StackStyle.RHINO;
        }
    }

    public String toString()
    {
        switch (format) {
        case FORMAT_RHINO:
            return "Rhino";
        case FORMAT_MOZILLA:
            return "Mozilla";
        case FORMAT_V8:
            return "V8";
        default:
            return "(undefined)";
        }
    }

    public boolean equals(Object o)
    {
        try {
            StackStyle f = (StackStyle)o;
            return (f.format == format);
        } catch (ClassCastException cce) {
            return false;
        }
    }

    public int hashCode()
    {
        return format;
    }
}