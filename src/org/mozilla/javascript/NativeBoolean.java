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

import static org.mozilla.javascript.TopLevel.getBuiltinPrototype;
import org.mozilla.javascript.TopLevel.Builtins;

/**
 * This class implements the Boolean native object.
 * See ECMA 15.6.
 * @author Norris Boyd
 */
final class NativeBoolean extends IdScriptableObject
{
    static final long serialVersionUID = -3716996899943880933L;

    private static final Object BOOLEAN_TAG = "Boolean";

    static void init(Scriptable scope, boolean sealed)
    {
        NativeBoolean obj = new NativeBoolean(false);
        obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
    }

    NativeBoolean(boolean b)
    {
        booleanValue = b;
    }

    @Override
    public String getClassName()
    {
        return "Boolean";
    }

    @Override
    public Object getDefaultValue(Class<?> typeHint) {
        // This is actually non-ECMA, but will be proposed
        // as a change in round 2.
        if (typeHint == ScriptRuntime.BooleanClass)
            return ScriptRuntime.wrapBoolean(booleanValue);
        return super.getDefaultValue(typeHint);
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
          case Id_valueOf:     arity=0; s="valueOf";     break;
          default: throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(BOOLEAN_TAG, id, s, arity);
    }

    @Override
    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
                             Object thisObj, Object[] args)
    {
        if (!f.hasTag(BOOLEAN_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();

        if (id == Id_constructor) {
            boolean b;
            if (args.length == 0) {
                b = false;
            } else {
                b = args[0] instanceof ScriptableObject &&
                        ((ScriptableObject) args[0]).avoidObjectDetection()
                    ? true
                    : ScriptRuntime.toBoolean(args[0]);
            }
            if (thisObj == null) {
                // new Boolean(val) creates a new boolean object.
                return new NativeBoolean(b);
            }
            // Boolean(val) converts val to a boolean.
            return ScriptRuntime.wrapBoolean(b);
        }

        // The rest of Boolean.prototype methods require thisObj to be Boolean

        boolean value;
        if (thisObj instanceof Boolean) {
            value = ((Boolean)thisObj).booleanValue();
        } else if (thisObj instanceof NativeBoolean) {
            value = ((NativeBoolean)thisObj).booleanValue;
        } else {
            throw incompatibleCallError(f);
        }

        switch (id) {

          case Id_toString:
            return value ? "true" : "false";

          case Id_toSource:
            return value ? "(new Boolean(true))" : "(new Boolean(false))";

          case Id_valueOf:
            return ScriptRuntime.wrapBoolean(value);
        }
        throw new IllegalArgumentException(String.valueOf(id));
    }

    /**
     * Special [[Get]] if reference base value is primitive, see ES5.1 [8.7.1]:
     * - Support for the Boolean type
     * @see ScriptRuntime#getPrimitiveValue(Object, Scriptable, String, int, Context, Scriptable)
     */
    static Object getPrimitiveValue(Boolean base, String property, int index,
                                    Context cx, Scriptable scope) {
        Object value = NOT_FOUND;
        // search in boolean prototype
        NativeBoolean booleanProto = getBooleanPrototype(scope);
        value = booleanProto.getSlotOrProtoValue(property, index, base, cx, scope);
        if (value != NOT_FOUND) return value;
        // search in object prototype
        NativeObject objectProto = (NativeObject)booleanProto.getPrototype();
        value = objectProto.getSlotOrProtoValue(property, index, base, cx, scope);
        // object prototype has no other prototype
        assert objectProto.getPrototype() == null;
        return value;
    }

    private static NativeBoolean getBooleanPrototype(Scriptable scope) {
        Scriptable proto = getBuiltinPrototype(getTopLevelScope(scope), Builtins.Boolean);
        assert proto instanceof NativeBoolean;
        return (NativeBoolean) proto;
    }

// #string_id_map#

    @Override
    protected int findPrototypeId(String s)
    {
        int id;
// #generated# Last update: 2007-05-09 08:15:31 EDT
        L0: { id = 0; String X = null; int c;
            int s_length = s.length();
            if (s_length==7) { X="valueOf";id=Id_valueOf; }
            else if (s_length==8) {
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
        Id_constructor          = 1,
        Id_toString             = 2,
        Id_toSource             = 3,
        Id_valueOf              = 4,
        MAX_PROTOTYPE_ID        = 4;

// #/string_id_map#

    private boolean booleanValue;
}
