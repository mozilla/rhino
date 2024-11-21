/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.EnumSet;

/**
 * The JavaScript Script object.
 *
 * <p>Note that the C version of the engine uses XDR as the format used by freeze and thaw. Since
 * this depends on the internal format of structures in the C runtime, we cannot duplicate it.
 *
 * <p>Since we cannot replace 'this' as a result of the compile method, will forward requests to
 * execute to the nonnull 'script' field.
 *
 * @since 1.3
 * @author Norris Boyd
 */
class NativeScript extends BaseFunction {
    private static final long serialVersionUID = -6795101161980121700L;

    private static final Object SCRIPT_TAG = "Script";

    static void init(Context cx, Scriptable scope, boolean sealed) {
        NativeScript obj = new NativeScript(null);
        obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
    }

    /**
     * @deprecated Use {@link #init(Context, Scriptable, boolean)} instead
     */
    @Deprecated
    static void init(Scriptable scope, boolean sealed) {
        init(Context.getContext(), scope, sealed);
    }

    private NativeScript(Script script) {
        this.script = script;
    }

    /** Returns the name of this JavaScript class, "Script". */
    @Override
    public String getClassName() {
        return "Script";
    }

    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (script != null) {
            return script.exec(cx, scope);
        }
        return Undefined.instance;
    }

    @Override
    public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
        throw Context.reportRuntimeErrorById("msg.script.is.not.constructor");
    }

    @Override
    public int getLength() {
        return 0;
    }

    @Override
    public int getArity() {
        return 0;
    }

    @Override
    String decompile(int indent, EnumSet<DecompilerFlag> flags) {
        if (script instanceof NativeFunction) {
            return ((NativeFunction) script).decompile(indent, flags);
        }
        return super.decompile(indent, flags);
    }

    @Override
    protected void initPrototypeId(int id) {
        String s;
        int arity;
        switch (id) {
            case Id_constructor:
                arity = 1;
                s = "constructor";
                break;
            case Id_toString:
                arity = 0;
                s = "toString";
                break;
            case Id_exec:
                arity = 0;
                s = "exec";
                break;
            case Id_compile:
                arity = 1;
                s = "compile";
                break;
            default:
                throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(SCRIPT_TAG, id, s, arity);
    }

    @Override
    public Object execIdCall(
            IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (!f.hasTag(SCRIPT_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();
        switch (id) {
            case Id_constructor:
                {
                    String source = (args.length == 0) ? "" : ScriptRuntime.toString(args[0]);
                    Script script = compile(cx, source);
                    NativeScript nscript = new NativeScript(script);
                    ScriptRuntime.setObjectProtoAndParent(nscript, scope);
                    return nscript;
                }

            case Id_toString:
                {
                    NativeScript real = realThis(thisObj, f);
                    Script realScript = real.script;
                    if (realScript == null) {
                        return "";
                    }
                    return cx.decompileScript(realScript, 0);
                }

            case Id_exec:
                {
                    throw Context.reportRuntimeErrorById("msg.cant.call.indirect", "exec");
                }

            case Id_compile:
                {
                    NativeScript real = realThis(thisObj, f);
                    String source = ScriptRuntime.toString(args, 0);
                    real.script = compile(cx, source);
                    return real;
                }
        }
        throw new IllegalArgumentException(String.valueOf(id));
    }

    private static NativeScript realThis(Scriptable thisObj, IdFunctionObject f) {
        return ensureType(thisObj, NativeScript.class, f);
    }

    private static Script compile(Context cx, String source) {
        int[] linep = {0};
        String filename = Context.getSourcePositionFromStack(linep);
        if (filename == null) {
            filename = "<Script object>";
            linep[0] = 1;
        }
        ErrorReporter reporter;
        reporter = DefaultErrorReporter.forEval(cx.getErrorReporter());
        return cx.compileString(source, null, reporter, filename, linep[0], null, null);
    }

    @Override
    protected int findPrototypeId(String s) {
        int id;
        switch (s) {
            case "constructor":
                id = Id_constructor;
                break;
            case "toString":
                id = Id_toString;
                break;
            case "compile":
                id = Id_compile;
                break;
            case "exec":
                id = Id_exec;
                break;
            default:
                id = 0;
                break;
        }
        return id;
    }

    private static final int Id_constructor = 1,
            Id_toString = 2,
            Id_compile = 3,
            Id_exec = 4,
            MAX_PROTOTYPE_ID = 4;

    private Script script;
}
