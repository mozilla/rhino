/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * This class is used by the V8 extension "Error.prepareStackTrace." It is passed to that function,
 * which may then use it to format the stack as it sees fit.
 */
public class NativeCallSite extends ScriptableObject {
    private static final long serialVersionUID = 2688372752566593594L;

    private ScriptStackElement element;

    static void init(Scriptable scope, boolean sealed) {
        LambdaConstructor constructor =
                new LambdaConstructor(
                        scope,
                        "CallSite",
                        0,
                        (Context cx, Scriptable s, Object[] args) -> new NativeCallSite());

        constructor.definePrototypeMethod(
                scope, "getFunctionName", 0, DONTENUM, NativeCallSite::getFunctionName);
        constructor.definePrototypeMethod(
                scope, "getFileName", 0, DONTENUM, NativeCallSite::getFileName);
        constructor.definePrototypeMethod(
                scope, "getLineNumber", 0, DONTENUM, NativeCallSite::getLineNumber);
        constructor.definePrototypeMethod(
                scope, "toString", 0, DONTENUM, NativeCallSite::js_toString);
        constructor.definePrototypeMethod(
                scope, "getThis", 0, DONTENUM, NativeCallSite::getUndefined);
        constructor.definePrototypeMethod(
                scope, "getTypeName", 0, DONTENUM, NativeCallSite::getUndefined);
        constructor.definePrototypeMethod(
                scope, "getFunction", 0, DONTENUM, NativeCallSite::getUndefined);
        constructor.definePrototypeMethod(
                scope, "getColumnNumber", 0, DONTENUM, NativeCallSite::getUndefined);
        constructor.definePrototypeMethod(
                scope, "getMethodName", 0, DONTENUM, NativeCallSite::getNull);
        constructor.definePrototypeMethod(
                scope, "getEvalOrigin", 0, DONTENUM, NativeCallSite::getFalse);
        constructor.definePrototypeMethod(scope, "isEval", 0, DONTENUM, NativeCallSite::getFalse);
        constructor.definePrototypeMethod(
                scope, "isConstructor", 0, DONTENUM, NativeCallSite::getFalse);
        constructor.definePrototypeMethod(scope, "isNative", 0, DONTENUM, NativeCallSite::getFalse);
        constructor.definePrototypeMethod(
                scope, "isToplevel", 0, DONTENUM, NativeCallSite::getFalse);

        ScriptableObject.defineProperty(scope, "CallSite", constructor, DONTENUM);
        if (sealed) {
            constructor.sealObject();
        }
    }

    private NativeCallSite() {}

    void setElement(ScriptStackElement elt) {
        this.element = elt;
    }

    @Override
    public String getClassName() {
        return "CallSite";
    }

    @Override
    public String toString() {
        if (element == null) {
            return "";
        }
        return element.toString();
    }

    private static Object js_toString(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        while (thisObj != null && !(thisObj instanceof NativeCallSite)) {
            thisObj = thisObj.getPrototype();
        }
        if (thisObj == null) {
            return NOT_FOUND;
        }
        NativeCallSite cs = (NativeCallSite) thisObj;
        StringBuilder sb = new StringBuilder();
        cs.element.renderJavaStyle(sb);
        return sb.toString();
    }

    private static Object getFunctionName(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        while (thisObj != null && !(thisObj instanceof NativeCallSite)) {
            thisObj = thisObj.getPrototype();
        }
        if (thisObj == null) {
            return NOT_FOUND;
        }
        NativeCallSite cs = (NativeCallSite) thisObj;
        return (cs.element == null ? null : cs.element.functionName);
    }

    private static Object getFileName(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        while (thisObj != null && !(thisObj instanceof NativeCallSite)) {
            thisObj = thisObj.getPrototype();
        }
        if (thisObj == null) {
            return NOT_FOUND;
        }
        NativeCallSite cs = (NativeCallSite) thisObj;
        return (cs.element == null ? null : cs.element.fileName);
    }

    private static Object getLineNumber(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        while (thisObj != null && !(thisObj instanceof NativeCallSite)) {
            thisObj = thisObj.getPrototype();
        }
        if (thisObj == null) {
            return NOT_FOUND;
        }
        NativeCallSite cs = (NativeCallSite) thisObj;
        if ((cs.element == null) || (cs.element.lineNumber < 0)) {
            return Undefined.instance;
        }
        return cs.element.lineNumber;
    }

    private static Object getNull(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return null;
    }

    private static Object getUndefined(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return Undefined.instance;
    }

    private static Object getFalse(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return false;
    }
}
