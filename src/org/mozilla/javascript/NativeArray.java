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
 *   Mike McCabe
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * This class implements the Array native object.
 * @author Norris Boyd
 * @author Mike McCabe
 */
public class NativeArray extends ScriptableObject implements IdFunctionCall, List
{
    static final long serialVersionUID = 7331366857676127338L;

    /*
     * Optimization possibilities and open issues:
     * - Long vs. double schizophrenia.  I suspect it might be better
     * to use double throughout.
     *
     * - Functions that need a new Array call "new Array" in the
     * current scope rather than using a hardwired constructor;
     * "Array" could be redefined.  It turns out that js calls the
     * equivalent of "new Array" in the current scope, except that it
     * always gets at least an object back, even when Array == null.
     */

    private static final Integer NEGATIVE_ONE = Integer.valueOf(-1);

    static void init(Scriptable scope, boolean sealed)
    {
        NativeArray proto = new NativeArray(0);
        proto.setParentScope(scope);
        proto.setPrototype(getObjectPrototype(scope));
        IdFunctionObject ctor = null;
        for (Methods method : Methods.values()) {
            IdFunctionObject idfun = new IdFunctionObject(proto, method,
                    0, method.name(), method.arity, scope);
            idfun.addAsProperty(proto);
            if (method == Methods.constructor) {
                ctor = idfun;
                ctor.initFunction(proto.getClassName(), scope);
                ctor.markAsConstructor(proto);
                ctor.exportAsScopeProperty();
            }
        }
        for (StaticMethods method : StaticMethods.values()) {
            IdFunctionObject idfun = new IdFunctionObject(proto, method,
                    0, method.name(),  method.arity, scope);
            idfun.addAsProperty(ctor);
        }
        if (sealed) {
            proto.sealObject();
            ctor.sealObject();
        }
    }

    static int getMaximumInitialCapacity() {
        return maximumInitialCapacity;
    }

    static void setMaximumInitialCapacity(int maximumInitialCapacity) {
        NativeArray.maximumInitialCapacity = maximumInitialCapacity;
    }

    public NativeArray(long lengthArg)
    {
        denseOnly = lengthArg <= maximumInitialCapacity;
        if (denseOnly) {
            int intLength = (int) lengthArg;
            if (intLength < DEFAULT_INITIAL_CAPACITY)
                intLength = DEFAULT_INITIAL_CAPACITY;
            dense = new Object[intLength];
            Arrays.fill(dense, Scriptable.NOT_FOUND);
        }
        length = lengthArg;
    }

    public NativeArray(Object[] array)
    {
        denseOnly = true;
        dense = array;
        length = array.length;
    }

    @Override
    public String getClassName()
    {
        return "Array";
    }



    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
                             Scriptable thisObj, Object[] args)
    {
        Object tag = f.getTag();
        Methods method = null;

        if (tag instanceof Methods) {
            method = (Methods) tag;
        } else if (tag instanceof StaticMethods) {
            StaticMethods staticMethod = (StaticMethods) tag;

            switch (staticMethod) {
                case join:
                case reverse:
                case sort:
                case push:
                case pop:
                case shift:
                case unshift:
                case splice:
                case concat:
                case slice:
                case indexOf:
                case lastIndexOf:
                case every:
                case filter:
                case forEach:
                case map:
                case some:
                case reduce:
                case reduceRight:
                    if (args.length > 0) {
                        thisObj = ScriptRuntime.toObject(scope, args[0]);
                        Object[] newArgs = new Object[args.length-1];
                        for (int i=0; i < newArgs.length; i++)
                            newArgs[i] = args[i+1];
                        args = newArgs;
                    }
                    method = staticMethod.instanceMethod;
                    break;
                    // Continue to instance method switch below

                case isArray:
                    return Boolean.valueOf(
                            args.length > 0 && (args[0] instanceof NativeArray));

            }
        }

        if (method != null) {
            switch (method) {
                case constructor: {
                    boolean inNewExpr = (thisObj == null);
                    if (!inNewExpr) {
                        // IdFunctionObject.construct will set up parent, proto
                        return f.construct(cx, scope, args);
                    }
                    return jsConstructor(cx, scope, args);
                }

                case toString:
                    return toStringHelper(cx, scope, thisObj,
                            cx.hasFeature(Context.FEATURE_TO_STRING_AS_SOURCE), false);

                case toLocaleString:
                    return toStringHelper(cx, scope, thisObj, false, true);

                case toSource:
                    return toStringHelper(cx, scope, thisObj, true, false);

                case join:
                    return js_join(cx, thisObj, args);

                case reverse:
                    return js_reverse(cx, thisObj, args);

                case sort:
                    return js_sort(cx, scope, thisObj, args);

                case push:
                    return js_push(cx, thisObj, args);

                case pop:
                    return js_pop(cx, thisObj, args);

                case shift:
                    return js_shift(cx, thisObj, args);

                case unshift:
                    return js_unshift(cx, thisObj, args);

                case splice:
                    return js_splice(cx, scope, thisObj, args);

                case concat:
                    return js_concat(cx, scope, thisObj, args);

                case slice:
                    return js_slice(cx, thisObj, args);

                case indexOf:
                    return indexOfHelper(cx, thisObj, args, false);

                case lastIndexOf:
                    return indexOfHelper(cx, thisObj, args, true);

                case every:
                case filter:
                case forEach:
                case map:
                case some:
                    return iterativeMethod(cx, method, scope, thisObj, args);
                case reduce:
                case reduceRight:
                    return reduceMethod(cx, method, scope, thisObj, args);
            }
        }
        throw new IllegalArgumentException(String.valueOf(tag));
    }

    @Override
    public Object get(int index, Scriptable start)
    {
        if (!denseOnly && isGetterOrSetter(null, index, false))
            return super.get(index, start);
        if (dense != null && 0 <= index && index < dense.length)
            return dense[index];
        return super.get(index, start);
    }

    @Override
    public boolean has(int index, Scriptable start)
    {
        if (!denseOnly && isGetterOrSetter(null, index, false))
            return super.has(index, start);
        if (dense != null && 0 <= index && index < dense.length)
            return dense[index] != NOT_FOUND;
        return super.has(index, start);
    }

    private static long toArrayIndex(Object id) {
        if (id instanceof String) {
            return toArrayIndex((String)id);
        } else if (id instanceof Number) {
            return toArrayIndex(((Number)id).doubleValue());
        }
        return -1;
    }

    // if id is an array index (ECMA 15.4.0), return the number,
    // otherwise return -1L
    private static long toArrayIndex(String id)
    {
        long index = toArrayIndex(ScriptRuntime.toNumber(id));
        // Assume that ScriptRuntime.toString(index) is the same
        // as java.lang.Long.toString(index) for long
        if (Long.toString(index).equals(id)) {
            return index;
        }
        return -1;
    }

    private static long toArrayIndex(double d) {
        if (d == d) {
            long index = ScriptRuntime.toUint32(d);
            if (index == d && index != 4294967295L) {
                return index;
            }
        }
        return -1;
    }

    private static int toDenseIndex(Object id) {
      long index = toArrayIndex(id);
      return 0 <= index && index < Integer.MAX_VALUE ? (int) index : -1;
    }

    @Override
    public Object get(String name, Scriptable start) {
        if ("length".equals(name)) {
            if (boxedLength == null || boxedLength.longValue() != length) {
                boxedLength =  ScriptRuntime.wrapNumber(length);
            }
            return boxedLength;
        }
        return super.get(name, start);
    }

    @Override
    public void put(String id, Scriptable start, Object value)
    {
        if ("length".equals(id) && start == this) {
            setLength(value);
            return;
        }
        super.put(id, start, value);
        if (start == this) {
            // If the object is sealed, super will throw exception
            long index = toArrayIndex(id);
            if (index >= length) {
                length = index + 1;
                denseOnly = false;
            }
        }
    }

    @Override
    public boolean has(String name, Scriptable start) {
        return "length".equals(name) || super.has(name, start);
    }

    private boolean ensureCapacity(int capacity)
    {
        if (capacity > dense.length) {
            if (capacity > MAX_PRE_GROW_SIZE) {
                denseOnly = false;
                return false;
            }
            capacity = Math.max(capacity, (int)(dense.length * GROW_FACTOR));
            Object[] newDense = new Object[capacity];
            System.arraycopy(dense, 0, newDense, 0, dense.length);
            Arrays.fill(newDense, dense.length, newDense.length,
                        Scriptable.NOT_FOUND);
            dense = newDense;
        }
        return true;
    }

    @Override
    public void put(int index, Scriptable start, Object value)
    {
        if (start == this && !isSealed() && dense != null && 0 <= index &&
            (denseOnly || !isGetterOrSetter(null, index, true)))
        {
            if (index < dense.length) {
                dense[index] = value;
                if (this.length <= index)
                    this.length = (long)index + 1;
                return;
            } else if (denseOnly && index < dense.length * GROW_FACTOR &&
                       ensureCapacity(index+1))
            {
                dense[index] = value;
                this.length = (long)index + 1;
                return;
            } else {
                denseOnly = false;
            }
        }
        super.put(index, start, value);
        if (start == this && (lengthAttr & READONLY) == 0) {
            // only set the array length if given an array index (ECMA 15.4.0)
            if (this.length <= index) {
                // avoid overflowing index!
                this.length = (long)index + 1;
            }
        }
    }

    @Override
    public void delete(int index)
    {
        if (dense != null && 0 <= index && index < dense.length &&
            !isSealed() && (denseOnly || !isGetterOrSetter(null, index, true)))
        {
            dense[index] = NOT_FOUND;
        } else {
            super.delete(index);
        }
    }

    @Override
    public Object[] getIds()
    {
        Object[] superIds = super.getIds();
        if (dense == null) { return superIds; }
        int N = dense.length;
        long currentLength = length;
        if (N > currentLength) {
            N = (int)currentLength;
        }
        if (N == 0) { return superIds; }
        int superLength = superIds.length;
        Object[] ids = new Object[N + superLength];

        int presentCount = 0;
        for (int i = 0; i != N; ++i) {
            // Replace existing elements by their indexes
            if (dense[i] != NOT_FOUND) {
                ids[presentCount] = Integer.valueOf(i);
                ++presentCount;
            }
        }
        if (presentCount != N) {
            // dense contains deleted elems, need to shrink the result
            Object[] tmp = new Object[presentCount + superLength];
            System.arraycopy(ids, 0, tmp, 0, presentCount);
            ids = tmp;
        }
        System.arraycopy(superIds, 0, ids, presentCount, superLength);
        return ids;
    }

    @Override
    public Object[] getAllIds()
    {
      Set<Object> allIds = new LinkedHashSet<Object>(
            Arrays.asList(this.getIds()));
      allIds.add("length");
      allIds.addAll(Arrays.asList(super.getAllIds()));
      return allIds.toArray();
    }

    public Integer[] getIndexIds() {
      Object[] ids = getIds();
      java.util.List<Integer> indices = new java.util.ArrayList<Integer>(ids.length);
      for (Object id : ids) {
        int int32Id = ScriptRuntime.toInt32(id);
        if (int32Id >= 0 && ScriptRuntime.toString(int32Id).equals(ScriptRuntime.toString(id))) {
          indices.add(int32Id);
        }
      }
      return indices.toArray(new Integer[indices.size()]);
    }

    @Override
    public Object getDefaultValue(Class<?> hint)
    {
        if (hint == ScriptRuntime.NumberClass) {
            Context cx = Context.getContext();
            if (cx.getLanguageVersion() == Context.VERSION_1_2)
                return Long.valueOf(length);
        }
        return super.getDefaultValue(hint);
    }

    private ScriptableObject defaultIndexPropertyDescriptor(Object value) {
      Scriptable scope = getParentScope();
      if (scope == null) scope = this;
      ScriptableObject desc = new NativeObject();
      ScriptRuntime.setBuiltinProtoAndParent(desc, scope, TopLevel.Builtins.Object);
      desc.defineProperty("value", value, EMPTY);
      desc.defineProperty("writable", true, EMPTY);
      desc.defineProperty("enumerable", true, EMPTY);
      desc.defineProperty("configurable", true, EMPTY);
      return desc;
    }

    @Override
    public int getAttributes(String name) {
        if (name.equals("length")) {
            return lengthAttr;
        }
        return super.getAttributes(name);
    }

    @Override
    public void setAttributes(String name, int attributes) {
        if (name.equals("length")) {
            lengthAttr = attributes;
            return;
        }
        super.setAttributes(name, attributes);
    }

    @Override
    public int getAttributes(int index) {
        if (dense != null && index >= 0 && index < dense.length
                && dense[index] != NOT_FOUND) {
            return EMPTY;
        }
        return super.getAttributes(index);
    }

    @Override
    protected ScriptableObject getOwnPropertyDescriptor(Context cx, Object id) {
      if ("length".equals(id)) {
          return buildDataDescriptor(getParentScope(),
                  ScriptRuntime.wrapNumber(length),
                  lengthAttr);
      }
      if (dense != null) {
        int index = toDenseIndex(id);
        if (0 <= index && index < dense.length && dense[index] != NOT_FOUND) {
          Object value = dense[index];
          return defaultIndexPropertyDescriptor(value);
        }
      }
      return super.getOwnPropertyDescriptor(cx, id);
    }

    @Override
    protected void defineOwnProperty(Context cx, Object id,
                                     ScriptableObject desc,
                                     boolean checkValid) {
      if (id.equals("length")) {
        checkPropertyDefinition(desc);
        if (checkValid) {
          ScriptableObject current = getOwnPropertyDescriptor(cx, "length");
          checkPropertyChange("length", current, desc);
        }
        Object value = getProperty(desc, "value");
        if (value != NOT_FOUND) {
          setLength(value);
        }
        lengthAttr = applyDescriptorToAttributeBitset(lengthAttr, desc);
        return;
      }
      if (dense != null) {
        Object[] values = dense;
        dense = null;
        denseOnly = false;
        for (int i = 0; i < values.length; i++) {
          if (values[i] != NOT_FOUND) {
            put(i, this, values[i]);
          }
        }
      }
      long index = toArrayIndex(id);
      if (index >= length) {
        length = index + 1;
      }
      super.defineOwnProperty(cx, id, desc, checkValid);
    }

    /**
     * See ECMA 15.4.1,2
     */
    private static Object jsConstructor(Context cx, Scriptable scope,
                                        Object[] args)
    {
        if (args.length == 0)
            return new NativeArray(0);

        // Only use 1 arg as first element for version 1.2; for
        // any other version (including 1.3) follow ECMA and use it as
        // a length.
        if (cx.getLanguageVersion() == Context.VERSION_1_2) {
            return new NativeArray(args);
        } else {
            Object arg0 = args[0];
            if (args.length > 1 || !(arg0 instanceof Number)) {
                return new NativeArray(args);
            } else {
                long len = ScriptRuntime.toUint32(arg0);
                if (len != ((Number)arg0).doubleValue()) {
                    String msg = ScriptRuntime.getMessage0("msg.arraylength.bad");
                    throw ScriptRuntime.constructError("RangeError", msg);
                }
                return new NativeArray(len);
            }
        }
    }

    public long getLength() {
        return length;
    }

    /** @deprecated Use {@link #getLength()} instead. */
    public long jsGet_length() {
        return getLength();
    }

    /**
     * Change the value of the internal flag that determines whether all
     * storage is handed by a dense backing array rather than an associative
     * store.
     * @param denseOnly new value for denseOnly flag
     * @throws IllegalArgumentException if an attempt is made to enable
     *   denseOnly after it was disabled; NativeArray code is not written
     *   to handle switching back to a dense representation
     */
    void setDenseOnly(boolean denseOnly) {
        if (denseOnly && !this.denseOnly)
            throw new IllegalArgumentException();
        this.denseOnly = denseOnly;
    }

    private void setLength(Object val) {
        /* XXX do we satisfy this?
         * 15.4.5.1 [[Put]](P, V):
         * 1. Call the [[CanPut]] method of A with name P.
         * 2. If Result(1) is false, return.
         * ?
         */
        if ((lengthAttr & READONLY) != 0) {
            return;
        }

        double d = ScriptRuntime.toNumber(val);
        long longVal = ScriptRuntime.toUint32(d);
        if (longVal != d) {
            String msg = ScriptRuntime.getMessage0("msg.arraylength.bad");
            throw ScriptRuntime.constructError("RangeError", msg);
        }

        if (denseOnly) {
            if (longVal < length) {
                // downcast okay because denseOnly
                Arrays.fill(dense, (int) longVal, dense.length, NOT_FOUND);
                length = longVal;
                return;
            } else if (longVal < MAX_PRE_GROW_SIZE &&
                       longVal < (length * GROW_FACTOR) &&
                       ensureCapacity((int)longVal))
            {
                length = longVal;
                return;
            } else {
                denseOnly = false;
            }
        }
        if (longVal < length) {
            // remove all properties between longVal and length
            if (length - longVal > 0x1000) {
                // assume that the representation is sparse
                Object[] e = getIds(); // will only find in object itself
                for (int i=0; i < e.length; i++) {
                    Object id = e[i];
                    if (id instanceof String) {
                        // > MAXINT will appear as string
                        String strId = (String)id;
                        long index = toArrayIndex(strId);
                        if (index >= longVal)
                            delete(strId);
                    } else {
                        int index = ((Integer)id).intValue();
                        if (index >= longVal)
                            delete(index);
                    }
                }
            } else {
                // assume a dense representation
                for (long i = longVal; i < length; i++) {
                    deleteElem(this, i);
                }
            }
        }
        length = longVal;
    }

    /* Support for generic Array-ish objects.  Most of the Array
     * functions try to be generic; anything that has a length
     * property is assumed to be an array.
     * getLengthProperty returns 0 if obj does not have the length property
     * or its value is not convertible to a number.
     */
    static long getLengthProperty(Context cx, Scriptable obj) {
        // These will both give numeric lengths within Uint32 range.
        if (obj instanceof NativeString) {
            return ((NativeString)obj).getLength();
        } else if (obj instanceof NativeArray) {
            return ((NativeArray)obj).getLength();
        }
        return ScriptRuntime.toUint32(
            ScriptRuntime.getObjectProp(obj, "length", cx));
    }

    private static Object setLengthProperty(Context cx, Scriptable target,
                                            long length)
    {
        return ScriptRuntime.setObjectProp(
                   target, "length", ScriptRuntime.wrapNumber(length), cx);
    }

    /* Utility functions to encapsulate index > Integer.MAX_VALUE
     * handling.  Also avoids unnecessary object creation that would
     * be necessary to use the general ScriptRuntime.get/setElem
     * functions... though this is probably premature optimization.
     */
    private static void deleteElem(Scriptable target, long index) {
        int i = (int)index;
        if (i == index) { target.delete(i); }
        else { target.delete(Long.toString(index)); }
    }

    private static Object getElem(Context cx, Scriptable target, long index)
    {
        if (index > Integer.MAX_VALUE) {
            String id = Long.toString(index);
            return ScriptRuntime.getObjectProp(target, id, cx);
        } else {
            return ScriptRuntime.getObjectIndex(target, (int)index, cx);
        }
    }

    // same as getElem, but without converting NOT_FOUND to undefined
    private static Object getRawElem(Scriptable target, long index) {
        if (index > Integer.MAX_VALUE) {
            return ScriptableObject.getProperty(target, Long.toString(index));
        } else {
            return ScriptableObject.getProperty(target, (int)index);
        }
    }

    private static void setElem(Context cx, Scriptable target, long index,
                                Object value)
    {
        if (index > Integer.MAX_VALUE) {
            String id = Long.toString(index);
            ScriptRuntime.setObjectProp(target, id, value, cx);
        } else {
            ScriptRuntime.setObjectIndex(target, (int)index, value, cx);
        }
    }

    // Similar as setElem(), but triggers deleteElem() if value is NOT_FOUND
    private static void setRawElem(Context cx, Scriptable target, long index,
                                   Object value) {
        if (value == NOT_FOUND) {
            deleteElem(target, index);
        } else {
            setElem(cx, target, index, value);
        }
    }

    private static String toStringHelper(Context cx, Scriptable scope,
                                         Scriptable thisObj,
                                         boolean toSource, boolean toLocale)
    {
        /* It's probably redundant to handle long lengths in this
         * function; StringBuilders are limited to 2^31 in java.
         */

        long length = getLengthProperty(cx, thisObj);

        StringBuilder result = new StringBuilder(256);

        // whether to return '4,unquoted,5' or '[4, "quoted", 5]'
        String separator;

        if (toSource) {
            result.append('[');
            separator = ", ";
        } else {
            separator = ",";
        }

        boolean haslast = false;
        long i = 0;

        boolean toplevel, iterating;
        if (cx.iterating == null) {
            toplevel = true;
            iterating = false;
            cx.iterating = new ObjToIntMap(31);
        } else {
            toplevel = false;
            iterating = cx.iterating.has(thisObj);
        }

        // Make sure cx.iterating is set to null when done
        // so we don't leak memory
        try {
            if (!iterating) {
                cx.iterating.put(thisObj, 0); // stop recursion.
                for (i = 0; i < length; i++) {
                    if (i > 0) result.append(separator);
                    Object elem = getElem(cx, thisObj, i);
                    if (elem == null || elem == Undefined.instance) {
                        haslast = false;
                        continue;
                    }
                    haslast = true;

                    if (toSource) {
                        result.append(ScriptRuntime.uneval(cx, scope, elem));

                    } else if (elem instanceof String) {
                        String s = (String)elem;
                        if (toSource) {
                            result.append('\"');
                            result.append(ScriptRuntime.escapeString(s));
                            result.append('\"');
                        } else {
                            result.append(s);
                        }

                    } else {
                        if (toLocale)
                        {
                            Callable fun;
                            Scriptable funThis;
                            fun = ScriptRuntime.getPropFunctionAndThis(
                                      elem, "toLocaleString", cx);
                            funThis = ScriptRuntime.lastStoredScriptable(cx);
                            elem = fun.call(cx, scope, funThis,
                                            ScriptRuntime.emptyArgs);
                        }
                        result.append(ScriptRuntime.toString(elem));
                    }
                }
            }
        } finally {
            if (toplevel) {
                cx.iterating = null;
            }
        }

        if (toSource) {
            //for [,,].length behavior; we want toString to be symmetric.
            if (!haslast && i > 0)
                result.append(", ]");
            else
                result.append(']');
        }
        return result.toString();
    }

    /**
     * See ECMA 15.4.4.3
     */
    private static String js_join(Context cx, Scriptable thisObj,
                                  Object[] args)
    {
        long llength = getLengthProperty(cx, thisObj);
        int length = (int)llength;
        if (llength != length) {
            throw Context.reportRuntimeError1(
                "msg.arraylength.too.big", String.valueOf(llength));
        }
        // if no args, use "," as separator
        String separator = (args.length < 1 || args[0] == Undefined.instance)
                           ? ","
                           : ScriptRuntime.toString(args[0]);
        if (thisObj instanceof NativeArray) {
            NativeArray na = (NativeArray) thisObj;
            if (na.denseOnly) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < length; i++) {
                    if (i != 0) {
                        sb.append(separator);
                    }
                    if (i < na.dense.length) {
                        Object temp = na.dense[i];
                        if (temp != null && temp != Undefined.instance &&
                            temp != Scriptable.NOT_FOUND)
                        {
                            sb.append(ScriptRuntime.toString(temp));
                        }
                    }
                }
                return sb.toString();
            }
        }
        if (length == 0) {
            return "";
        }
        String[] buf = new String[length];
        int total_size = 0;
        for (int i = 0; i != length; i++) {
            Object temp = getElem(cx, thisObj, i);
            if (temp != null && temp != Undefined.instance) {
                String str = ScriptRuntime.toString(temp);
                total_size += str.length();
                buf[i] = str;
            }
        }
        total_size += (length - 1) * separator.length();
        StringBuilder sb = new StringBuilder(total_size);
        for (int i = 0; i != length; i++) {
            if (i != 0) {
                sb.append(separator);
            }
            String str = buf[i];
            if (str != null) {
                // str == null for undefined or null
                sb.append(str);
            }
        }
        return sb.toString();
    }

    /**
     * See ECMA 15.4.4.4
     */
    private static Scriptable js_reverse(Context cx, Scriptable thisObj,
                                         Object[] args)
    {
        if (thisObj instanceof NativeArray) {
            NativeArray na = (NativeArray) thisObj;
            if (na.denseOnly) {
                for (int i=0, j=((int)na.length)-1; i < j; i++,j--) {
                    Object temp = na.dense[i];
                    na.dense[i] = na.dense[j];
                    na.dense[j] = temp;
                }
                return thisObj;
            }
        }
        long len = getLengthProperty(cx, thisObj);

        long half = len / 2;
        for(long i=0; i < half; i++) {
            long j = len - i - 1;
            Object temp1 = getRawElem(thisObj, i);
            Object temp2 = getRawElem(thisObj, j);
            setRawElem(cx, thisObj, i, temp2);
            setRawElem(cx, thisObj, j, temp1);
        }
        return thisObj;
    }

    /**
     * See ECMA 15.4.4.5
     */
    private static Scriptable js_sort(final Context cx, final Scriptable scope,
            final Scriptable thisObj, final Object[] args)
    {
        final Comparator<Object> comparator;
        if (args.length > 0 && Undefined.instance != args[0]) {
            final Callable jsCompareFunction = ScriptRuntime
                    .getValueFunctionAndThis(args[0], cx);
            final Scriptable funThis = ScriptRuntime.lastStoredScriptable(cx);
            final Object[] cmpBuf = new Object[2]; // Buffer for cmp arguments
            comparator = new Comparator<Object>() {
                public int compare(final Object x, final Object y) {
                    // sort undefined to end
                    if (x == y) {
                        return 0;
                    } else if (y == Undefined.instance
                            || y == Scriptable.NOT_FOUND) {
                        return -1;
                    } else if (x == Undefined.instance
                            || x == Scriptable.NOT_FOUND) {
                        return 1;
                    }

                    cmpBuf[0] = x;
                    cmpBuf[1] = y;
                    Object ret = jsCompareFunction.call(cx, scope, funThis,
                            cmpBuf);
                    final double d = ScriptRuntime.toNumber(ret);
                    if (d < 0) {
                        return -1;
                    } else if (d > 0) {
                        return +1;
                    }
                    return 0; // ??? double and 0???
                }
            };
        } else {
            comparator = new Comparator<Object>() {
                public int compare(final Object x, final Object y) {
                    // sort undefined to end
                    if (x == y)
                        return 0;
                    else if (y == Undefined.instance
                            || y == Scriptable.NOT_FOUND) {
                        return -1;
                    } else if (x == Undefined.instance
                            || x == Scriptable.NOT_FOUND) {
                        return 1;
                    }

                    final String a = ScriptRuntime.toString(x);
                    final String b = ScriptRuntime.toString(y);
                    return a.compareTo(b);
                }
            };
        }

        final int length = (int) getLengthProperty(cx, thisObj);
        // copy the JS array into a working array, so it can be
        // sorted cheaply.
        final Object[] working = new Object[length];
        for (int i = 0; i != length; ++i) {
            working[i] = getElem(cx, thisObj, i);
        }

        Arrays.sort(working, comparator);

        // copy the working array back into thisObj
        for (int i = 0; i < length; ++i) {
            setElem(cx, thisObj, i, working[i]);
        }

        return thisObj;
    }

    /**
     * Non-ECMA methods.
     */

    private static Object js_push(Context cx, Scriptable thisObj,
                                  Object[] args)
    {
        if (thisObj instanceof NativeArray) {
            NativeArray na = (NativeArray) thisObj;
            if (na.denseOnly &&
                na.ensureCapacity((int) na.length + args.length))
            {
                for (int i = 0; i < args.length; i++) {
                    na.dense[(int)na.length++] = args[i];
                }
                return ScriptRuntime.wrapNumber(na.length);
            }
        }
        long length = getLengthProperty(cx, thisObj);
        for (int i = 0; i < args.length; i++) {
            setElem(cx, thisObj, length + i, args[i]);
        }

        length += args.length;
        Object lengthObj = setLengthProperty(cx, thisObj, length);

        /*
         * If JS1.2, follow Perl4 by returning the last thing pushed.
         * Otherwise, return the new array length.
         */
        if (cx.getLanguageVersion() == Context.VERSION_1_2)
            // if JS1.2 && no arguments, return undefined.
            return args.length == 0
                ? Undefined.instance
                : args[args.length - 1];

        else
            return lengthObj;
    }

    private static Object js_pop(Context cx, Scriptable thisObj,
                                 Object[] args)
    {
        Object result;
        if (thisObj instanceof NativeArray) {
            NativeArray na = (NativeArray) thisObj;
            if (na.denseOnly && na.length > 0) {
                na.length--;
                result = na.dense[(int)na.length];
                na.dense[(int)na.length] = NOT_FOUND;
                return result;
            }
        }
        long length = getLengthProperty(cx, thisObj);
        if (length > 0) {
            length--;

            // Get the to-be-deleted property's value.
            result = getElem(cx, thisObj, length);

            // We don't need to delete the last property, because
            // setLength does that for us.
        } else {
            result = Undefined.instance;
        }
        // necessary to match js even when length < 0; js pop will give a
        // length property to any target it is called on.
        setLengthProperty(cx, thisObj, length);

        return result;
    }

    private static Object js_shift(Context cx, Scriptable thisObj,
                                   Object[] args)
    {
        if (thisObj instanceof NativeArray) {
            NativeArray na = (NativeArray) thisObj;
            if (na.denseOnly && na.length > 0) {
                na.length--;
                Object result = na.dense[0];
                System.arraycopy(na.dense, 1, na.dense, 0, (int)na.length);
                na.dense[(int)na.length] = NOT_FOUND;
                return result == NOT_FOUND ? Undefined.instance : result;
            }
        }
        Object result;
        long length = getLengthProperty(cx, thisObj);
        if (length > 0) {
            long i = 0;
            length--;

            // Get the to-be-deleted property's value.
            result = getElem(cx, thisObj, i);

            /*
             * Slide down the array above the first element.  Leave i
             * set to point to the last element.
             */
            if (length > 0) {
                for (i = 1; i <= length; i++) {
                    Object temp = getRawElem(thisObj, i);
                    setRawElem(cx, thisObj, i - 1, temp);
                }
            }
            // We don't need to delete the last property, because
            // setLength does that for us.
        } else {
            result = Undefined.instance;
        }
        setLengthProperty(cx, thisObj, length);
        return result;
    }

    private static Object js_unshift(Context cx, Scriptable thisObj,
                                     Object[] args)
    {
        if (thisObj instanceof NativeArray) {
            NativeArray na = (NativeArray) thisObj;
            if (na.denseOnly &&
                na.ensureCapacity((int)na.length + args.length))
            {
                System.arraycopy(na.dense, 0, na.dense, args.length,
                                 (int) na.length);
                for (int i = 0; i < args.length; i++) {
                    na.dense[i] = args[i];
                }
                na.length += args.length;
                return ScriptRuntime.wrapNumber(na.length);
            }
        }
        long length = getLengthProperty(cx, thisObj);
        int argc = args.length;

        if (args.length > 0) {
            /*  Slide up the array to make room for args at the bottom */
            if (length > 0) {
                for (long last = length - 1; last >= 0; last--) {
                    Object temp = getRawElem(thisObj, last);
                    setRawElem(cx, thisObj, last + argc, temp);
                }
            }

            /* Copy from argv to the bottom of the array. */
            for (int i = 0; i < args.length; i++) {
                setElem(cx, thisObj, i, args[i]);
            }

            /* Follow Perl by returning the new array length. */
            length += args.length;
            return setLengthProperty(cx, thisObj, length);
        }
        return ScriptRuntime.wrapNumber(length);
    }

    private static Object js_splice(Context cx, Scriptable scope,
                                    Scriptable thisObj, Object[] args)
    {
    	NativeArray na = null;
    	boolean denseMode = false;
        if (thisObj instanceof NativeArray) {
            na = (NativeArray) thisObj;
            denseMode = na.denseOnly;
        }

        /* create an empty Array to return. */
        scope = getTopLevelScope(scope);
        int argc = args.length;
        if (argc == 0)
            return cx.newArray(scope, 0);
        long length = getLengthProperty(cx, thisObj);

        /* Convert the first argument into a starting index. */
        long begin = toSliceIndex(ScriptRuntime.toInteger(args[0]), length);
        argc--;

        /* Convert the second argument into count */
        long count;
        if (args.length == 1) {
            count = length - begin;
        } else {
            double dcount = ScriptRuntime.toInteger(args[1]);
            if (dcount < 0) {
                count = 0;
            } else if (dcount > (length - begin)) {
                count = length - begin;
            } else {
                count = (long)dcount;
            }
            argc--;
        }

        long end = begin + count;

        /* If there are elements to remove, put them into the return value. */
        Object result;
        if (count != 0) {
            if (count == 1
                && (cx.getLanguageVersion() == Context.VERSION_1_2))
            {
                /*
                 * JS lacks "list context", whereby in Perl one turns the
                 * single scalar that's spliced out into an array just by
                 * assigning it to @single instead of $single, or by using it
                 * as Perl push's first argument, for instance.
                 *
                 * JS1.2 emulated Perl too closely and returned a non-Array for
                 * the single-splice-out case, requiring callers to test and
                 * wrap in [] if necessary.  So JS1.3, default, and other
                 * versions all return an array of length 1 for uniformity.
                 */
                result = getElem(cx, thisObj, begin);
            } else {
            	if (denseMode) {
                    int intLen = (int) (end - begin);
                    Object[] copy = new Object[intLen];
                    System.arraycopy(na.dense, (int) begin, copy, 0, intLen);
                    result = cx.newArray(scope, copy);
                } else {
                    Scriptable resultArray = cx.newArray(scope, 0);
                    for (long last = begin; last != end; last++) {
                        Object temp = getRawElem(thisObj, last);
                        if (temp != NOT_FOUND) {
                            setElem(cx, resultArray, last - begin, temp);
                        }
                    }
                    // Need to set length for sparse result array
                    setLengthProperty(cx, resultArray, end - begin);
                    result = resultArray;
            	}
            }
        } else { // (count == 0)
        	if (cx.getLanguageVersion() == Context.VERSION_1_2) {
                /* Emulate C JS1.2; if no elements are removed, return undefined. */
                result = Undefined.instance;
            } else {
                result = cx.newArray(scope, 0);
            }
        }

        /* Find the direction (up or down) to copy and make way for argv. */
        long delta = argc - count;
        if (denseMode && length + delta < Integer.MAX_VALUE &&
            na.ensureCapacity((int) (length + delta)))
        {
            System.arraycopy(na.dense, (int) end, na.dense,
                             (int) (begin + argc), (int) (length - end));
            if (argc > 0) {
                System.arraycopy(args, 2, na.dense, (int) begin, argc);
            }
            if (delta < 0) {
                Arrays.fill(na.dense, (int) (length + delta), (int) length,
                            NOT_FOUND);
            }
            na.length = length + delta;
            return result;
        }

        if (delta > 0) {
            for (long last = length - 1; last >= end; last--) {
                Object temp = getRawElem(thisObj, last);
                setRawElem(cx, thisObj, last + delta, temp);
            }
        } else if (delta < 0) {
            for (long last = end; last < length; last++) {
                Object temp = getRawElem(thisObj, last);
                setRawElem(cx, thisObj, last + delta, temp);
            }
        }

        /* Copy from argv into the hole to complete the splice. */
        int argoffset = args.length - argc;
        for (int i = 0; i < argc; i++) {
            setElem(cx, thisObj, begin + i, args[i + argoffset]);
        }

        /* Update length in case we deleted elements from the end. */
        setLengthProperty(cx, thisObj, length + delta);
        return result;
    }

    /*
     * See Ecma 262v3 15.4.4.4
     */
    private static Scriptable js_concat(Context cx, Scriptable scope,
                                        Scriptable thisObj, Object[] args)
    {
        // create an empty Array to return.
        scope = getTopLevelScope(scope);
        Function ctor = ScriptRuntime.getExistingCtor(scope, "Array");
        Scriptable result = ctor.construct(cx, scope, ScriptRuntime.emptyArgs);
        if (thisObj instanceof NativeArray && result instanceof NativeArray) {
            NativeArray denseThis = (NativeArray) thisObj;
            NativeArray denseResult = (NativeArray) result;
            if (denseThis.denseOnly && denseResult.denseOnly) {
                // First calculate length of resulting array
                boolean canUseDense = true;
                int length = (int) denseThis.length;
                for (int i = 0; i < args.length && canUseDense; i++) {
                    if (args[i] instanceof NativeArray) {
                        // only try to use dense approach for Array-like
                        // objects that are actually NativeArrays
                        final NativeArray arg = (NativeArray) args[i];
                        canUseDense = arg.denseOnly;
                        length += arg.length;
                    } else {
                        length++;
                    }
                }
                if (canUseDense && denseResult.ensureCapacity(length)) {
                    System.arraycopy(denseThis.dense, 0, denseResult.dense,
                                     0, (int) denseThis.length);
                    int cursor = (int) denseThis.length;
                    for (int i = 0; i < args.length && canUseDense; i++) {
                        if (args[i] instanceof NativeArray) {
                            NativeArray arg = (NativeArray) args[i];
                            System.arraycopy(arg.dense, 0,
                                    denseResult.dense, cursor,
                                    (int)arg.length);
                            cursor += (int)arg.length;
                        } else {
                            denseResult.dense[cursor++] = args[i];
                        }
                    }
                    denseResult.length = length;
                    return result;
                }
            }
        }

        long length;
        long slot = 0;

        /* Put the target in the result array; only add it as an array
         * if it looks like one.
         */
        if (ScriptRuntime.instanceOf(thisObj, ctor, cx)) {
            length = getLengthProperty(cx, thisObj);

            // Copy from the target object into the result
            for (slot = 0; slot < length; slot++) {
                Object temp = getRawElem(thisObj, slot);
                if (temp != NOT_FOUND) {
                    setElem(cx, result, slot, temp);
                }
            }
        } else {
            setElem(cx, result, slot++, thisObj);
        }

        /* Copy from the arguments into the result.  If any argument
         * has a numeric length property, treat it as an array and add
         * elements separately; otherwise, just copy the argument.
         */
        for (int i = 0; i < args.length; i++) {
            if (ScriptRuntime.instanceOf(args[i], ctor, cx)) {
                // ScriptRuntime.instanceOf => instanceof Scriptable
                Scriptable arg = (Scriptable)args[i];
                length = getLengthProperty(cx, arg);
                for (long j = 0; j < length; j++, slot++) {
                    Object temp = getRawElem(arg, j);
                    if (temp != NOT_FOUND) {
                        setElem(cx, result, slot, temp);
                    }
                }
            } else {
                setElem(cx, result, slot++, args[i]);
            }
        }
        setLengthProperty(cx, result, slot);
        return result;
    }

    private Scriptable js_slice(Context cx, Scriptable thisObj,
                                Object[] args)
    {
        Scriptable scope = getTopLevelScope(this);
        Scriptable result = cx.newArray(scope, 0);
        long length = getLengthProperty(cx, thisObj);

        long begin, end;
        if (args.length == 0) {
            begin = 0;
            end = length;
        } else {
            begin = toSliceIndex(ScriptRuntime.toInteger(args[0]), length);
            if (args.length == 1) {
                end = length;
            } else {
                end = toSliceIndex(ScriptRuntime.toInteger(args[1]), length);
            }
        }

        for (long slot = begin; slot < end; slot++) {
            Object temp = getRawElem(thisObj, slot);
            if (temp != NOT_FOUND) {
                setElem(cx, result, slot - begin, temp);
            }
        }
        setLengthProperty(cx, result, Math.max(0, end - begin));

        return result;
    }

    private static long toSliceIndex(double value, long length) {
        long result;
        if (value < 0.0) {
            if (value + length < 0.0) {
                result = 0;
            } else {
                result = (long)(value + length);
            }
        } else if (value > length) {
            result = length;
        } else {
            result = (long)value;
        }
        return result;
    }

    /**
     * Implements the methods "indexOf" and "lastIndexOf".
     */
    private Object indexOfHelper(Context cx, Scriptable thisObj,
                                 Object[] args, boolean isLast)
    {
        Object compareTo = args.length > 0 ? args[0] : Undefined.instance;
        long length = getLengthProperty(cx, thisObj);
        long start;
        if (isLast) {
            // lastIndexOf
            /*
             * From http://developer.mozilla.org/en/docs/Core_JavaScript_1.5_Reference:Objects:Array:lastIndexOf
             * The index at which to start searching backwards. Defaults to the
             * array's length, i.e. the whole array will be searched. If the
             * index is greater than or equal to the length of the array, the
             * whole array will be searched. If negative, it is taken as the
             * offset from the end of the array. Note that even when the index
             * is negative, the array is still searched from back to front. If
             * the calculated index is less than 0, -1 is returned, i.e. the
             * array will not be searched.
             */
            if (args.length < 2) {
                // default
                start = length-1;
            } else {
                start = (long)ScriptRuntime.toInteger(args[1]);
                if (start >= length)
                    start = length-1;
                else if (start < 0)
                    start += length;
                if (start < 0) return NEGATIVE_ONE;
            }
        } else {
            // indexOf
            /*
             * From http://developer.mozilla.org/en/docs/Core_JavaScript_1.5_Reference:Objects:Array:indexOf
             * The index at which to begin the search. Defaults to 0, i.e. the
             * whole array will be searched. If the index is greater than or
             * equal to the length of the array, -1 is returned, i.e. the array
             * will not be searched. If negative, it is taken as the offset from
             * the end of the array. Note that even when the index is negative,
             * the array is still searched from front to back. If the calculated
             * index is less than 0, the whole array will be searched.
             */
            if (args.length < 2) {
                // default
                start = 0;
            } else {
                start = (long)ScriptRuntime.toInteger(args[1]);
                if (start < 0) {
                    start += length;
                    if (start < 0)
                        start = 0;
                }
                if (start > length - 1) return NEGATIVE_ONE;
            }
        }
        if (thisObj instanceof NativeArray) {
            NativeArray na = (NativeArray) thisObj;
            if (na.denseOnly) {
                if (isLast) {
                  for (int i=(int)start; i >= 0; i--) {
                      if (na.dense[i] != Scriptable.NOT_FOUND &&
                          ScriptRuntime.shallowEq(na.dense[i], compareTo))
                      {
                          return Long.valueOf(i);
                      }
                  }
                } else {
                  for (int i=(int)start; i < length; i++) {
                      if (na.dense[i] != Scriptable.NOT_FOUND &&
                          ScriptRuntime.shallowEq(na.dense[i], compareTo))
                      {
                          return Long.valueOf(i);
                      }
                  }
                }
                return NEGATIVE_ONE;
            }
        }
        if (isLast) {
          for (long i=start; i >= 0; i--) {
              Object val = getRawElem(thisObj, i);
              if (val != NOT_FOUND && ScriptRuntime.shallowEq(val, compareTo)) {
                  return Long.valueOf(i);
              }
          }
        } else {
          for (long i=start; i < length; i++) {
              Object val = getRawElem(thisObj, i);
              if (val != NOT_FOUND && ScriptRuntime.shallowEq(val, compareTo)) {
                  return Long.valueOf(i);
              }
          }
        }
        return NEGATIVE_ONE;
    }

    /**
     * Implements the methods "every", "filter", "forEach", "map", and "some".
     */
    private Object iterativeMethod(Context cx, Methods method, Scriptable scope,
                                   Scriptable thisObj, Object[] args)
    {
        Object callbackArg = args.length > 0 ? args[0] : Undefined.instance;
        if (callbackArg == null || !(callbackArg instanceof Function)) {
            throw ScriptRuntime.notFunctionError(callbackArg);
        }
        Function f = (Function) callbackArg;
        Scriptable parent = ScriptableObject.getTopLevelScope(f);
        Scriptable thisArg;
        if (args.length < 2 || args[1] == null || args[1] == Undefined.instance)
        {
            thisArg = parent;
        } else {
            thisArg = ScriptRuntime.toObject(cx, scope, args[1]);
        }
        long length = getLengthProperty(cx, thisObj);
        int resultLength = method == Methods.map ? (int) length : 0;
        Scriptable array = cx.newArray(scope, resultLength);
        long j=0;
        for (long i=0; i < length; i++) {
            Object[] innerArgs = new Object[3];
            Object elem = getRawElem(thisObj, i);
            if (elem == Scriptable.NOT_FOUND) {
                continue;
            }
            innerArgs[0] = elem;
            innerArgs[1] = Long.valueOf(i);
            innerArgs[2] = thisObj;
            Object result = f.call(cx, parent, thisArg, innerArgs);
            switch (method) {
              case every:
                if (!ScriptRuntime.toBoolean(result))
                    return Boolean.FALSE;
                break;
              case filter:
                if (ScriptRuntime.toBoolean(result))
                  setElem(cx, array, j++, innerArgs[0]);
                break;
              case forEach:
                break;
              case map:
                setElem(cx, array, i, result);
                break;
              case some:
                if (ScriptRuntime.toBoolean(result))
                    return Boolean.TRUE;
                break;
            }
        }
        switch (method) {
          case every:
            return Boolean.TRUE;
          case filter:
          case map:
            return array;
          case some:
            return Boolean.FALSE;
          case forEach:
          default:
            return Undefined.instance;
        }
    }

    /**
     * Implements the methods "reduce" and "reduceRight".
     */
    private Object reduceMethod(Context cx, Methods method, Scriptable scope,
                                   Scriptable thisObj, Object[] args)
    {
        Object callbackArg = args.length > 0 ? args[0] : Undefined.instance;
        if (callbackArg == null || !(callbackArg instanceof Function)) {
            throw ScriptRuntime.notFunctionError(callbackArg);
        }
        Function f = (Function) callbackArg;
        Scriptable parent = ScriptableObject.getTopLevelScope(f);
        long length = getLengthProperty(cx, thisObj);
        // hack to serve both reduce and reduceRight with the same loop
        boolean movingLeft = method == Methods.reduce;
        Object value = args.length > 1 ? args[1] : Scriptable.NOT_FOUND;
        for (long i = 0; i < length; i++) {
            long index = movingLeft ? i : (length - 1 - i);
            Object elem = getRawElem(thisObj, index);
            if (elem == Scriptable.NOT_FOUND) {
                continue;
            }
            if (value == Scriptable.NOT_FOUND) {
                // no initial value passed, use first element found as inital value
                value = elem;
            } else {
                Object[] innerArgs = { value, elem, index, thisObj };
                value = f.call(cx, parent, parent, innerArgs);
            }
        }
        if (value == Scriptable.NOT_FOUND) {
            // reproduce spidermonkey error message
            throw ScriptRuntime.typeError0("msg.empty.array.reduce");
        }
        return value;
    }

    // methods to implement java.util.List

    public boolean contains(Object o) {
        return indexOf(o) > -1;
    }

    public Object[] toArray() {
        return toArray(ScriptRuntime.emptyArgs);
    }

    public Object[] toArray(Object[] a) {
        long longLen = length;
        if (longLen > Integer.MAX_VALUE) {
            throw new IllegalStateException();
        }
        int len = (int) longLen;
        Object[] array = a.length >= len ?
                a : (Object[]) java.lang.reflect.Array
                .newInstance(a.getClass().getComponentType(), len);
        for (int i = 0; i < len; i++) {
            array[i] = get(i);
        }
        return array;
    }

    public boolean containsAll(Collection c) {
        for (Object aC : c)
            if (!contains(aC))
                return false;
        return true;
    }

    public int size() {
        long longLen = length;
        if (longLen > Integer.MAX_VALUE) {
            throw new IllegalStateException();
        }
        return (int) longLen;
    }

    public Object get(long index) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException();
        }
        Object value = getRawElem(this, index);
        if (value == Scriptable.NOT_FOUND || value == Undefined.instance) {
            return null;
        } else if (value instanceof Wrapper) {
            return ((Wrapper) value).unwrap();
        } else {
            return value;
        }
    }

    public Object get(int index) {
        return get((long) index);
    }

    public int indexOf(Object o) {
        long longLen = length;
        if (longLen > Integer.MAX_VALUE) {
            throw new IllegalStateException();
        }
        int len = (int) longLen;
        if (o == null) {
            for (int i = 0; i < len; i++) {
                if (get(i) == null) {
                    return i;
                }
            }
        } else {
            for (int i = 0; i < len; i++) {
                if (o.equals(get(i))) {
                    return i;
                }
            }
        }
        return -1;
    }

    public int lastIndexOf(Object o) {
        long longLen = length;
        if (longLen > Integer.MAX_VALUE) {
            throw new IllegalStateException();
        }
        int len = (int) longLen;
        if (o == null) {
            for (int i = len - 1; i >= 0; i--) {
                if (get(i) == null) {
                    return i;
                }
            }
        } else {
            for (int i = len - 1; i >= 0; i--) {
                if (o.equals(get(i))) {
                    return i;
                }
            }
        }
        return -1;
    }

    public Iterator iterator() {
        return listIterator(0);
    }

    public ListIterator listIterator() {
        return listIterator(0);
    }

    public ListIterator listIterator(final int start) {
        long longLen = length;
        if (longLen > Integer.MAX_VALUE) {
            throw new IllegalStateException();
        }
        final int len = (int) longLen;

        if (start < 0 || start > len) {
            throw new IndexOutOfBoundsException("Index: " + start);
        }

        return new ListIterator() {

            int cursor = start;

            public boolean hasNext() {
                return cursor < len;
            }

            public Object next() {
                if (cursor == len) {
                    throw new NoSuchElementException();
                }
                return get(cursor++);
            }

            public boolean hasPrevious() {
                return cursor > 0;
            }

            public Object previous() {
                if (cursor == 0) {
                    throw new NoSuchElementException();
                }
                return get(--cursor);
            }

            public int nextIndex() {
                return cursor;
            }

            public int previousIndex() {
                return cursor - 1;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

            public void add(Object o) {
                throw new UnsupportedOperationException();
            }

            public void set(Object o) {
                throw new UnsupportedOperationException();
            }
        };
    }

    public boolean add(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public void add(int index, Object element) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(int index, Collection c) {
        throw new UnsupportedOperationException();
    }

    public Object set(int index, Object element) {
        throw new UnsupportedOperationException();
    }

    public Object remove(int index) {
        throw new UnsupportedOperationException();
    }

    public List subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }

    enum Methods {
        constructor(1),
        join(1),
        reverse(0),
        sort(1),
        push(1),
        pop(0),
        shift(0),
        unshift(1),
        splice(2),
        concat(1),
        slice(2),
        indexOf(1),
        lastIndexOf(1),
        every(1),
        filter(1),
        forEach(1),
        map(1),
        some(1),
        reduce(1),
        reduceRight(1),
        toString(0),
        toLocaleString(0),
        toSource(0);

        private final int arity;
        Methods(int arity) {
            this.arity = arity;
        }
    }

    enum StaticMethods {
        join(1, true),
        reverse(0, true),
        sort(1, true),
        push(1, true),
        pop(0, true),
        shift(0, true),
        unshift(1, true),
        splice(2, true),
        concat(1, true),
        slice(2, true),
        indexOf(1, true),
        lastIndexOf(1, true),
        every(1, true),
        filter(1, true),
        forEach(1, true),
        map(1, true),
        some(1, true),
        reduce(1, true),
        reduceRight(1, true),
        isArray(1, false);

        private final int arity;
        private final Methods instanceMethod;
        StaticMethods(int arity, boolean callsInstance) {
            this.arity = arity;
            this.instanceMethod = callsInstance ? Methods.valueOf(name()) : null;
        }
    }

    /**
     * Internal representation of the JavaScript array's length property.
     */
    private long length;

    /**
     * Cache boxed length to avoid repeated wrapping
     */
    private Number boxedLength;

    /**
     * Attributes of the array's length property
     */
    private int lengthAttr = DONTENUM | PERMANENT;

    /**
     * Fast storage for dense arrays. Sparse arrays will use the superclass's
     * hashtable storage scheme.
     */
    private Object[] dense;

    /**
     * True if all numeric properties are stored in <code>dense</code>.
     */
    private boolean denseOnly;

    /**
     * The maximum size of <code>dense</code> that will be allocated initially.
     */
    private static int maximumInitialCapacity = 10000;

    /**
     * The default capacity for <code>dense</code>.
     */
    private static final int DEFAULT_INITIAL_CAPACITY = 10;

    /**
     * The factor to grow <code>dense</code> by.
     */
    private static final double GROW_FACTOR = 1.5;
    private static final int MAX_PRE_GROW_SIZE = (int)(Integer.MAX_VALUE / GROW_FACTOR);
}
