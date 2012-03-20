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
 *   Igor Bukanov
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

public final class NativeContinuation extends IdScriptableObject
    implements Function
{
    static final long serialVersionUID = 1794167133757605367L;

    private static final Object FTAG = "Continuation";

    private Object implementation;

    public static void init(Context cx, Scriptable scope, boolean sealed)
    {
        NativeContinuation obj = new NativeContinuation();
        obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
    }

    public Object getImplementation()
    {
        return implementation;
    }

    public void initImplementation(Object implementation)
    {
        this.implementation = implementation;
    }

    @Override
    public String getClassName()
    {
        return "Continuation";
    }

    public Scriptable construct(Context cx, Scriptable scope, Object[] args)
    {
        throw Context.reportRuntimeError("Direct call is not supported");
    }

    public Object call(Context cx, Scriptable scope, Object thisObj,
                       Object[] args)
    {
        return Interpreter.restartContinuation(this, cx, scope, args);
    }

    public static boolean isContinuationConstructor(IdFunctionObject f)
    {
        if (f.hasTag(FTAG) && f.methodId() == Id_constructor) {
            return true;
        }
        return false;
    }

    @Override
    protected void initPrototypeId(int id)
    {
        String s;
        int arity;
        switch (id) {
          case Id_constructor: arity=0; s="constructor"; break;
          default: throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(FTAG, id, s, arity);
    }

    @Override
    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
                             Object thisObj, Object[] args)
    {
        if (!f.hasTag(FTAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();
        switch (id) {
          case Id_constructor:
            throw Context.reportRuntimeError("Direct call is not supported");
        }
        throw new IllegalArgumentException(String.valueOf(id));
    }

// #string_id_map#

    @Override
    protected int findPrototypeId(String s)
    {
        int id;
// #generated# Last update: 2007-05-09 08:16:40 EDT
        L0: { id = 0; String X = null;
            if (s.length()==11) { X="constructor";id=Id_constructor; }
            if (X!=null && X!=s && !X.equals(s)) id = 0;
            break L0;
        }
// #/generated#
        return id;
    }

    private static final int
        Id_constructor          = 1,
        MAX_PROTOTYPE_ID        = 1;

// #/string_id_map#
}
