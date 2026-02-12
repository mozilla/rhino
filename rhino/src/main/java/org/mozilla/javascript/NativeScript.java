/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static org.mozilla.javascript.ClassDescriptor.Destination.PROTO;

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

    private static final String SCRIPT_TAG = "Script";

    private static final ClassDescriptor DESCRIPTOR;

    static {
        DESCRIPTOR =
                new ClassDescriptor.Builder(
                                SCRIPT_TAG,
                                1,
                                NativeScript::js_constructorCall,
                                NativeScript::js_constructor)
                        .withMethod(PROTO, "toString", 0, NativeScript::js_toString)
                        .withMethod(PROTO, "exec", 0, NativeScript::js_exec)
                        .withMethod(PROTO, "compile", 0, NativeScript::js_compile)
                        .build();
    }

    static JSFunction init2(Context cx, VarScope scope, boolean sealed) {
        var proto = new NativeScript(null);
        return DESCRIPTOR.buildConstructor(cx, scope, proto, sealed);
    }

    /**
     * @deprecated Use {@link #init(Context, VarScope, boolean)} instead
     */
    @Deprecated
    static void init(VarScope scope, boolean sealed) {
        init2(Context.getContext(), scope, sealed);
    }

    private NativeScript(Script script) {
        super(null);
        this.script = script;
    }

    /** Returns the name of this JavaScript class, "Script". */
    @Override
    public String getClassName() {
        return "Script";
    }

    @Override
    public Object call(Context cx, VarScope scope, Object thisObj, Object[] args) {
        if (script != null) {
            return script.exec(cx, scope, thisObj);
        }
        return Undefined.instance;
    }

    @Override
    public Scriptable construct(Context cx, VarScope scope, Object[] args) {
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
        if (script instanceof JSFunction) {
            return ((JSFunction) script).decompile(indent, flags);
        }
        return super.decompile(indent, flags);
    }

    private static Object js_compile(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        NativeScript real = realThis(thisObj, "compile");
        String source = ScriptRuntime.toString(args, 0);
        real.script = compile(cx, source);
        return real;
    }

    private static Object js_exec(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        throw Context.reportRuntimeErrorById("msg.cant.call.indirect", "exec");
    }

    private static Object js_toString(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        NativeScript real = realThis(thisObj, "toString");
        Script realScript = real.script;
        if (realScript == null) {
            return "";
        }
        return cx.decompileScript(realScript, 0);
    }

    private static Scriptable js_constructorCall(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return js_constructor(cx, f, nt, s, thisObj, args);
    }

    private static Scriptable js_constructor(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        String source = (args.length == 0) ? "" : ScriptRuntime.toString(args[0]);
        Script script = compile(cx, source);
        NativeScript nscript = new NativeScript(script);
        ScriptRuntime.setObjectProtoAndParent(nscript, f.getDeclarationScope());
        return nscript;
    }

    private static NativeScript realThis(Object thisObj, String name) {
        return ensureType(thisObj, NativeScript.class, name);
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

    private Script script;
}
