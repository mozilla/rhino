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
 *   Bob Jervis
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
 * This class implements the activation object.
 *
 * See ECMA 10.1.6
 *
 * @see org.mozilla.javascript.Arguments
 * @author Norris Boyd
 */
public final class NativeCall extends IdScriptableObject
{
    static final long serialVersionUID = -7471457301304454454L;

    private static final Object CALL_TAG = "Call";

    static void init(Scriptable scope, boolean sealed)
    {
        NativeCall obj = new NativeCall();
        obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
    }

    NativeCall() { }

    NativeCall(NativeFunction function, Scriptable scope, Object[] args)
    {
        this.function = function;

        setParentScope(scope);
        // leave prototype null

        this.originalArgs = (args == null) ? ScriptRuntime.emptyArgs : args;

        // initialize values of arguments
        int paramAndVarCount = function.getParamAndVarCount();
        int paramCount = function.getParamCount();
        if (paramAndVarCount != 0) {
            for (int i = 0; i < paramCount; ++i) {
                String name = function.getParamOrVarName(i);
                Object val = i < args.length ? args[i]
                                             : Undefined.instance;
                defineProperty(name, val, PERMANENT);
            }
        }

        // initialize "arguments" property but only if it was not overridden by
        // the parameter with the same name
        if (!super.has("arguments", this)) {
            defineProperty("arguments", new Arguments(this), PERMANENT);
        }

        if (paramAndVarCount != 0) {
            for (int i = paramCount; i < paramAndVarCount; ++i) {
                String name = function.getParamOrVarName(i);
                if (!super.has(name, this)) {
                    if (function.getParamOrVarConst(i))
                        defineProperty(name, Undefined.instance, CONST);
                    else
                        defineProperty(name, Undefined.instance, PERMANENT);
                }
            }
        }
    }

    @Override
    public String getClassName()
    {
        return "Call";
    }

    @Override
    protected int findPrototypeId(String s)
    {
        return s.equals("constructor") ? Id_constructor : 0;
    }

    @Override
    protected void initPrototypeId(int id)
    {
        String s;
        int arity;
        if (id == Id_constructor) {
            arity=1; s="constructor";
        } else {
            throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(CALL_TAG, id, s, arity);
    }

    @Override
    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
                             Object thisObj, Object[] args)
    {
        if (!f.hasTag(CALL_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();
        if (id == Id_constructor) {
            if (thisObj != null) {
                throw Context.reportRuntimeError1("msg.only.from.new", "Call");
            }
            ScriptRuntime.checkDeprecated(cx, "Call");
            NativeCall result = new NativeCall();
            result.setPrototype(getObjectPrototype(scope));
            return result;
        }
        throw new IllegalArgumentException(String.valueOf(id));
    }

    private static final int
        Id_constructor   = 1,
        MAX_PROTOTYPE_ID = 1;

    NativeFunction function;
    Object[] originalArgs;

    transient NativeCall parentActivationCall;
}

