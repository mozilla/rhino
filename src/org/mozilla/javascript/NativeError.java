/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.io.Serializable;

/**
 * The class of error objects
 *
 * <p>ECMA 15.11
 */
final class NativeError extends ScriptableObject {
    private static final long serialVersionUID = -5338413581437645187L;

    /** Default stack limit is set to "Infinity", here represented as a negative int */
    public static final int DEFAULT_STACK_LIMIT = -1;

    private RhinoException stackProvider;
    private Object stack;

    static void init(Scriptable scope, boolean sealed) {
        // This object will hang around due to various lambdas
        final ProtoProps pprops = new ProtoProps();

        LambdaConstructor constructor =
                new LambdaConstructor(
                        scope,
                        "Error",
                        1,
                        (Context cx, Scriptable s, Object[] args) -> {
                            NativeError err = new NativeError();
                            err.initialize(args, pprops);
                            return err;
                        });

        constructor.defineConstructorMethod(
                scope,
                "captureStackTrace",
                2,
                (Context cx, Scriptable s, Scriptable thisObj, Object[] args) ->
                        js_captureStackTrace(cx, thisObj, args, pprops));

        constructor.definePrototypeProperty("message", "", DONTENUM);
        constructor.definePrototypeProperty("name", "Error", DONTENUM);

        constructor.definePrototypeMethod(
                scope,
                "toString",
                0,
                DONTENUM,
                (Context cx, Scriptable s, Scriptable thisObj, Object[] args) ->
                        js_toString(thisObj));
        constructor.definePrototypeMethod(scope, "toSource", 0, DONTENUM, NativeError::js_toSource);

        constructor.defineOwnProperty(
                "stackTraceLimit", pprops::getStackTraceLimit, pprops::setStackTraceLimit, 0);
        constructor.defineOwnProperty(
                "prepareStackTrace", pprops::getPrepareStackTrace, pprops::setPrepareStackTrace, 0);

        ScriptableObject.defineProperty(scope, "Error", constructor, DONTENUM);
        NativeCallSite.init(scope, sealed);
    }

    void initialize(Object[] args, ProtoProps pprops) {
        String fileName = "";
        int lineNum = 0;
        if (args.length >= 1 && !Undefined.isUndefined(args[0])) {
            defineProperty("message", ScriptRuntime.toString(args[0]), DONTENUM);
        }
        if (args.length >= 2) {
            fileName = ScriptRuntime.toString(args[1]);
        }
        if (args.length >= 3) {
            lineNum = ScriptRuntime.toInt32(args[2]);
        }

        defineOwnProperty("stack", () -> getStack(pprops), this::setStack, DONTENUM);
        defineProperty("fileName", fileName, DONTENUM);
        defineProperty("lineNumber", lineNum, DONTENUM);
    }

    // Construct a new error with the specified constructor as the prototype.
    // This is used to construct the native error objects.
    static NativeError make(Context cx, Scriptable scope, Scriptable ctorObj, Object[] args) {
        Scriptable proto = (Scriptable) (ctorObj.get("prototype", ctorObj));
        NativeError obj = new NativeError();
        obj.setPrototype(proto);
        obj.setParentScope(scope);
        obj.initialize(args, null);
        Object protoName = ScriptableObject.getProperty(proto, "name");
        if (protoName != null && !Undefined.isUndefined(protoName)) {
            ScriptableObject.putProperty(obj, "name", protoName);
        }
        return obj;
    }

    @Override
    public String getClassName() {
        return "Error";
    }

    @Override
    public String toString() {
        // According to spec, Error.prototype.toString() may return undefined.
        Object toString = js_toString(this);
        return toString instanceof String ? (String) toString : super.toString();
    }

    public void setStackProvider(RhinoException re) {
        // We go some extra miles to make sure the stack property is only
        // generated on demand, is cached after the first access, and is
        // overwritable like an ordinary property. Hence this setup with
        // the getter and setter below.
        if (stackProvider == null) {
            stackProvider = re;
        }
    }

    public Object getStack(ProtoProps pprops) {
        if (stack != null) {
            // Cached response
            return stack;
        }
        createStack(pprops, null);
        return stack;
    }

    public void setStack(Object value) {
        stack = value;
        stackProvider = null;
    }

    private void createStack(ProtoProps pprops, String hideFunc) {
        if (stackProvider == null) {
            stack = Undefined.instance;
            return;
        }

        int stackLimit = pprops == null ? DEFAULT_STACK_LIMIT : pprops.getInternalStackTraceLimit();

        ScriptStackElement[] scriptStack = stackProvider.getScriptStack(stackLimit, hideFunc);

        // Determine whether to format the stack trace ourselves, or call the user's code to do it
        Object value;
        Function prepareFunc = pprops == null ? null : pprops.getInternalPrepareStackTrace();
        if (prepareFunc == null) {
            value = RhinoException.formatStackTrace(scriptStack, stackProvider.details());
        } else {
            value = callPrepareStack(prepareFunc, scriptStack);
        }

        stack = value;
        stackProvider = null;
    }

    private Object callPrepareStack(Function prepare, ScriptStackElement[] stack) {
        Context cx = Context.getCurrentContext();
        Object[] elts = new Object[stack.length];

        // The "prepareStackTrace" function takes an array of CallSite objects.
        for (int i = 0; i < stack.length; i++) {
            NativeCallSite site = (NativeCallSite) cx.newObject(this, "CallSite");
            site.setElement(stack[i]);
            elts[i] = site;
        }

        Scriptable eltArray = cx.newArray(this, elts);
        return prepare.call(cx, prepare, this, new Object[] {this, eltArray});
    }

    private static Object js_toString(Scriptable thisObj) {
        Object name = ScriptableObject.getProperty(thisObj, "name");
        if (name == NOT_FOUND || name == Undefined.instance) {
            name = "Error";
        } else {
            name = ScriptRuntime.toString(name);
        }
        Object msg = ScriptableObject.getProperty(thisObj, "message");
        if (msg == NOT_FOUND || Undefined.isUndefined(msg)) {
            msg = "";
        } else {
            msg = ScriptRuntime.toString(msg);
        }
        if (name.toString().length() == 0) {
            return msg;
        } else if (msg.toString().length() == 0) {
            return name;
        } else {
            return name + ": " + msg;
        }
    }

    private static String js_toSource(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        // Emulation of SpiderMonkey behavior
        Object name = ScriptableObject.getProperty(thisObj, "name");
        Object message = ScriptableObject.getProperty(thisObj, "message");
        Object fileName = ScriptableObject.getProperty(thisObj, "fileName");
        Object lineNumber = ScriptableObject.getProperty(thisObj, "lineNumber");

        StringBuilder sb = new StringBuilder();
        sb.append("(new ");
        if (name == NOT_FOUND) {
            name = Undefined.instance;
        }
        sb.append(ScriptRuntime.toString(name));
        sb.append("(");
        if (message != NOT_FOUND || fileName != NOT_FOUND || lineNumber != NOT_FOUND) {
            if (message == NOT_FOUND) {
                message = "";
            }
            sb.append(ScriptRuntime.uneval(cx, scope, message));
            if (fileName != NOT_FOUND || lineNumber != NOT_FOUND) {
                sb.append(", ");
                if (fileName == NOT_FOUND) {
                    fileName = "";
                }
                sb.append(ScriptRuntime.uneval(cx, scope, fileName));
                if (lineNumber != NOT_FOUND) {
                    int line = ScriptRuntime.toInt32(lineNumber);
                    if (line != 0) {
                        sb.append(", ");
                        sb.append(ScriptRuntime.toString(line));
                    }
                }
            }
        }
        sb.append("))");
        return sb.toString();
    }

    // Implementation of Error.captureStackTrace, which puts the current stack trace
    // on a "stack" property on the first object supplied.
    private static Object js_captureStackTrace(
            Context cx, Scriptable thisObj, Object[] args, ProtoProps pprops) {
        ScriptableObject obj =
                (ScriptableObject) ScriptRuntime.toObjectOrNull(cx, args[0], thisObj);
        Function func = null;
        if (args.length > 1) {
            func = (Function) ScriptRuntime.toObjectOrNull(cx, args[1], thisObj);
        }

        // Create a new error that will have the correct prototype so we can re-use "getStackTrace"
        NativeError err = (NativeError) cx.newObject(thisObj, "Error");
        // Wire it up so that it will have an actual exception with a stack trace
        err.setStackProvider(new EvaluatorException("[object Object]"));

        // Figure out if they passed a function used to hide part of the stack
        String hideFunc = null;
        if (func != null) {
            Object funcName = func.get("name", func);
            if ((funcName != null) && !Undefined.instance.equals(funcName)) {
                hideFunc = Context.toString(funcName);
            }
        }

        // from https://v8.dev/docs/stack-trace-api
        // Error.captureStackTrace(error, constructorOpt)
        // adds a stack property to the given error object that yields the stack trace
        // at the time captureStackTrace was called. Stack traces collected through
        // Error.captureStackTrace are immediately collected, formatted,
        // and attached to the given error object.
        err.createStack(pprops, hideFunc);
        ScriptableObject.defineProperty(obj, "stack", err.stack, DONTENUM);
        return Undefined.instance;
    }

    /** These properties are used to maintain state on behalf of the constructor functions. */
    private static final class ProtoProps implements Serializable {
        private static final long serialVersionUID = 1907180507775337939L;

        private int stackTraceLimit = DEFAULT_STACK_LIMIT;
        private Function prepareStackTrace;

        public Object getStackTraceLimit() {
            if (stackTraceLimit >= 0) {
                return stackTraceLimit;
            }
            return Double.POSITIVE_INFINITY;
        }

        int getInternalStackTraceLimit() {
            return stackTraceLimit;
        }

        public void setStackTraceLimit(Object value) {
            double limit = Context.toNumber(value);
            if (Double.isNaN(limit) || Double.isInfinite(limit)) {
                stackTraceLimit = -1;
            } else {
                stackTraceLimit = (int) limit;
            }
        }

        public Object getPrepareStackTrace() {
            return (prepareStackTrace == null ? Undefined.instance : prepareStackTrace);
        }

        Function getInternalPrepareStackTrace() {
            return prepareStackTrace;
        }

        public void setPrepareStackTrace(Object value) {
            if ((value == null) || Undefined.isUndefined(value)) {
                prepareStackTrace = null;
            } else if (value instanceof Function) {
                prepareStackTrace = (Function) value;
            }
        }
    }
}
