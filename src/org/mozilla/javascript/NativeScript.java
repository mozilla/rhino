/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1999.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1997-1999
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Norris Boyd
 *   Igor Bukanov
 *   Roger Lawrence
 *   Mike McCabe
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU General Public License Version 2 or later (the "GPL"), in which
 * case the provisions of the GPL are applicable instead of those above. If
 * you wish to allow use of your version of this file only under the terms of
 * the GPL and not to allow others to use your version of this file under the
 * MPL, indicate your decision by deleting the provisions above and replacing
 * them with the notice and other provisions required by the GPL. If you do
 * not delete the provisions above, a recipient may use your version of this
 * file under either the MPL or the GPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.javascript;

/**
 * The JavaScript Script object.
 *
 * Note that the C version of the engine uses XDR as the format used
 * by freeze and thaw. Since this depends on the internal format of
 * structures in the C runtime, we cannot duplicate it.
 *
 * Since we cannot replace 'this' as a result of the compile method,
 * will forward requests to execute to the nonnull 'script' field.
 *
 * @since 1.3
 * @author Norris Boyd
 */

class NativeScript extends BaseFunction
{
    static final long serialVersionUID = -6795101161980121700L;

    private static final Object SCRIPT_TAG = "Script";

    static void init(Scriptable scope, boolean sealed)
    {
        NativeScript obj = new NativeScript(null);
        obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
    }

    private NativeScript(Script script)
    {
        this.script = script;
    }

    /**
     * Returns the name of this JavaScript class, "Script".
     */
    @Override
    public String getClassName()
    {
        return "Script";
    }

    @Override
    public Object call(Context cx, Scriptable scope, Object thisObj,
                       Object[] args)
    {
        if (script != null) {
            return script.exec(cx, scope);
        }
        return Undefined.instance;
    }

    @Override
    public Scriptable construct(Context cx, Scriptable scope, Object[] args)
    {
        throw Context.reportRuntimeError0("msg.script.is.not.constructor");
    }

    @Override
    public int getLength()
    {
        return 0;
    }

    @Override
    public int getArity()
    {
        return 0;
    }

    @Override
    String decompile(int indent, int flags)
    {
        if (script instanceof NativeFunction) {
            return ((NativeFunction)script).decompile(indent, flags);
        }
        return super.decompile(indent, flags);
    }

    @Override
    protected void initPrototypeId(int id)
    {
        String s;
        int arity;
        switch (id) {
          case Id_constructor: arity=1; s="constructor"; break;
          case Id_toString:    arity=0; s="toString";    break;
          case Id_exec:        arity=0; s="exec";        break;
          case Id_compile:     arity=1; s="compile";     break;
          default: throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(SCRIPT_TAG, id, s, arity);
    }

    @Override
    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
                             Object thisObj, Object[] args)
    {
        if (!f.hasTag(SCRIPT_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();
        switch (id) {
          case Id_constructor: {
            String source = (args.length == 0)
                            ? ""
                            : ScriptRuntime.toString(args[0]);
            Script script = compile(cx, source);
            NativeScript nscript = new NativeScript(script);
            ScriptRuntime.setObjectProtoAndParent(nscript, scope);
            return nscript;
          }

          case Id_toString: {
            NativeScript real = realThis(thisObj, f);
            Script realScript = real.script;
            if (realScript == null) { return ""; }
            return cx.decompileScript(realScript, 0);
          }

          case Id_exec: {
            throw Context.reportRuntimeError1(
                "msg.cant.call.indirect", "exec");
          }

          case Id_compile: {
            NativeScript real = realThis(thisObj, f);
            String source = ScriptRuntime.toString(args, 0);
            real.script = compile(cx, source);
            return real;
          }
        }
        throw new IllegalArgumentException(String.valueOf(id));
    }

    private static NativeScript realThis(Object thisObj, IdFunctionObject f)
    {
        if (!(thisObj instanceof NativeScript))
            throw incompatibleCallError(f);
        return (NativeScript)thisObj;
    }

    private static Script compile(Context cx, String source)
    {
        int[] linep = { 0 };
        String filename = Context.getSourcePositionFromStack(linep);
        if (filename == null) {
            filename = "<Script object>";
            linep[0] = 1;
        }
        ErrorReporter reporter;
        reporter = DefaultErrorReporter.forEval(cx.getErrorReporter());
        return cx.compileString(source, null, reporter, filename,
                                linep[0], null);
    }

// #string_id_map#

    @Override
    protected int findPrototypeId(String s)
    {
        int id;
// #generated# Last update: 2007-05-09 08:16:01 EDT
        L0: { id = 0; String X = null;
            L: switch (s.length()) {
            case 4: X="exec";id=Id_exec; break L;
            case 7: X="compile";id=Id_compile; break L;
            case 8: X="toString";id=Id_toString; break L;
            case 11: X="constructor";id=Id_constructor; break L;
            }
            if (X!=null && X!=s && !X.equals(s)) id = 0;
            break L0;
        }
// #/generated#
        return id;
    }

    private static final int
        Id_constructor    = 1,
        Id_toString       = 2,
        Id_compile        = 3,
        Id_exec           = 4,
        MAX_PROTOTYPE_ID  = 4;

// #/string_id_map#

    private Script script;
}

