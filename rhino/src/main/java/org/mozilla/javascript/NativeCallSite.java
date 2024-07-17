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
public class NativeCallSite extends IdScriptableObject {
    private static final long serialVersionUID = 2688372752566593594L;
    private static final String CALLSITE_TAG = "CallSite";
    private ScriptStackElement element;

    static void init(Scriptable scope, boolean sealed) {
        NativeCallSite cs = new NativeCallSite();
        cs.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
    }

    static NativeCallSite make(Scriptable scope, Scriptable ctorObj) {
        NativeCallSite cs = new NativeCallSite();
        Scriptable proto = (Scriptable) ctorObj.get("prototype", ctorObj);
        cs.setParentScope(scope);
        cs.setPrototype(proto);
        return cs;
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
    protected void initPrototypeId(int id) {
        String s;
        int arity;
        switch (id) {
            case Id_constructor:
                arity = 0;
                s = "constructor";
                break;
            case Id_getThis:
                arity = 0;
                s = "getThis";
                break;
            case Id_getTypeName:
                arity = 0;
                s = "getTypeName";
                break;
            case Id_getFunction:
                arity = 0;
                s = "getFunction";
                break;
            case Id_getFunctionName:
                arity = 0;
                s = "getFunctionName";
                break;
            case Id_getMethodName:
                arity = 0;
                s = "getMethodName";
                break;
            case Id_getFileName:
                arity = 0;
                s = "getFileName";
                break;
            case Id_getLineNumber:
                arity = 0;
                s = "getLineNumber";
                break;
            case Id_getColumnNumber:
                arity = 0;
                s = "getColumnNumber";
                break;
            case Id_getEvalOrigin:
                arity = 0;
                s = "getEvalOrigin";
                break;
            case Id_isToplevel:
                arity = 0;
                s = "isToplevel";
                break;
            case Id_isEval:
                arity = 0;
                s = "isEval";
                break;
            case Id_isNative:
                arity = 0;
                s = "isNative";
                break;
            case Id_isConstructor:
                arity = 0;
                s = "isConstructor";
                break;
            case Id_toString:
                arity = 0;
                s = "toString";
                break;
            default:
                throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(CALLSITE_TAG, id, s, arity);
    }

    @Override
    public Object execIdCall(
            IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (!f.hasTag(CALLSITE_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();
        switch (id) {
            case Id_constructor:
                return make(scope, f);
            case Id_getFunctionName:
                return getFunctionName(thisObj);
            case Id_getFileName:
                return getFileName(thisObj);
            case Id_getLineNumber:
                return getLineNumber(thisObj);
            case Id_getThis:
            case Id_getTypeName:
            case Id_getFunction:
            case Id_getColumnNumber:
                return Undefined.instance;
            case Id_getMethodName:
                return null;
            case Id_getEvalOrigin:
            case Id_isEval:
            case Id_isConstructor:
            case Id_isNative:
            case Id_isToplevel:
                return Boolean.FALSE;
            case Id_toString:
                return js_toString(thisObj);
            default:
                throw new IllegalArgumentException(String.valueOf(id));
        }
    }

    @Override
    public String toString() {
        if (element == null) {
            return "";
        }
        return element.toString();
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

    @Override
    protected int findPrototypeId(String s) {
        int id;
        switch (s) {
            case "constructor":
                id = Id_constructor;
                break;
            case "getThis":
                id = Id_getThis;
                break;
            case "getTypeName":
                id = Id_getTypeName;
                break;
            case "getFunction":
                id = Id_getFunction;
                break;
            case "getFunctionName":
                id = Id_getFunctionName;
                break;
            case "getMethodName":
                id = Id_getMethodName;
                break;
            case "getFileName":
                id = Id_getFileName;
                break;
            case "getLineNumber":
                id = Id_getLineNumber;
                break;
            case "getColumnNumber":
                id = Id_getColumnNumber;
                break;
            case "getEvalOrigin":
                id = Id_getEvalOrigin;
                break;
            case "isToplevel":
                id = Id_isToplevel;
                break;
            case "isEval":
                id = Id_isEval;
                break;
            case "isNative":
                id = Id_isNative;
                break;
            case "isConstructor":
                id = Id_isConstructor;
                break;
            case "toString":
                id = Id_toString;
                break;
            default:
                id = 0;
                break;
        }
        return id;
    }

    private static final int Id_constructor = 1,
            Id_getThis = 2,
            Id_getTypeName = 3,
            Id_getFunction = 4,
            Id_getFunctionName = 5,
            Id_getMethodName = 6,
            Id_getFileName = 7,
            Id_getLineNumber = 8,
            Id_getColumnNumber = 9,
            Id_getEvalOrigin = 10,
            Id_isToplevel = 11,
            Id_isEval = 12,
            Id_isNative = 13,
            Id_isConstructor = 14,
            Id_toString = 15,
            MAX_PROTOTYPE_ID = 15;
}
