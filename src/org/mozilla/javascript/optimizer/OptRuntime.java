/* ***** BEGIN LICENSE BLOCK *****
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
 *   Norris Boyd
 *   Roger Lawrence
 *   Hannes Wallnoefer
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


package org.mozilla.javascript.optimizer;

import org.mozilla.javascript.*;

public final class OptRuntime extends ScriptRuntime
{

    public static final Double zeroObj = new Double(0.0);
    public static final Double oneObj = new Double(1.0);
    public static final Double minusOneObj = new Double(-1.0);

    /**
     * Implement ....() call shrinking optimizer code.
     */
    public static Object call0(Object fun, Object thisObj,
                               Context cx, Scriptable scope)
    {
        Callable c = ensureCallable(fun);
        return c.call(cx, scope, thisObj, ScriptRuntime.emptyArgs);
    }

    /**
     * Implement ....(arg) call shrinking optimizer code.
     */
    public static Object call1(Object fun, Object thisObj, Object arg0,
                               Context cx, Scriptable scope)
    {
        Callable c = ensureCallable(fun);
        return c.call(cx, scope, thisObj, new Object[] { arg0 } );
    }

    /**
     * Implement ....(arg0, arg1) call shrinking optimizer code.
     */
    public static Object call2(Object fun, Object thisObj,
                               Object arg0, Object arg1,
                               Context cx, Scriptable scope)
    {
        Callable c = ensureCallable(fun);
        return c.call(cx, scope, thisObj, new Object[] { arg0, arg1 });
    }

    /**
     * Implement ....(arg0, arg1, ...) call shrinking optimizer code.
     */
    public static Object callN(Object fun, Object thisObj,
                               Object[] args,
                               Context cx, Scriptable scope)
    {
        Callable c = ensureCallable(fun);
        return c.call(cx, scope, thisObj, args);
    }

    /**
     * Implement name(args) call shrinking optimizer code.
     */
    public static Object callName(Object[] args, String name,
                                  Context cx, Scriptable scope)
    {
        Object f = getNameObjectAndThis(name, cx, scope);
        Object thisObj = lastStoredThis(cx);
        thisObj = (thisObj != null ? thisObj : Undefined.instance);
        Callable c = ensureCallable(f);
        return c.call(cx, scope, thisObj, args);
    }

    /**
     * Implement name() call shrinking optimizer code.
     */
    public static Object callName0(String name,
                                   Context cx, Scriptable scope)
    {
        Object f = getNameObjectAndThis(name, cx, scope);
        Object thisObj = lastStoredThis(cx);
        thisObj = (thisObj != null ? thisObj : Undefined.instance);
        Callable c = ensureCallable(f);
        return c.call(cx, scope, thisObj, ScriptRuntime.emptyArgs);
    }

    /**
     * Implement x.property() call shrinking optimizer code.
     */
    public static Object callProp0(Object value, String property,
                                   Context cx, Scriptable scope)
    {
        Object f = getPropObjectAndThis(value, property, cx, scope);
        // ignore stored this
        Object thisObj = lastStoredThis(cx);
        thisObj = value;
        Callable c = ensureCallable(f);
        return c.call(cx, scope, thisObj, ScriptRuntime.emptyArgs);
    }

    /**
     * Implement ....(arg0, arg1, ...) call-ref shrinking optimizer code.
     */
    public static Ref callRef(Object fun, Scriptable thisObj,
                              Object[] args, Context cx, Scriptable scope)
    {
        Callable c = ensureCallable(fun);
        return callRef(c, scope, thisObj, args, cx);
    }

    public static Object add(Object val1, double val2)
    {
        if (val1 instanceof Scriptable)
            val1 = ((Scriptable) val1).getDefaultValue(null);
        if (!(val1 instanceof CharSequence))
            return wrapDouble(toNumber(val1) + val2);
        return new ConsString((CharSequence)val1, toString(val2));
    }

    public static Object add(double val1, Object val2)
    {
        if (val2 instanceof Scriptable)
            val2 = ((Scriptable) val2).getDefaultValue(null);
        if (!(val2 instanceof CharSequence))
            return wrapDouble(toNumber(val2) + val1);
        return new ConsString(toString(val1), (CharSequence)val2);
    }

    public static Object elemIncrDecr(Object obj, double index, boolean strict,
                                      Context cx, Scriptable scope,
                                      int incrDecrMask)
    {
        return ScriptRuntime.elemIncrDecr(obj, new Double(index), strict,
                                          cx, scope, incrDecrMask);
    }

    public static Object[] padStart(Object[] currentArgs, int count) {
        Object[] result = new Object[currentArgs.length + count];
        System.arraycopy(currentArgs, 0, result, count, currentArgs.length);
        return result;
    }

    public static void initFunction(NativeFunction fn, int functionType,
                                    Scriptable scope, Context cx,
                                    boolean strict)
    {
        ScriptRuntime.initFunction(cx, scope, fn, functionType, false, strict);
    }

    public static Object callSpecial(Context cx, Object fun,
                                     Object thisObj, Object[] args,
                                     Scriptable scope,
                                     Object callerThis, int callType,
                                     String fileName, int lineNumber,
                                     boolean strictMode)
    {
        Callable c = ensureCallable(fun);
        return ScriptRuntime.callSpecial(cx, c, thisObj, args, scope,
                                         callerThis, callType,
                                         fileName, lineNumber, strictMode);
    }

    public static Object newObjectSpecial(Context cx, Object fun,
                                          Object[] args, Scriptable scope,
                                          Object callerThis, int callType)
    {
        return ScriptRuntime.newSpecial(cx, fun, args, scope, callType);
    }

    public static Double wrapDouble(double num)
    {
        if (num == 0.0) {
            if (1 / num > 0) {
                // +0.0
                return zeroObj;
            }
        } else if (num == 1.0) {
            return oneObj;
        } else if (num == -1.0) {
            return minusOneObj;
        } else if (num != num) {
            return NaNobj;
        }
        return new Double(num);
    }

    static String encodeIntArray(int[] array)
    {
        // XXX: this extremely inefficient for small integers
        if (array == null) { return null; }
        int n = array.length;
        char[] buffer = new char[1 + n * 2];
        buffer[0] = 1;
        for (int i = 0; i != n; ++i) {
            int value = array[i];
            int shift = 1 + i * 2;
            buffer[shift] = (char)(value >>> 16);
            buffer[shift + 1] = (char)value;
        }
        return new String(buffer);
    }

    private static int[] decodeIntArray(String str, int arraySize)
    {
        // XXX: this extremely inefficient for small integers
        if (arraySize == 0) {
            if (str != null) throw new IllegalArgumentException();
            return null;
        }
        if (str.length() != 1 + arraySize * 2 && str.charAt(0) != 1) {
            throw new IllegalArgumentException();
        }
        int[] array = new int[arraySize];
        for (int i = 0; i != arraySize; ++i) {
            int shift = 1 + i * 2;
            array[i] = (str.charAt(shift) << 16) | str.charAt(shift + 1);
        }
        return array;
    }

    public static Scriptable newArrayLiteral(Object[] objects,
                                             String encodedInts,
                                             int skipCount,
                                             Context cx,
                                             Scriptable scope)
    {
        int[] skipIndexces = decodeIntArray(encodedInts, skipCount);
        return newArrayLiteral(objects, skipIndexces, cx, scope);
    }

    public static void main(final Script script, final String[] args)
    {
        ContextFactory.getGlobal().call(new ContextAction() {
            public Object run(Context cx)
            {
                ScriptableObject global = getGlobal(cx);

                // get the command line arguments and define "arguments"
                // array in the top-level object
                Object[] argsCopy = new Object[args.length];
                System.arraycopy(args, 0, argsCopy, 0, args.length);
                Scriptable argsObj = cx.newArray(global, argsCopy);
                global.defineProperty("arguments", argsObj,
                                      ScriptableObject.DONTENUM, false);
                script.exec(cx, global);
                return null;
            }
        });
    }

    public static void throwStopIteration(Object obj) {
        throw new JavaScriptException(
            NativeIterator.getStopIterationObject((Scriptable)obj), "", 0);
    }

    public static Scriptable createNativeGenerator(NativeFunction funObj,
                                                   Scriptable scope,
                                                   Object thisObj,
                                                   int maxLocals,
                                                   int maxStack)
    {
        return new NativeGenerator(scope, funObj,
                new GeneratorState(thisObj, maxLocals, maxStack));
    }

    public static Object[] getGeneratorStackState(Object obj) {
        GeneratorState rgs = (GeneratorState) obj;
        if (rgs.stackState == null)
            rgs.stackState = new Object[rgs.maxStack];
        return rgs.stackState;
    }

    public static Object[] getGeneratorLocalsState(Object obj) {
        GeneratorState rgs = (GeneratorState) obj;
        if (rgs.localsState == null)
            rgs.localsState = new Object[rgs.maxLocals];
        return rgs.localsState;
    }

    public static class GeneratorState {
        static final String CLASS_NAME =
            "org/mozilla/javascript/optimizer/OptRuntime$GeneratorState";

        public int resumptionPoint;
        static final String resumptionPoint_NAME = "resumptionPoint";
        static final String resumptionPoint_TYPE = "I";

        public Object thisObj;
        static final String thisObj_NAME = "thisObj";
        static final String thisObj_TYPE = "Ljava/lang/Object;";

        Object[] stackState;
        Object[] localsState;
        int maxLocals;
        int maxStack;

        GeneratorState(Object thisObj, int maxLocals, int maxStack) {
            this.thisObj = thisObj;
            this.maxLocals = maxLocals;
            this.maxStack = maxStack;
        }
    }
}
