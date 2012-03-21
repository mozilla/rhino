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
 * This class implements the Number native object.
 *
 * See ECMA 15.7.
 *
 * @author Norris Boyd
 */
final class NativeNumber extends IdScriptableObject
{
    static final long serialVersionUID = 3504516769741512101L;

    private static final Object NUMBER_TAG = "Number";

    private static final int MAX_PRECISION = 100;

    static void init(Scriptable scope, boolean sealed)
    {
        NativeNumber obj = new NativeNumber(0.0);
        obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
    }

    NativeNumber(double number)
    {
        doubleValue = number;
    }

    @Override
    public String getClassName()
    {
        return "Number";
    }

    @Override
    protected void fillConstructorProperties(IdFunctionObject ctor)
    {
        final int attr = ScriptableObject.DONTENUM |
                         ScriptableObject.PERMANENT |
                         ScriptableObject.READONLY;

        ctor.defineProperty("NaN", ScriptRuntime.NaNobj, attr, false);
        ctor.defineProperty("POSITIVE_INFINITY",
                            ScriptRuntime.wrapNumber(Double.POSITIVE_INFINITY),
                            attr, false);
        ctor.defineProperty("NEGATIVE_INFINITY",
                            ScriptRuntime.wrapNumber(Double.NEGATIVE_INFINITY),
                            attr, false);
        ctor.defineProperty("MAX_VALUE",
                            ScriptRuntime.wrapNumber(Double.MAX_VALUE),
                            attr, false);
        ctor.defineProperty("MIN_VALUE",
                            ScriptRuntime.wrapNumber(Double.MIN_VALUE),
                            attr, false);

        super.fillConstructorProperties(ctor);
    }

    @Override
    protected void initPrototypeId(int id)
    {
        String s;
        int arity;
        switch (id) {
          case Id_constructor:    arity=1; s="constructor";    break;
          case Id_toString:       arity=1; s="toString";       break;
          case Id_toLocaleString: arity=1; s="toLocaleString"; break;
          case Id_toSource:       arity=0; s="toSource";       break;
          case Id_valueOf:        arity=0; s="valueOf";        break;
          case Id_toFixed:        arity=1; s="toFixed";        break;
          case Id_toExponential:  arity=1; s="toExponential";  break;
          case Id_toPrecision:    arity=1; s="toPrecision";    break;
          default: throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(NUMBER_TAG, id, s, arity);
    }

    @Override
    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
                             Object thisObj, Object[] args)
    {
        if (!f.hasTag(NUMBER_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();
        if (id == Id_constructor) {
            double val = (args.length >= 1)
                ? ScriptRuntime.toNumber(args[0]) : 0.0;
            if (thisObj == null) {
                // new Number(val) creates a new Number object.
                return new NativeNumber(val);
            }
            // Number(val) converts val to a number value.
            return ScriptRuntime.wrapNumber(val);
        }

        // The rest of Number.prototype methods require thisObj to be Number

        double value;
        if (thisObj instanceof Number) {
            value = ((Number)thisObj).doubleValue();
        } else if (thisObj instanceof NativeNumber) {
            value = ((NativeNumber)thisObj).doubleValue;
        } else {
            throw incompatibleCallError(f);
        }

        switch (id) {

          case Id_toString:
          case Id_toLocaleString:
            {
                // toLocaleString is just an alias for toString for now
                int base = (args.length == 0 || args[0] == Undefined.instance)
                    ? 10 : ScriptRuntime.toInt32(args[0]);
                return ScriptRuntime.numberToString(value, base);
            }

          case Id_toSource:
            return "(new Number("+ScriptRuntime.toString(value)+"))";

          case Id_valueOf:
            return ScriptRuntime.wrapNumber(value);

          case Id_toFixed:
            return num_to(value, args, DToA.DTOSTR_FIXED,
                          DToA.DTOSTR_FIXED, 0, 0);

          case Id_toExponential: {
              // Handle special values before range check
              if(Double.isNaN(value)) {
                  return "NaN";
              }
              if(Double.isInfinite(value)) {
                  if(value >= 0) {
                      return "Infinity";
                  }
                  else {
                      return "-Infinity";
                  }
              }
              // General case
              return num_to(value, args, DToA.DTOSTR_STANDARD_EXPONENTIAL,
                      DToA.DTOSTR_EXPONENTIAL, 0, 1);
          }

          case Id_toPrecision: {
              // Undefined precision, fall back to ToString()
              if(args.length == 0 || args[0] == Undefined.instance) {
                  return ScriptRuntime.numberToString(value, 10);
              }
              // Handle special values before range check
              if(Double.isNaN(value)) {
                  return "NaN";
              }
              if(Double.isInfinite(value)) {
                  if(value >= 0) {
                      return "Infinity";
                  }
                  else {
                      return "-Infinity";
                  }
              }
              return num_to(value, args, DToA.DTOSTR_STANDARD,
                      DToA.DTOSTR_PRECISION, 1, 0);
          }

          default: throw new IllegalArgumentException(String.valueOf(id));
        }
    }

    @Override
    public String toString() {
        return ScriptRuntime.numberToString(doubleValue, 10);
    }

    private static String num_to(double val,
                                 Object[] args,
                                 int zeroArgMode, int oneArgMode,
                                 int precisionMin, int precisionOffset)
    {
        int precision;
        if (args.length == 0) {
            precision = 0;
            oneArgMode = zeroArgMode;
        } else {
            /* We allow a larger range of precision than ECMA requires;
               this is permitted by ECMA. [ES5.1, ch. 16 Errors] */
            double p = ScriptRuntime.toInteger(args[0]);
            if (p < precisionMin || p > MAX_PRECISION) {
                String msg = ScriptRuntime.getMessage1(
                    "msg.bad.precision", ScriptRuntime.toString(args[0]));
                throw ScriptRuntime.constructError("RangeError", msg);
            }
            precision = ScriptRuntime.toInt32(p);
        }
        StringBuilder sb = new StringBuilder();
        DToA.JS_dtostr(sb, oneArgMode, precision + precisionOffset, val);
        return sb.toString();
    }

    /**
     * Special [[Get]] if reference base value is primitive, see ES5.1 [8.7.1]:
     * - Support for the Number type
     * @see ScriptRuntime#getPrimitiveValue(Object, Scriptable, String, int, Context, Scriptable)
     */
    static Object getPrimitiveValue(Number base, String property, int index,
                                    Context cx, Scriptable scope) {
        Object value = NOT_FOUND;
        // search in number prototype
        NativeNumber numberProto = getNumberPrototype(scope);
        value = numberProto.getSlotOrProtoValue(property, index, base, cx, scope);
        if (value != NOT_FOUND) return value;
        // search in object prototype
        NativeObject objectProto = (NativeObject)numberProto.getPrototype();
        value = objectProto.getSlotOrProtoValue(property, index, base, cx, scope);
        // object prototype has no other prototype
        assert objectProto.getPrototype() == null;
        return value;
    }

    private static NativeNumber getNumberPrototype(Scriptable scope) {
        Scriptable proto = getBuiltinPrototype(getTopLevelScope(scope), Builtins.Number);
        assert proto instanceof NativeNumber;
        return (NativeNumber) proto;
    }

// #string_id_map#

    @Override
    protected int findPrototypeId(String s)
    {
        int id;
// #generated# Last update: 2007-05-09 08:15:50 EDT
        L0: { id = 0; String X = null; int c;
            L: switch (s.length()) {
            case 7: c=s.charAt(0);
                if (c=='t') { X="toFixed";id=Id_toFixed; }
                else if (c=='v') { X="valueOf";id=Id_valueOf; }
                break L;
            case 8: c=s.charAt(3);
                if (c=='o') { X="toSource";id=Id_toSource; }
                else if (c=='t') { X="toString";id=Id_toString; }
                break L;
            case 11: c=s.charAt(0);
                if (c=='c') { X="constructor";id=Id_constructor; }
                else if (c=='t') { X="toPrecision";id=Id_toPrecision; }
                break L;
            case 13: X="toExponential";id=Id_toExponential; break L;
            case 14: X="toLocaleString";id=Id_toLocaleString; break L;
            }
            if (X!=null && X!=s && !X.equals(s)) id = 0;
            break L0;
        }
// #/generated#
        return id;
    }

    private static final int
        Id_constructor           = 1,
        Id_toString              = 2,
        Id_toLocaleString        = 3,
        Id_toSource              = 4,
        Id_valueOf               = 5,
        Id_toFixed               = 6,
        Id_toExponential         = 7,
        Id_toPrecision           = 8,
        MAX_PROTOTYPE_ID         = 8;

// #/string_id_map#

    private double doubleValue;
}
