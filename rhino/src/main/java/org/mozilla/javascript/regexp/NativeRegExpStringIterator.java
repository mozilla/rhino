/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.regexp;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ES6Iterator;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

// See ECMAScript spec 22.2.9.1
public final class NativeRegExpStringIterator extends ES6Iterator {
    private static final long serialVersionUID = 1L;
    private static final String ITERATOR_TAG = "RegExpStringIterator";

    private Scriptable regexp;
    private String string;
    private boolean global;
    private boolean fullUnicode;
    private boolean nextDone;
    private Object next = null;

    public static void init(ScriptableObject scope, boolean sealed) {
        ES6Iterator.init(scope, sealed, new NativeRegExpStringIterator(), ITERATOR_TAG);
    }

    /** Only for constructing the prototype object. */
    private NativeRegExpStringIterator() {
        super();
    }

    public NativeRegExpStringIterator(
            Scriptable scope,
            Scriptable regexp,
            String string,
            boolean global,
            boolean fullUnicode) {
        super(scope, ITERATOR_TAG);

        this.regexp = regexp;
        this.string = string;
        this.global = global;
        this.fullUnicode = fullUnicode;
        this.nextDone = false;
    }

    @Override
    public String getClassName() {
        return "RegExp String Iterator";
    }

    @Override
    protected boolean isDone(Context cx, Scriptable scope) {
        // The base class calls _first_ isDone and _then_ nextValue, so we'll just compute the next
        // value here and return it form "nextValue".
        // Also, for non-global regexp, we need to return the first match and then "done" on the
        // next iteration.

        if (nextDone) {
            return true;
        }

        next = NativeRegExp.regExpExec(regexp, string, cx, scope);
        if (next == null) {
            // Done! Point ii of the spec
            next = Undefined.instance;
            nextDone = true;
            return true;
        } else if (!global) {
            // Return false at this iteration, but true at the next. Point iii of the spec
            nextDone = true;
            return false;
        }

        // Increment index if matched empty string, as per the spec, point v.
        String matchStr = ScriptRuntime.toString(ScriptRuntime.getObjectIndex(next, 0, cx, scope));
        if (matchStr.isEmpty()) {
            long thisIndex =
                    ScriptRuntime.toLength(ScriptRuntime.getObjectProp(regexp, "lastIndex", cx));
            long nextIndex = ScriptRuntime.advanceStringIndex(string, thisIndex, fullUnicode);
            ScriptRuntime.setObjectProp(regexp, "lastIndex", nextIndex, cx);
        }

        return false;
    }

    @Override
    protected Object nextValue(Context cx, Scriptable scope) {
        return next;
    }

    @Override
    protected String getTag() {
        return ITERATOR_TAG;
    }
}
