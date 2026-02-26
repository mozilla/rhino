/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static org.mozilla.javascript.ClassDescriptor.Destination.PROTO;

/**
 * This class is used by the V8 extension "Error.prepareStackTrace." It is passed to that function,
 * which may then use it to format the stack as it sees fit.
 */
public class NativeCallSite extends ScriptableObject {
    private static final long serialVersionUID = 2688372752566593594L;

    private ScriptStackElement element;

    private static final ClassDescriptor DESCRIPTOR;

    static {
        DESCRIPTOR =
                new ClassDescriptor.Builder(
                                "CallSite",
                                0,
                                NativeCallSite::js_constructor,
                                NativeCallSite::js_constructor)
                        .withMethod(PROTO, "getThis", 0, NativeCallSite::js_getThis)
                        .withMethod(PROTO, "getTypeName", 0, NativeCallSite::js_getTypeName)
                        .withMethod(PROTO, "getFunction", 0, NativeCallSite::js_getFunction)
                        .withMethod(PROTO, "getFunctionName", 0, NativeCallSite::js_getFunctionName)
                        .withMethod(PROTO, "getMethodName", 0, NativeCallSite::js_getMethodName)
                        .withMethod(PROTO, "getFileName", 0, NativeCallSite::js_getFileName)
                        .withMethod(PROTO, "getLineNumber", 0, NativeCallSite::js_getLineNumber)
                        .withMethod(PROTO, "getColumnNumber", 0, NativeCallSite::js_getColumnNumber)
                        .withMethod(PROTO, "getEvalOrigin", 0, NativeCallSite::js_getEvalOrigin)
                        .withMethod(PROTO, "isToplevel", 0, NativeCallSite::js_isToplevel)
                        .withMethod(PROTO, "isEval", 0, NativeCallSite::js_isEval)
                        .withMethod(PROTO, "isNative", 0, NativeCallSite::js_isNative)
                        .withMethod(PROTO, "isConstructor", 0, NativeCallSite::js_isConstructor)
                        .withMethod(PROTO, "toString", 0, NativeCallSite::js_toString)
                        .build();
    }

    static void init(Context cx, VarScope scope, boolean sealed) {
        DESCRIPTOR.buildConstructor(cx, scope, new NativeCallSite(), sealed);
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

    private static Object js_constructor(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        var res = new NativeCallSite();
        res.setPrototype((Scriptable) f.getPrototypeProperty());
        res.setParentScope(s);
        return res;
    }

    private static Object js_getThis(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return Undefined.instance;
    }

    private static Object js_getTypeName(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return Undefined.instance;
    }

    private static Object js_getFunction(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return Undefined.instance;
    }

    private static Object js_getFunctionName(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return getFunctionName((Scriptable) thisObj);
    }

    private static Object js_getMethodName(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return null;
    }

    private static Object js_getFileName(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return getFileName((Scriptable) thisObj);
    }

    private static Object js_getLineNumber(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return getLineNumber((Scriptable) thisObj);
    }

    private static Object js_getColumnNumber(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return Undefined.instance;
    }

    private static Object js_getEvalOrigin(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return Boolean.FALSE;
    }

    private static Object js_isToplevel(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return Boolean.FALSE;
    }

    private static Object js_isEval(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return Boolean.FALSE;
    }

    private static Object js_isNative(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return Boolean.FALSE;
    }

    private static Object js_isConstructor(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return Boolean.FALSE;
    }

    private static Object js_toString(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return js_toString((Scriptable) thisObj);
    }

    private static Object js_toString(Scriptable obj) {
        while (obj != null && !(obj instanceof NativeCallSite)) {
            obj = obj.getPrototype();
        }
        if (obj == null) {
            return NOT_FOUND;
        }
        NativeCallSite cs = (NativeCallSite) obj;
        StringBuilder sb = new StringBuilder();
        cs.element.renderJavaStyle(sb);
        return sb.toString();
    }

    private static Object getFunctionName(Scriptable obj) {
        while (obj != null && !(obj instanceof NativeCallSite)) {
            obj = obj.getPrototype();
        }
        if (obj == null) {
            return NOT_FOUND;
        }
        NativeCallSite cs = (NativeCallSite) obj;
        return (cs.element == null ? null : cs.element.functionName);
    }

    private static Object getFileName(Scriptable obj) {
        while (obj != null && !(obj instanceof NativeCallSite)) {
            obj = obj.getPrototype();
        }
        if (obj == null) {
            return NOT_FOUND;
        }
        NativeCallSite cs = (NativeCallSite) obj;
        return (cs.element == null ? null : cs.element.fileName);
    }

    private static Object getLineNumber(Scriptable obj) {
        while (obj != null && !(obj instanceof NativeCallSite)) {
            obj = obj.getPrototype();
        }
        if (obj == null) {
            return NOT_FOUND;
        }
        NativeCallSite cs = (NativeCallSite) obj;
        if ((cs.element == null) || (cs.element.lineNumber < 0)) {
            return Undefined.instance;
        }
        return Integer.valueOf(cs.element.lineNumber);
    }
}
