/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 *
 * The class of error objects
 *
 *  ECMA 15.11
 */
final class NativeError extends IdScriptableObject
{
    private static final long serialVersionUID = -5338413581437645187L;

    private static final Object ERROR_TAG = "Error";

    private static final Method ERROR_DELEGATE_GET_STACK;
    private static final Method ERROR_DELEGATE_SET_STACK;

    static {
        try {
            // Pre-cache methods to be called via reflection
            ERROR_DELEGATE_GET_STACK = NativeError.class.getMethod("getStackDelegated", Scriptable.class);
            ERROR_DELEGATE_SET_STACK = NativeError.class.getMethod("setStackDelegated", Scriptable.class, Object.class);
        } catch (NoSuchMethodException nsm) {
            throw new RuntimeException(nsm);
        }
    }

    /** Default stack limit is set to "Infinity", here represented as a negative int */
    public static final int DEFAULT_STACK_LIMIT = -1;

    // This is used by "captureStackTrace"
    private static final String STACK_HIDE_KEY = "_stackHide";

    private RhinoException stackProvider;

    static void init(Scriptable scope, boolean sealed)
    {
        NativeError obj = new NativeError();
        ScriptableObject.putProperty(obj, "name", "Error");
        ScriptableObject.putProperty(obj, "message", "");
        ScriptableObject.putProperty(obj, "fileName", "");
        ScriptableObject.putProperty(obj, "lineNumber", Integer.valueOf(0));
        obj.setAttributes("name", ScriptableObject.DONTENUM);
        obj.setAttributes("message", ScriptableObject.DONTENUM);
        obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
        NativeCallSite.init(obj, sealed);
    }

    static NativeError make(Context cx, Scriptable scope,
                            IdFunctionObject ctorObj, Object[] args)
    {
        Scriptable proto = (Scriptable)(ctorObj.get("prototype", ctorObj));

        NativeError obj = new NativeError();
        obj.setPrototype(proto);
        obj.setParentScope(scope);

        int arglen = args.length;
        if (arglen >= 1) {
            if (args[0] != Undefined.instance) {
                ScriptableObject.putProperty(obj, "message",
                        ScriptRuntime.toString(args[0]));
            }
            if (arglen >= 2) {
                ScriptableObject.putProperty(obj, "fileName", args[1]);
                if (arglen >= 3) {
                    int line = ScriptRuntime.toInt32(args[2]);
                    ScriptableObject.putProperty(obj, "lineNumber",
                            Integer.valueOf(line));
                }
            }
        }
        return obj;
    }

    @Override
    protected void fillConstructorProperties(IdFunctionObject ctor)
    {
        addIdFunctionProperty(ctor, ERROR_TAG, ConstructorId_captureStackTrace,
                                  "captureStackTrace", 2);

        // This is running on the global "Error" object. Associate an object there that can store
        // default stack trace, etc.
        // This prevents us from having to add two additional fields to every Error object.
        ProtoProps protoProps = new ProtoProps();
        associateValue(ProtoProps.KEY, protoProps);

        // Define constructor properties that delegate to the ProtoProps object.
        ctor.defineProperty("stackTraceLimit", protoProps,
                            ProtoProps.GET_STACK_LIMIT, ProtoProps.SET_STACK_LIMIT, 0);
        ctor.defineProperty("prepareStackTrace", protoProps,
                            ProtoProps.GET_PREPARE_STACK, ProtoProps.SET_PREPARE_STACK, 0);

        super.fillConstructorProperties(ctor);
    }

    @Override
    public String getClassName()
    {
        return "Error";
    }

    @Override
    public String toString()
    {
        // According to spec, Error.prototype.toString() may return undefined.
        Object toString = js_toString(this);
        return toString instanceof String ? (String) toString : super.toString();
    }

    @Override
    protected void initPrototypeId(int id)
    {
        String s;
        int arity;
        switch (id) {
          case Id_constructor: arity=1; s="constructor"; break;
          case Id_toString:    arity=0; s="toString";    break;
          case Id_toSource:    arity=0; s="toSource";    break;
          default: throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(ERROR_TAG, id, s, arity);
    }

    @Override
    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
                             Scriptable thisObj, Object[] args)
    {
        if (!f.hasTag(ERROR_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();
        switch (id) {
          case Id_constructor:
            return make(cx, scope, f, args);

          case Id_toString:
            return js_toString(thisObj);

          case Id_toSource:
            return js_toSource(cx, scope, thisObj);

          case ConstructorId_captureStackTrace:
            js_captureStackTrace(cx, thisObj, args);
            return Undefined.instance;
        }
        throw new IllegalArgumentException(String.valueOf(id));
    }

    public void setStackProvider(RhinoException re) {
        // We go some extra miles to make sure the stack property is only
        // generated on demand, is cached after the first access, and is
        // overwritable like an ordinary property. Hence this setup with
        // the getter and setter below.
        if (stackProvider == null) {
            stackProvider = re;
            defineProperty("stack", this,
                           ERROR_DELEGATE_GET_STACK, ERROR_DELEGATE_SET_STACK,
                           DONTENUM);
        }
    }

    public Object getStackDelegated(Scriptable target) {
        if (stackProvider == null) {
            return NOT_FOUND;
        }

        // Get the object where prototype stuff is stored.
        int limit = DEFAULT_STACK_LIMIT;
        Function prepare = null;
        NativeError cons = (NativeError)getPrototype();
        ProtoProps pp = (ProtoProps)cons.getAssociatedValue(ProtoProps.KEY);

        if (pp != null) {
            limit = pp.getStackTraceLimit();
            prepare = pp.getPrepareStackTrace();
        }

        // This key is only set by captureStackTrace
        String hideFunc = (String)getAssociatedValue(STACK_HIDE_KEY);
        ScriptStackElement[] stack = stackProvider.getScriptStack(limit, hideFunc);

        // Determine whether to format the stack trace ourselves, or call the user's code to do it
        Object value;
        if (prepare == null) {
            value = RhinoException.formatStackTrace(stack, stackProvider.details());
        } else {
            value = callPrepareStack(prepare, stack);
        }

        // We store the stack as local property both to cache it
        // and to make the property writable
        setStackDelegated(target, value);
        return value;
    }

    public void setStackDelegated(Scriptable target, Object value) {
        target.delete("stack");
        stackProvider = null;
        target.put("stack", target, value);
    }

    private Object callPrepareStack(Function prepare, ScriptStackElement[] stack)
    {
        Context cx = Context.getCurrentContext();
        Object[] elts = new Object[stack.length];

        // The "prepareStackTrace" function takes an array of CallSite objects.
        for (int i = 0; i < stack.length; i++) {
            NativeCallSite site = (NativeCallSite)cx.newObject(this, "CallSite");
            site.setElement(stack[i]);
            elts[i] = site;
        }

        Scriptable eltArray = cx.newArray(this, elts);
        return prepare.call(cx, prepare, this, new Object[] { this, eltArray });
    }

    private static Object js_toString(Scriptable thisObj) {
        Object name = ScriptableObject.getProperty(thisObj, "name");
        if (name == NOT_FOUND || name == Undefined.instance) {
            name = "Error";
        } else {
            name = ScriptRuntime.toString(name);
        }
        Object msg = ScriptableObject.getProperty(thisObj, "message");
        if (msg == NOT_FOUND || msg == Undefined.instance) {
            msg = "";
        } else {
            msg = ScriptRuntime.toString(msg);
        }
        if (name.toString().length() == 0) {
            return msg;
        } else if (msg.toString().length() == 0) {
            return name;
        } else {
            return ((String) name) + ": " + ((String) msg);
        }
    }

    private static String js_toSource(Context cx, Scriptable scope,
                                      Scriptable thisObj)
    {
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
        if (message != NOT_FOUND
            || fileName != NOT_FOUND
            || lineNumber != NOT_FOUND)
        {
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

    private static void js_captureStackTrace(Context cx, Scriptable thisObj, Object[] args)
    {
        ScriptableObject obj = (ScriptableObject)ScriptRuntime.toObjectOrNull(cx, args[0], thisObj);
        Function func = null;
        if (args.length > 1) {
            func = (Function)ScriptRuntime.toObjectOrNull(cx, args[1], thisObj);
        }

        // Create a new error that will have the correct prototype so we can re-use "getStackTrace"
        NativeError err = (NativeError)cx.newObject(thisObj, "Error");
        // Wire it up so that it will have an actual exception with a stack trace
        err.setStackProvider(new EvaluatorException("[object Object]"));

        // Figure out if they passed a function used to hide part of the stack
        if (func != null) {
            Object funcName = func.get("name", func);
            if ((funcName != null) && !Undefined.instance.equals(funcName)) {
                err.associateValue(STACK_HIDE_KEY, Context.toString(funcName));
            }
        }

        // Define a property on the specified object to get that stack
        // that delegates to our new error. Build the stack trace lazily
        // using the "getStack" code from NativeError.
        obj.defineProperty("stack", err,
                           ERROR_DELEGATE_GET_STACK, ERROR_DELEGATE_SET_STACK, 0);
    }

    @Override
    protected int findPrototypeId(String s)
    {
        int id;
// #string_id_map#
// #generated# Last update: 2007-05-09 08:15:45 EDT
        L0: { id = 0; String X = null; int c;
            int s_length = s.length();
            if (s_length==8) {
                c=s.charAt(3);
                if (c=='o') { X="toSource";id=Id_toSource; }
                else if (c=='t') { X="toString";id=Id_toString; }
            }
            else if (s_length==11) { X="constructor";id=Id_constructor; }
            if (X!=null && X!=s && !X.equals(s)) id = 0;
            break L0;
        }
// #/generated#
        return id;
    }

    private static final int
        Id_constructor    = 1,
        Id_toString       = 2,
        Id_toSource       = 3,
        ConstructorId_captureStackTrace = -1,

        MAX_PROTOTYPE_ID  = 3;

// #/string_id_map#

    /**
     * We will attch this object to the constructor and use it solely to store the constructor properties
     * that are "global." We can't make them static because there can be many contexts in the same JVM.
     */
    private static final class ProtoProps
        implements Serializable
    {
        static final String KEY = "_ErrorPrototypeProps";

        static final Method GET_STACK_LIMIT;
        static final Method SET_STACK_LIMIT;
        static final Method GET_PREPARE_STACK;
        static final Method SET_PREPARE_STACK;

        static {
            try {
                GET_STACK_LIMIT = ProtoProps.class.getMethod("getStackTraceLimit", Scriptable.class);
                SET_STACK_LIMIT = ProtoProps.class.getMethod("setStackTraceLimit", Scriptable.class, Object.class);
                GET_PREPARE_STACK = ProtoProps.class.getMethod("getPrepareStackTrace", Scriptable.class);
                SET_PREPARE_STACK = ProtoProps.class.getMethod("setPrepareStackTrace", Scriptable.class, Object.class);
            } catch (NoSuchMethodException nsm) {
                throw new RuntimeException(nsm);
            }
        }

        private static final long serialVersionUID = 1907180507775337939L;

        private int stackTraceLimit = DEFAULT_STACK_LIMIT;
        private Function prepareStackTrace;

        public Object getStackTraceLimit(Scriptable thisObj) {
            if (stackTraceLimit >= 0) {
                return Integer.valueOf(stackTraceLimit);
            }
            return Double.valueOf(Double.POSITIVE_INFINITY);
        }

        public int getStackTraceLimit() {
            return stackTraceLimit;
        }

        public void setStackTraceLimit(Scriptable thisObj, Object value) {
            double limit = Context.toNumber(value);
            if (Double.isNaN(limit) || Double.isInfinite(limit)) {
                stackTraceLimit = -1;
            } else {
                stackTraceLimit = (int)limit;
            }
        }

        public Object getPrepareStackTrace(Scriptable thisObj)
        {
            Object ps = getPrepareStackTrace();
            return (ps == null ? Undefined.instance : ps);
        }

        public Function getPrepareStackTrace() {
            return prepareStackTrace;
        }

        public void setPrepareStackTrace(Scriptable thisObj, Object value) {
            if ((value == null) || Undefined.instance.equals(value)) {
                prepareStackTrace = null;
            } else if (value instanceof Function) {
                prepareStackTrace = (Function)value;
            }
        }
    }
}
