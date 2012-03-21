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
 * Portions created by the Initial Developer are Copyright (C) 1997-2000
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Igor Bukanov
 *   Bob Jervis
 *   Roger Lawrence
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

import org.mozilla.javascript.debug.DebuggableScript;

final class InterpretedFunction extends NativeFunction implements Script
{
    static final long serialVersionUID = 541475680333911468L;

    InterpreterData idata;
    SecurityController securityController;
    Object securityDomain;
    Scriptable[] functionRegExps;

    private InterpretedFunction(InterpreterData idata,
                                Object staticSecurityDomain)
    {
        this.idata = idata;

        // Always get Context from the current thread to
        // avoid security breaches via passing mangled Context instances
        // with bogus SecurityController
        Context cx = Context.getContext();
        SecurityController sc = cx.getSecurityController();
        Object dynamicDomain;
        if (sc != null) {
            dynamicDomain = sc.getDynamicSecurityDomain(staticSecurityDomain);
        } else {
            if (staticSecurityDomain != null) {
                throw new IllegalArgumentException();
            }
            dynamicDomain = null;
        }

        this.securityController = sc;
        this.securityDomain = dynamicDomain;
    }

    private InterpretedFunction(InterpretedFunction parent, int index)
    {
        this.idata = parent.idata.itsNestedFunctions[index];
        this.securityController = parent.securityController;
        this.securityDomain = parent.securityDomain;
    }

    /**
     * Create script from compiled bytecode.
     */
    static InterpretedFunction createScript(InterpreterData idata,
                                            Object staticSecurityDomain)
    {
        InterpretedFunction f;
        f = new InterpretedFunction(idata, staticSecurityDomain);
        return f;
    }

    /**
     * Create function compiled from Function(...) constructor.
     */
    static InterpretedFunction createFunction(Context cx,Scriptable scope,
                                              InterpreterData idata,
                                              Object staticSecurityDomain)
    {
        InterpretedFunction f;
        f = new InterpretedFunction(idata, staticSecurityDomain);
        f.initInterpretedFunction(cx, scope);
        return f;
    }

    /**
     * Create function embedded in script or another function.
     */
    static InterpretedFunction createFunction(Context cx, Scriptable scope,
                                              InterpretedFunction  parent,
                                              int index)
    {
        InterpretedFunction f = new InterpretedFunction(parent, index);
        f.initInterpretedFunction(cx, scope);
        return f;
    }

    Scriptable[] createRegExpWraps(Context cx, Scriptable scope)
    {
        if (idata.itsRegExpLiterals == null) Kit.codeBug();

        RegExpProxy rep = ScriptRuntime.checkRegExpProxy(cx);
        int N = idata.itsRegExpLiterals.length;
        Scriptable[] array = new Scriptable[N];
        for (int i = 0; i != N; ++i) {
            array[i] = rep.wrapRegExp(cx, scope, idata.itsRegExpLiterals[i]);
        }
        return array;
    }

    private void initInterpretedFunction(Context cx, Scriptable scope)
    {
        initScriptFunction(cx, scope);
        if (idata.itsRegExpLiterals != null) {
            functionRegExps = createRegExpWraps(cx, scope);
        }
    }

    @Override
    public String getFunctionName()
    {
        return (idata.itsName == null) ? "" : idata.itsName;
    }

    /**
     * Calls the function.
     * @param cx the current context
     * @param scope the scope used for the call
     * @param thisObj the value of "this"
     * @param args function arguments. Must not be null. You can use
     * {@link ScriptRuntime#emptyArgs} to pass empty arguments.
     * @return the result of the function call.
     */
    @Override
    public Object call(Context cx, Scriptable scope, Object thisObj,
                       Object[] args)
    {
        if (!idata.isStrict) {
            thisObj = ScriptRuntime.toObjectOrNull(cx, thisObj, scope);
            if (thisObj == null) {
                thisObj = ScriptRuntime.getTopCallScope(cx);
            }
        }
        if (!ScriptRuntime.hasTopCall(cx)) {
            return ScriptRuntime.doTopCall(this, cx, scope, thisObj, args);
        }
        return Interpreter.interpret(this, cx, scope, thisObj, args);
    }

    public Object exec(Context cx, Scriptable scope)
    {
        if (!isScript()) {
            // Can only be applied to scripts
            throw new IllegalStateException();
        }
        if (!ScriptRuntime.hasTopCall(cx)) {
            // It will go through "call" path. but they are equivalent
            return ScriptRuntime.doTopCall(
                this, cx, scope, scope, ScriptRuntime.emptyArgs);
        }
        return Interpreter.interpret(
            this, cx, scope, scope, ScriptRuntime.emptyArgs);
    }

    public boolean isScript() {
        return idata.itsFunctionType == 0;
    }

    @Override
    public String getEncodedSource()
    {
        return Interpreter.getEncodedSource(idata);
    }

    @Override
    public DebuggableScript getDebuggableView()
    {
        return idata;
    }

    @Override
    public Object resumeGenerator(Context cx, Scriptable scope, int operation,
                                  Object state, Object value)
    {
        return Interpreter.resumeGenerator(cx, scope, operation, state, value);
    }

    @Override
    protected int getLanguageVersion()
    {
        return idata.languageVersion;
    }

    @Override
    protected int getParamCount()
    {
        return idata.argCount;
    }

    @Override
    protected int getParamAndVarCount()
    {
        return idata.argNames.length;
    }

    @Override
    protected String getParamOrVarName(int index)
    {
        return idata.argNames[index];
    }

    @Override
    protected boolean getParamOrVarConst(int index)
    {
        return idata.argIsConst[index];
    }

    @Override
    protected boolean isStrict()
    {
        return idata.isStrict;
    }
}

