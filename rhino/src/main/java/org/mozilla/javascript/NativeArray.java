/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static org.mozilla.javascript.ArrayLikeAbstractOperations.getRawElem;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import org.mozilla.javascript.ArrayLikeAbstractOperations.IterativeOperation;
import org.mozilla.javascript.ArrayLikeAbstractOperations.ReduceOperation;
import org.mozilla.javascript.xml.XMLObject;

/**
 * This class implements the Array native object.
 *
 * @author Norris Boyd
 * @author Mike McCabe
 */
public class NativeArray extends ScriptableObject implements List {
    private static final long serialVersionUID = 7331366857676127338L;

    /*
     * Optimization possibilities and open issues:
     * - Long vs. double schizophrenia. I suspect it might be better
     * to use double throughout.
     *
     * - Functions that need a new Array call "new Array" in the
     * current scope rather than using a hardwired constructor;
     * "Array" could be redefined. It turns out that js calls the
     * equivalent of "new Array" in the current scope, except that it
     * always gets at least an object back, even when Array == null.
     */

    static final long MAX_ARRAY_INDEX = 0xfffffffel;

    private static final Object ARRAY_TAG = "Array";
    private static final String CLASS_NAME = "Array";
    private static final Long NEGATIVE_ONE = Long.valueOf(-1);
    private static final String[] UNSCOPABLES = {
        "at",
        "copyWithin",
        "entries",
        "fill",
        "find",
        "findIndex",
        "findLast",
        "findLastIndex",
        "flat",
        "flatMap",
        "includes",
        "keys",
        "toReversed",
        "toSorted",
        "toSpliced",
        "values"
    };

    static void init(Context cx, Scriptable scope, boolean sealed) {
        LambdaConstructor ctor =
                new LambdaConstructor(scope, CLASS_NAME, 1, NativeArray::jsConstructor);

        var proto = new NativeArray(0);
        ctor.setPrototypeScriptable(proto);

        defineMethodOnConstructor(ctor, scope, "of", 0, NativeArray::js_of);
        defineMethodOnConstructor(ctor, scope, "from", 1, NativeArray::js_from);
        defineMethodOnConstructor(ctor, scope, "fromAsync", 1, NativeArray::js_fromAsync);
        defineMethodOnConstructor(ctor, scope, "isArray", 1, NativeArray::js_isArrayMethod);

        // The following need to appear on the constructor for
        // historical reasons even thought they should no tbe there
        // according to the spec.

        exposeMethodOnConstructor(ctor, scope, "join", 1, NativeArray::js_join);
        exposeMethodOnConstructor(ctor, scope, "reverse", 0, NativeArray::js_reverse);
        exposeMethodOnConstructor(ctor, scope, "sort", 1, NativeArray::js_sort);
        exposeMethodOnConstructor(ctor, scope, "push", 1, NativeArray::js_push);
        exposeMethodOnConstructor(ctor, scope, "pop", 0, NativeArray::js_pop);
        exposeMethodOnConstructor(ctor, scope, "shift", 0, NativeArray::js_shift);
        exposeMethodOnConstructor(ctor, scope, "unshift", 1, NativeArray::js_unshift);
        exposeMethodOnConstructor(ctor, scope, "splice", 2, NativeArray::js_splice);
        exposeMethodOnConstructor(ctor, scope, "concat", 1, NativeArray::js_concat);
        exposeMethodOnConstructor(ctor, scope, "slice", 2, NativeArray::js_slice);
        exposeMethodOnConstructor(ctor, scope, "indexOf", 1, NativeArray::js_indexOf);
        exposeMethodOnConstructor(ctor, scope, "lastIndexOf", 1, NativeArray::js_lastIndexOf);
        exposeMethodOnConstructor(ctor, scope, "every", 1, NativeArray::js_every);
        exposeMethodOnConstructor(ctor, scope, "filter", 1, NativeArray::js_filter);
        exposeMethodOnConstructor(ctor, scope, "forEach", 1, NativeArray::js_forEach);
        exposeMethodOnConstructor(ctor, scope, "map", 1, NativeArray::js_map);
        exposeMethodOnConstructor(ctor, scope, "some", 1, NativeArray::js_some);
        exposeMethodOnConstructor(ctor, scope, "find", 1, NativeArray::js_find);
        exposeMethodOnConstructor(ctor, scope, "findIndex", 1, NativeArray::js_findIndex);
        exposeMethodOnConstructor(ctor, scope, "findLast", 1, NativeArray::js_findLast);
        exposeMethodOnConstructor(ctor, scope, "findLastIndex", 1, NativeArray::js_findLastIndex);
        exposeMethodOnConstructor(ctor, scope, "reduce", 1, NativeArray::js_reduce);
        exposeMethodOnConstructor(ctor, scope, "reduceRight", 1, NativeArray::js_reduceRight);

        defineMethodOnPrototype(ctor, scope, "toString", 0, NativeArray::js_toString);
        defineMethodOnPrototype(ctor, scope, "toLocaleString", 0, NativeArray::js_toLocaleString);
        defineMethodOnPrototype(ctor, scope, "toSource", 0, NativeArray::js_toSource);
        defineMethodOnPrototype(ctor, scope, "join", 1, NativeArray::js_join);
        defineMethodOnPrototype(ctor, scope, "reverse", 0, NativeArray::js_reverse);
        defineMethodOnPrototype(ctor, scope, "sort", 1, NativeArray::js_sort);
        defineMethodOnPrototype(ctor, scope, "push", 1, NativeArray::js_push);
        defineMethodOnPrototype(ctor, scope, "pop", 0, NativeArray::js_pop);
        defineMethodOnPrototype(ctor, scope, "shift", 0, NativeArray::js_shift);
        defineMethodOnPrototype(ctor, scope, "unshift", 1, NativeArray::js_unshift);
        defineMethodOnPrototype(ctor, scope, "splice", 2, NativeArray::js_splice);
        defineMethodOnPrototype(ctor, scope, "concat", 1, NativeArray::js_concat);
        defineMethodOnPrototype(ctor, scope, "slice", 2, NativeArray::js_slice);
        defineMethodOnPrototype(ctor, scope, "indexOf", 1, NativeArray::js_indexOf);
        defineMethodOnPrototype(ctor, scope, "lastIndexOf", 1, NativeArray::js_lastIndexOf);
        defineMethodOnPrototype(ctor, scope, "includes", 1, NativeArray::js_includes);
        defineMethodOnPrototype(ctor, scope, "fill", 1, NativeArray::js_fill);
        defineMethodOnPrototype(ctor, scope, "copyWithin", 2, NativeArray::js_copyWithin);
        defineMethodOnPrototype(ctor, scope, "at", 1, NativeArray::js_at);
        defineMethodOnPrototype(ctor, scope, "flat", 0, NativeArray::js_flat);
        defineMethodOnPrototype(ctor, scope, "flatMap", 1, NativeArray::js_flatMap);
        defineMethodOnPrototype(ctor, scope, "every", 1, NativeArray::js_every);
        defineMethodOnPrototype(ctor, scope, "filter", 1, NativeArray::js_filter);
        defineMethodOnPrototype(ctor, scope, "forEach", 1, NativeArray::js_forEach);
        defineMethodOnPrototype(ctor, scope, "map", 1, NativeArray::js_map);
        defineMethodOnPrototype(ctor, scope, "some", 1, NativeArray::js_some);
        defineMethodOnPrototype(ctor, scope, "find", 1, NativeArray::js_find);
        defineMethodOnPrototype(ctor, scope, "findIndex", 1, NativeArray::js_findIndex);
        defineMethodOnPrototype(ctor, scope, "findLast", 1, NativeArray::js_findLast);
        defineMethodOnPrototype(ctor, scope, "findLastIndex", 1, NativeArray::js_findLastIndex);
        defineMethodOnPrototype(ctor, scope, "reduce", 1, NativeArray::js_reduce);
        defineMethodOnPrototype(ctor, scope, "reduceRight", 1, NativeArray::js_reduceRight);
        defineMethodOnPrototype(ctor, scope, "keys", 0, NativeArray::js_keys);
        defineMethodOnPrototype(ctor, scope, "entries", 0, NativeArray::js_entries);
        defineMethodOnPrototype(ctor, scope, "values", 0, NativeArray::js_values);
        defineMethodOnPrototype(ctor, scope, "toReversed", 0, NativeArray::js_toReversed);
        defineMethodOnPrototype(ctor, scope, "toSorted", 1, NativeArray::js_toSorted);
        defineMethodOnPrototype(ctor, scope, "toSpliced", 2, NativeArray::js_toSpliced);
        defineMethodOnPrototype(ctor, scope, "with", 2, NativeArray::js_with);

        ctor.definePrototypeAlias("values", SymbolKey.ITERATOR, DONTENUM);
        ScriptRuntimeES6.addSymbolSpecies(cx, scope, ctor);
        ScriptRuntimeES6.addSymbolUnscopables(
                cx,
                scope,
                proto,
                new LazilyLoadedCtor(
                        proto, "", false, false, (c, s, sld) -> makeUnscopables(c, s)));

        ctor.setPrototypePropertyAttributes(PERMANENT | READONLY | DONTENUM);
        ScriptableObject.defineProperty(scope, CLASS_NAME, ctor, DONTENUM);
        if (sealed) {
            ctor.sealObject();
            ((NativeArray) ctor.getPrototypeProperty()).sealObject();
        }
    }

    private static void defineMethodOnConstructor(
            LambdaConstructor constructor,
            Scriptable scope,
            String name,
            int length,
            SerializableCallable target) {
        constructor.defineConstructorMethod(scope, name, length, target);
    }

    private static void defineMethodOnPrototype(
            LambdaConstructor constructor,
            Scriptable scope,
            String name,
            int length,
            SerializableCallable target) {
        constructor.definePrototypeMethod(scope, name, length, target);
    }

    private static void exposeMethodOnConstructor(
            LambdaConstructor constructor,
            Scriptable scope,
            String name,
            int length,
            SerializableCallable target) {
        constructor.defineConstructorMethod(
                scope,
                name,
                length,
                (cx, s, thisObj, args) -> {
                    var realThis = ScriptRuntime.toObject(cx, scope, args[0]);
                    var realArgs = Arrays.copyOfRange(args, 1, args.length);
                    return target.call(cx, s, realThis, realArgs);
                });
    }

    static int getMaximumInitialCapacity() {
        return maximumInitialCapacity;
    }

    static void setMaximumInitialCapacity(int maximumInitialCapacity) {
        NativeArray.maximumInitialCapacity = maximumInitialCapacity;
    }

    public NativeArray(long lengthArg) {
        denseOnly = lengthArg <= maximumInitialCapacity;
        if (denseOnly) {
            int intLength = (int) lengthArg;
            if (intLength < DEFAULT_INITIAL_CAPACITY) intLength = DEFAULT_INITIAL_CAPACITY;
            dense = new Object[intLength];
            Arrays.fill(dense, Scriptable.NOT_FOUND);
        }
        length = lengthArg;
        createLengthProp();
    }

    public NativeArray(Object[] array) {
        denseOnly = true;
        dense = array;
        length = array.length;
        createLengthProp();
    }

    @Override
    public String getClassName() {
        return "Array";
    }

    @Override
    public void setPrototype(Scriptable p) {
        super.setPrototype(p);
        if (!(p instanceof NativeArray)) {
            setDenseOnly(false);
        }
    }

    private static Object makeUnscopables(Context cx, Scriptable scope) {
        NativeObject obj;

        obj = (NativeObject) cx.newObject(scope);

        var desc = ScriptableObject.buildDataDescriptor(true, EMPTY);
        for (var k : UNSCOPABLES) {
            obj.defineOwnProperty(cx, k, desc);
        }
        obj.setPrototype(null); // unscopables don't have any prototype
        return obj;
    }

    @Override
    public Object get(int index, Scriptable start) {
        var slot = denseOnly ? null : getMap().query(null, index);
        if (!denseOnly && slot != null && slot.isSetterSlot()) return slot.getValue(start);
        if (dense != null && 0 <= index && index < dense.length) return dense[index];
        return slot == null ? NOT_FOUND : slot.getValue(start);
    }

    @Override
    public boolean has(int index, Scriptable start) {
        var slot = denseOnly ? null : getMap().query(null, index);
        if (slot != null) {
            return true;
        }
        if (dense != null && 0 <= index && index < dense.length) return dense[index] != NOT_FOUND;
        return false;
    }

    private static long toArrayIndex(Object id) {
        if (id instanceof String) {
            return toArrayIndex((String) id);
        } else if (id instanceof Number) {
            return toArrayIndex(((Number) id).doubleValue());
        }
        return -1;
    }

    // if id is an array index (ECMA 15.4.0), return the number,
    // otherwise return -1L
    private static long toArrayIndex(String id) {
        long index = toArrayIndex(ScriptRuntime.toNumber(id));
        // Assume that ScriptRuntime.toString(index) is the same
        // as java.lang.Long.toString(index) for long
        if (Long.toString(index).equals(id)) {
            return index;
        }
        return -1;
    }

    private static long toArrayIndex(double d) {
        if (!Double.isNaN(d)) {
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
    public void put(String id, Scriptable start, Object value) {
        super.put(id, start, value);
        if (start == this) {
            // If the object is sealed, super will throw exception
            long index = toArrayIndex(id);
            if (index >= length) {
                length = index + 1;
                modCount++;
                denseOnly = false;
            }
        }
    }

    private boolean ensureCapacity(int capacity) {
        if (capacity > dense.length) {
            if (capacity > MAX_PRE_GROW_SIZE) {
                denseOnly = false;
                return false;
            }
            capacity = Math.max(capacity, (int) (dense.length * GROW_FACTOR));
            Object[] newDense = new Object[capacity];
            System.arraycopy(dense, 0, newDense, 0, dense.length);
            Arrays.fill(newDense, dense.length, newDense.length, Scriptable.NOT_FOUND);
            dense = newDense;
        }
        return true;
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
        var slot = denseOnly ? null : getMap().query(null, index);
        if (start == this
                && !isSealed()
                && dense != null
                && 0 <= index
                && (denseOnly || (slot == null || !slot.isSetterSlot()))) {
            if (!isExtensible() && this.length <= index) {
                return;
            } else if (index < dense.length) {
                dense[index] = value;
                if (this.length <= index) {
                    this.length = (long) index + 1;
                    this.modCount++;
                }
                return;
            } else if (denseOnly
                    && index < dense.length * GROW_FACTOR
                    && ensureCapacity(index + 1)) {
                dense[index] = value;
                this.length = (long) index + 1;
                this.modCount++;
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
                this.length = (long) index + 1;
                this.modCount++;
            }
        }
    }

    @Override
    public void delete(int index) {
        var slot = denseOnly ? null : getMap().query(null, index);
        if (dense != null
                && 0 <= index
                && index < dense.length
                && !isSealed()
                && (denseOnly || (slot == null || !slot.isSetterSlot()))) {
            dense[index] = NOT_FOUND;
        } else {
            super.delete(index);
        }
    }

    public void deleteInternal(CompoundOperationMap compoundOp, String id) {
        compoundOp.compute(this, id, 0, ScriptableObject::checkSlotRemoval);
    }

    public void deleteInternal(CompoundOperationMap compoundOp, int index) {
        var slot = denseOnly ? null : compoundOp.query(null, index);
        if (dense != null
                && 0 <= index
                && index < dense.length
                && !isSealed()
                && (denseOnly || (slot == null || !slot.isSetterSlot()))) {
            dense[index] = NOT_FOUND;
        } else {
            compoundOp.compute(this, null, index, ScriptableObject::checkSlotRemoval);
        }
    }

    @Override
    public Object[] getIds(CompoundOperationMap map, boolean nonEnumerable, boolean getSymbols) {
        Object[] superIds = super.getIds(map, nonEnumerable, getSymbols);
        if (dense == null) {
            return superIds;
        }
        int N = dense.length;
        long currentLength = length;
        if (N > currentLength) {
            N = (int) currentLength;
        }
        if (N == 0) {
            return superIds;
        }
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

    public List<Integer> getIndexIds() {
        Object[] ids = getIds();
        List<Integer> indices = new ArrayList<>(ids.length);
        for (Object id : ids) {
            int int32Id = ScriptRuntime.toInt32(id);
            if (int32Id >= 0
                    && ScriptRuntime.toString(int32Id).equals(ScriptRuntime.toString(id))) {
                indices.add(Integer.valueOf(int32Id));
            }
        }
        return indices;
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        if (hint == ScriptRuntime.NumberClass) {
            Context cx = Context.getContext();
            if (cx.getLanguageVersion() == Context.VERSION_1_2) return Long.valueOf(length);
        }
        return super.getDefaultValue(hint);
    }

    private DescriptorInfo defaultIndexPropertyDescriptor(Object value) {
        return new DescriptorInfo(true, true, true, NOT_FOUND, NOT_FOUND, value);
    }

    @Override
    public int getAttributes(int index) {
        if (dense != null && index >= 0 && index < dense.length && dense[index] != NOT_FOUND) {
            return EMPTY;
        }
        return super.getAttributes(index);
    }

    @Override
    protected DescriptorInfo getOwnPropertyDescriptor(Context cx, Object id) {
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
    protected boolean defineOwnProperty(
            Context cx, Object id, DescriptorInfo desc, boolean checkValid) {
        long index = toArrayIndex(id);
        if (index >= length) {
            length = index + 1;
            modCount++;
        }

        if (index != -1 && dense != null) {
            Object[] values = dense;
            dense = null;
            denseOnly = false;
            for (int i = 0; i < values.length; i++) {
                if (values[i] != NOT_FOUND) {
                    if (!isExtensible()) {
                        // Force creating a slot, before calling .put(...) on the next line, which
                        // would otherwise fail on a array on which preventExtensions() has been
                        // called
                        setAttributes(i, 0);
                    }
                    put(i, this, values[i]);
                }
            }
        }

        super.defineOwnProperty(cx, id, desc, checkValid);

        if ("length".equals(id)) {
            lengthAttr =
                    getAttributes("length"); // Update cached attributes value for length property
        }
        return true;
    }

    /** See ECMA 15.4.1,2 */
    static Scriptable jsConstructor(Context cx, Scriptable scope, Object[] args) {
        if (args.length == 0) return new NativeArray(0);

        // Only use 1 arg as first element for version 1.2; for
        // any other version (including 1.3) follow ECMA and use it as
        // a length.
        NativeArray res;
        if (cx.getLanguageVersion() == Context.VERSION_1_2) {
            res = new NativeArray(args);
        } else {
            Object arg0 = args[0];
            if (args.length > 1 || !(arg0 instanceof Number)) {
                res = new NativeArray(args);
            } else {
                long len = ScriptRuntime.toUint32(arg0);
                if (len != ((Number) arg0).doubleValue()) {
                    String msg = ScriptRuntime.getMessageById("msg.arraylength.bad");
                    throw ScriptRuntime.rangeError(msg);
                }
                res = new NativeArray(len);
            }
        }

        return res;
    }

    private void createLengthProp() {
        ScriptableObject.defineBuiltInProperty(
                this,
                "length",
                DONTENUM | PERMANENT,
                NativeArray::lengthGetter,
                NativeArray::lengthSetter,
                NativeArray::lengthAttrSetter,
                NativeArray::arraySetLength);
    }

    private static Object lengthGetter(NativeArray array, Scriptable start) {
        return ScriptRuntime.wrapNumber((double) array.length);
    }

    private static boolean lengthSetter(
            NativeArray builtIn,
            Object value,
            Scriptable owner,
            Scriptable start,
            boolean isThrow) {
        double d = ScriptRuntime.toNumber(value);
        try (var map = builtIn.startCompoundOp(true)) {
            builtIn.setLength(map, d);
        }
        return true;
    }

    private static void lengthAttrSetter(NativeArray builtIn, int attrs) {
        builtIn.lengthAttr = attrs;
    }

    private static Slot lengthDescSetValue(
            ScriptableObject owner,
            DescriptorInfo info,
            Object key,
            Slot existing,
            CompoundOperationMap map,
            Slot slot) {
        ((NativeArray) owner).setLength(map, (Double) info.value);
        return slot;
    }

    protected static boolean arraySetLength(
            NativeArray builtIn,
            BuiltInSlot<NativeArray> current,
            Object id,
            DescriptorInfo info,
            boolean checkValid,
            Object key,
            int index) {
        PropDescValueSetter descSetter = NativeArray::lengthDescSetValue;
        // 10.2.4.2 Step 1.
        Object value = info.value;

        if (value == NOT_FOUND) {
            try (var map = builtIn.startCompoundOp(true)) {
                return ScriptableObject.defineOrdinaryProperty(
                        (o, i, k, e, m, s) -> s, builtIn, map, id, info, checkValid, key, index);
            }
        }

        // 10.2.4.2 Steps 2 - 6
        long newLength = checkLength(value);
        info.value = (double) newLength;

        Object writable = info.writable;
        // 10.2.4.2 9 is true by definition

        try (var map = builtIn.startCompoundOp(true)) {
            // 10.2.4.2 10-11
            if (newLength >= builtIn.length) {
                return ScriptableObject.defineOrdinaryProperty(
                        descSetter, builtIn, map, id, info, checkValid, key, index);
            }

            boolean currentWritable = ((current.getAttributes() & READONLY) == 0);
            if (!currentWritable) {
                throw ScriptRuntime.typeErrorById("msg.change.value.with.writable.false", id);
            }
            boolean newWritable = true;
            if (writable != NOT_FOUND) {
                newWritable = isTrue(writable);
                info.writable = true;
            }

            // The standard set path that will be done by this call will
            // clear any elements as required.
            if (ScriptableObject.defineOrdinaryProperty(
                    descSetter, builtIn, map, id, info, checkValid, key, index)) {
                var currentAttrs = current.getAttributes();
                var newAttrs = newWritable ? (currentAttrs & ~READONLY) : (currentAttrs | READONLY);
                current.setAttributes(newAttrs);
                return true;
            }
        }
        return false;
    }

    private static Scriptable callConstructorOrCreateArray(
            Context cx, Scriptable scope, Scriptable arg, long length, boolean lengthAlways) {
        Scriptable result = null;

        if (arg instanceof Constructable) {
            try {
                final Object[] args =
                        (lengthAlways || (length > 0))
                                ? new Object[] {Long.valueOf(length)}
                                : ScriptRuntime.emptyArgs;
                result = ((Constructable) arg).construct(cx, scope, args);
            } catch (EcmaError ee) {
                if (!"TypeError".equals(ee.getName())) {
                    throw ee;
                }
                // If we get here then it is likely that the function we called is not really
                // a constructor. Unfortunately there's no better way to tell in Rhino right now.
            }
        }

        if (result == null) {
            // "length" below is really a hint so don't worry if it's really large
            result = cx.newArray(scope, (length > Integer.MAX_VALUE) ? 0 : (int) length);
        }

        return result;
    }

    private static Object js_from(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        final Scriptable items =
                ScriptRuntime.toObject(scope, (args.length >= 1) ? args[0] : Undefined.instance);
        Object mapArg = (args.length >= 2) ? args[1] : Undefined.instance;
        Scriptable thisArg = null;
        final boolean mapping = !Undefined.isUndefined(mapArg);
        Function mapFn = null;

        if (mapping) {
            if (!(mapArg instanceof Function)) {
                throw ScriptRuntime.typeErrorById("msg.map.function.not");
            }
            mapFn = (Function) mapArg;

            Object callThisArg = args.length >= 3 ? args[2] : Undefined.SCRIPTABLE_UNDEFINED;
            thisArg = ScriptRuntime.getApplyOrCallThis(cx, scope, callThisArg, 1, mapFn);
        }

        Object iteratorProp = ScriptableObject.getProperty(items, SymbolKey.ITERATOR);
        if ((iteratorProp != Scriptable.NOT_FOUND) && !Undefined.isUndefined(iteratorProp)) {
            final Object iterator = ScriptRuntime.callIterator(items, cx, scope);
            if (!Undefined.isUndefined(iterator)) {
                final Scriptable result =
                        callConstructorOrCreateArray(cx, scope, thisObj, 0, false);
                long k = 0;
                try (IteratorLikeIterable it = new IteratorLikeIterable(cx, scope, iterator)) {
                    for (Object temp : it) {
                        if (mapping) {
                            temp =
                                    mapFn.call(
                                            cx,
                                            scope,
                                            thisArg,
                                            new Object[] {temp, Long.valueOf(k)});
                        }
                        ArrayLikeAbstractOperations.defineElem(cx, result, k, temp);
                        k++;
                    }
                }
                setLengthProperty(cx, result, k);
                return result;
            }
        }

        final long length = getLengthProperty(cx, items);
        final Scriptable result = callConstructorOrCreateArray(cx, scope, thisObj, length, true);
        for (long k = 0; k < length; k++) {
            Object temp = getElem(cx, items, k);
            if (mapping) {
                temp = mapFn.call(cx, scope, thisArg, new Object[] {temp, Long.valueOf(k)});
            }
            ArrayLikeAbstractOperations.defineElem(cx, result, k, temp);
        }

        setLengthProperty(cx, result, length);
        return result;
    }

    /**
     * ES2024 Array.fromAsync implementation. Context-safe: Uses BaseFunction instead of
     * LambdaFunction to avoid Context capture.
     *
     * @param cx Current context (never stored)
     * @param scope Current scope
     * @param thisObj Constructor this (Array constructor)
     * @param args Arguments [items, mapfn, thisArg]
     * @return Promise that resolves to the created array
     */
    private static Object js_fromAsync(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {

        final Object items = args.length >= 1 ? args[0] : Undefined.instance;
        final Object mapfn = args.length >= 2 ? args[1] : Undefined.instance;
        final Object thisArg = args.length >= 3 ? args[2] : Undefined.instance;

        // Get Promise constructor
        Object promiseCtor = ScriptableObject.getProperty(scope, "Promise");
        if (!(promiseCtor instanceof Function)) {
            throw ScriptRuntime.typeErrorById("msg.no.promise");
        }
        Function promiseFunc = (Function) promiseCtor;

        // Create promise executor using BaseFunction to avoid Context capture
        Function executor =
                new BaseFunction() {
                    @Override
                    public Object call(
                            Context lcx,
                            Scriptable lscope,
                            Scriptable lthisObj,
                            Object[] executorArgs) {
                        // lcx is FRESH Context from Promise - never stored
                        if (executorArgs.length < 2) {
                            return Undefined.instance;
                        }

                        Function resolveFunc = (Function) executorArgs[0];
                        Function rejectFunc = (Function) executorArgs[1];

                        try {
                            // Validate mapfn if provided
                            Function mapFunction = null;
                            Scriptable mapThis = null;

                            if (!Undefined.isUndefined(mapfn)) {
                                if (!(mapfn instanceof Function)) {
                                    throw ScriptRuntime.typeErrorById("msg.map.function.not");
                                }
                                mapFunction = (Function) mapfn;
                                mapThis =
                                        ScriptRuntime.getApplyOrCallThis(
                                                lcx, lscope, thisArg, 1, mapFunction);
                            }

                            // Process items with fresh Context
                            processAsyncItems(
                                    lcx,
                                    lscope,
                                    thisObj,
                                    items,
                                    mapFunction,
                                    mapThis,
                                    resolveFunc,
                                    rejectFunc);

                        } catch (RhinoException re) {
                            rejectFunc.call(
                                    lcx,
                                    lscope,
                                    null,
                                    new Object[] {getErrorObject(lcx, lscope, re)});
                        }

                        return Undefined.instance;
                    }
                };

        return promiseFunc.construct(cx, scope, new Object[] {executor});
    }

    /**
     * Process items for Array.fromAsync - Context-safe implementation. Simplified version that only
     * handles array-like objects (no iterator support yet).
     */
    private static void processAsyncItems(
            Context cx,
            Scriptable scope,
            Scriptable thisObj,
            Object items,
            Function mapFn,
            Scriptable mapThis,
            Function resolve,
            Function reject) {

        try {
            // Convert to object
            Scriptable itemsObj = ScriptRuntime.toObject(scope, items);

            // Get length and create result array
            long len = getLengthProperty(cx, itemsObj);
            Scriptable result = callConstructorOrCreateArray(cx, scope, thisObj, len, true);

            if (len == 0) {
                // Empty array - resolve immediately
                resolve.call(cx, scope, null, new Object[] {result});
                return;
            }

            // Process array-like object with Context-safe async processor
            AsyncArrayLikeProcessor processor =
                    new AsyncArrayLikeProcessor(
                            result, itemsObj, len, mapFn, mapThis, resolve, reject);
            processor.processNext(cx, scope, 0);

        } catch (RhinoException re) {
            reject.call(cx, scope, null, new Object[] {getErrorObject(cx, scope, re)});
        }
    }

    private static void processAsyncIterable(
            Context cx,
            Scriptable scope,
            IteratorLikeIterable iter,
            Scriptable A,
            long k,
            Function mapFn,
            Scriptable mapThis,
            Function resolve,
            Function reject) {

        try {
            if (!iter.iterator().hasNext()) {
                setLengthProperty(cx, A, k);
                resolve.call(cx, scope, null, new Object[] {A});
                iter.close();
                return;
            }

            Object value = iter.iterator().next();
            Object promise = promiseResolve(cx, scope, value);

            if (!(promise instanceof Scriptable)) {
                Object mappedValue = value;
                if (mapFn != null) {
                    mappedValue =
                            mapFn.call(cx, scope, mapThis, new Object[] {value, Long.valueOf(k)});
                }
                ArrayLikeAbstractOperations.defineElem(cx, A, k, mappedValue);
                processAsyncIterable(cx, scope, iter, A, k + 1, mapFn, mapThis, resolve, reject);
                return;
            }

            Object thenMethod = ScriptableObject.getProperty((Scriptable) promise, "then");
            if (!(thenMethod instanceof Function)) {
                Object mappedValue = promise;
                if (mapFn != null) {
                    mappedValue =
                            mapFn.call(cx, scope, mapThis, new Object[] {promise, Long.valueOf(k)});
                }
                ArrayLikeAbstractOperations.defineElem(cx, A, k, mappedValue);
                processAsyncIterable(cx, scope, iter, A, k + 1, mapFn, mapThis, resolve, reject);
                return;
            }

            Callable onFulfilled =
                    new LambdaFunction(
                            scope,
                            1,
                            (Context c, Scriptable s, Scriptable t, Object[] args) -> {
                                try {
                                    Object val = args.length > 0 ? args[0] : Undefined.instance;
                                    if (mapFn != null) {
                                        val =
                                                mapFn.call(
                                                        c,
                                                        s,
                                                        mapThis,
                                                        new Object[] {val, Long.valueOf(k)});
                                    }
                                    ArrayLikeAbstractOperations.defineElem(c, A, k, val);
                                    processAsyncIterable(
                                            c, s, iter, A, k + 1, mapFn, mapThis, resolve, reject);
                                } catch (Exception e) {
                                    iter.close();
                                    reject.call(c, s, null, new Object[] {e});
                                }
                                return Undefined.instance;
                            });

            Callable onRejected =
                    new LambdaFunction(
                            scope,
                            1,
                            (Context c, Scriptable s, Scriptable t, Object[] args) -> {
                                iter.close();
                                Object error = args.length > 0 ? args[0] : Undefined.instance;
                                reject.call(c, s, null, new Object[] {error});
                                return Undefined.instance;
                            });

            ((Function) thenMethod)
                    .call(cx, scope, (Scriptable) promise, new Object[] {onFulfilled, onRejected});

        } catch (Exception e) {
            iter.close();
            reject.call(cx, scope, null, new Object[] {e});
        }
    }

    private static void processAsyncArrayLike(
            Context cx,
            Scriptable scope,
            Scriptable source,
            Scriptable A,
            long k,
            long len,
            Function mapFn,
            Scriptable mapThis,
            Function resolve,
            Function reject) {

        if (k >= len) {
            setLengthProperty(cx, A, len);
            resolve.call(cx, scope, null, new Object[] {A});
            return;
        }

        try {
            Object value = getElem(cx, source, k);
            Object promise = promiseResolve(cx, scope, value);

            if (!(promise instanceof Scriptable)) {
                Object mappedValue = value;
                if (mapFn != null) {
                    mappedValue =
                            mapFn.call(cx, scope, mapThis, new Object[] {value, Long.valueOf(k)});
                }
                ArrayLikeAbstractOperations.defineElem(cx, A, k, mappedValue);
                processAsyncArrayLike(
                        cx, scope, source, A, k + 1, len, mapFn, mapThis, resolve, reject);
                return;
            }

            Object thenMethod = ScriptableObject.getProperty((Scriptable) promise, "then");
            if (!(thenMethod instanceof Function)) {
                Object mappedValue = promise;
                if (mapFn != null) {
                    mappedValue =
                            mapFn.call(cx, scope, mapThis, new Object[] {promise, Long.valueOf(k)});
                }
                ArrayLikeAbstractOperations.defineElem(cx, A, k, mappedValue);
                processAsyncArrayLike(
                        cx, scope, source, A, k + 1, len, mapFn, mapThis, resolve, reject);
                return;
            }

            Callable onFulfilled =
                    new LambdaFunction(
                            scope,
                            1,
                            (Context c, Scriptable s, Scriptable t, Object[] args) -> {
                                try {
                                    Object val = args.length > 0 ? args[0] : Undefined.instance;
                                    if (mapFn != null) {
                                        val =
                                                mapFn.call(
                                                        c,
                                                        s,
                                                        mapThis,
                                                        new Object[] {val, Long.valueOf(k)});
                                    }
                                    ArrayLikeAbstractOperations.defineElem(c, A, k, val);
                                    processAsyncArrayLike(
                                            c, s, source, A, k + 1, len, mapFn, mapThis, resolve,
                                            reject);
                                } catch (Exception e) {
                                    reject.call(c, s, null, new Object[] {e});
                                }
                                return Undefined.instance;
                            });

            Callable onRejected =
                    new LambdaFunction(
                            scope,
                            1,
                            (Context c, Scriptable s, Scriptable t, Object[] args) -> {
                                Object error = args.length > 0 ? args[0] : Undefined.instance;
                                reject.call(c, s, null, new Object[] {error});
                                return Undefined.instance;
                            });

            ((Function) thenMethod)
                    .call(cx, scope, (Scriptable) promise, new Object[] {onFulfilled, onRejected});

        } catch (Exception e) {
            reject.call(cx, scope, null, new Object[] {e});
        }
    }

    private static Object promiseResolve(Context cx, Scriptable scope, Object value) {
        Object promiseCtor = ScriptableObject.getProperty(scope, "Promise");
        if (promiseCtor instanceof Function) {
            Object resolveMethod =
                    ScriptableObject.getProperty((Scriptable) promiseCtor, "resolve");
            if (resolveMethod instanceof Function) {
                return ((Function) resolveMethod)
                        .call(cx, scope, (Scriptable) promiseCtor, new Object[] {value});
            }
        }
        return value;
    }

    private static Object getErrorObject(Context cx, Scriptable scope, RhinoException re) {
        if (re instanceof JavaScriptException) {
            return ((JavaScriptException) re).getValue();
        }
        return re;
    }

    /**
     * Context-safe async array-like processor. Never stores Context - receives fresh Context on
     * each call.
     */
    private static class AsyncArrayLikeProcessor {
        private final Scriptable result;
        private final Scriptable items;
        private final long length;
        private final Function mapFn;
        private final Scriptable mapThis;
        private final Function resolve;
        private final Function reject;

        AsyncArrayLikeProcessor(
                Scriptable result,
                Scriptable items,
                long length,
                Function mapFn,
                Scriptable mapThis,
                Function resolve,
                Function reject) {
            this.result = result;
            this.items = items;
            this.length = length;
            this.mapFn = mapFn;
            this.mapThis = mapThis;
            this.resolve = resolve;
            this.reject = reject;
        }

        void processNext(Context cx, Scriptable scope, final long index) {
            if (index >= length) {
                setLengthProperty(cx, result, length);
                resolve.call(cx, scope, null, new Object[] {result});
                return;
            }

            try {
                Object value = getElem(cx, items, index);

                // Apply mapping function if provided
                if (mapFn != null) {
                    value =
                            mapFn.call(
                                    cx, scope, mapThis, new Object[] {value, Long.valueOf(index)});
                }

                // Check if value is a promise/thenable
                if (isThenable(value)) {
                    // Handle promise resolution with Context-safe callbacks
                    Function onFulfilled =
                            new BaseFunction() {
                                @Override
                                public Object call(
                                        Context lcx,
                                        Scriptable lscope,
                                        Scriptable thisObj,
                                        Object[] args) {
                                    // lcx is FRESH Context when promise resolves
                                    Object resolvedValue =
                                            args.length > 0 ? args[0] : Undefined.instance;
                                    ArrayLikeAbstractOperations.defineElem(
                                            lcx, result, index, resolvedValue);
                                    processNext(lcx, lscope, index + 1);
                                    return Undefined.instance;
                                }
                            };

                    Function onRejected =
                            new BaseFunction() {
                                @Override
                                public Object call(
                                        Context lcx,
                                        Scriptable lscope,
                                        Scriptable thisObj,
                                        Object[] args) {
                                    // lcx is FRESH Context when promise rejects
                                    Object error = args.length > 0 ? args[0] : Undefined.instance;
                                    reject.call(lcx, lscope, null, new Object[] {error});
                                    return Undefined.instance;
                                }
                            };

                    // Attach callbacks to promise
                    Object thenMethod = ScriptableObject.getProperty((Scriptable) value, "then");
                    if (thenMethod instanceof Function) {
                        ((Function) thenMethod)
                                .call(
                                        cx,
                                        scope,
                                        (Scriptable) value,
                                        new Object[] {onFulfilled, onRejected});
                    }
                } else {
                    // Synchronous value - store and continue
                    ArrayLikeAbstractOperations.defineElem(cx, result, index, value);
                    processNext(cx, scope, index + 1);
                }

            } catch (Exception e) {
                reject.call(cx, scope, null, new Object[] {e});
            }
        }
    }

    // Helper method to check if value is thenable
    private static boolean isThenable(Object value) {
        if (!(value instanceof Scriptable)) {
            return false;
        }
        Object then = ScriptableObject.getProperty((Scriptable) value, "then");
        return then instanceof Function;
    }

    private static Object js_of(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        final Scriptable result =
                callConstructorOrCreateArray(cx, scope, thisObj, args.length, true);

        if (cx.getLanguageVersion() >= Context.VERSION_ES6 && result instanceof ScriptableObject) {
            var desc = ScriptableObject.buildDataDescriptor(null, EMPTY);
            for (int i = 0; i < args.length; i++) {
                desc.value = args[i];
                ((ScriptableObject) result).defineOwnProperty(cx, i, desc);
            }
        } else {
            for (int i = 0; i < args.length; i++) {
                ArrayLikeAbstractOperations.defineElem(cx, result, i, args[i]);
            }
        }
        setLengthProperty(cx, result, args.length);

        return result;
    }

    public long getLength() {
        return length;
    }

    /**
     * @deprecated Use {@link #getLength()} instead.
     */
    @Deprecated
    public long jsGet_length() {
        return getLength();
    }

    /**
     * Change the value of the internal flag that determines whether all storage is handed by a
     * dense backing array rather than an associative store.
     *
     * @param denseOnly new value for denseOnly flag
     * @throws IllegalArgumentException if an attempt is made to enable denseOnly after it was
     *     disabled; NativeArray code is not written to handle switching back to a dense
     *     representation
     */
    void setDenseOnly(boolean denseOnly) {
        if (denseOnly && !this.denseOnly) throw new IllegalArgumentException();
        this.denseOnly = denseOnly;
    }

    boolean getDenseOnly() {
        return denseOnly;
    }

    private boolean setLength(CompoundOperationMap compoundOp, double d) {
        /* XXX do we satisfy this?
         * 15.4.5.1 [[Put]](P, V):
         * 1. Call the [[CanPut]] method of A with name P.
         * 2. If Result(1) is false, return.
         * ?
         */
        long longVal = ScriptRuntime.toUint32(d);

        if ((lengthAttr & READONLY) != 0) {
            return false;
        }

        if (longVal != d) {
            String msg = ScriptRuntime.getMessageById("msg.arraylength.bad");
            throw ScriptRuntime.rangeError(msg);
        }

        if (denseOnly) {
            if (longVal < length) {
                // downcast okay because denseOnly
                Arrays.fill(dense, (int) longVal, dense.length, NOT_FOUND);
                length = longVal;
                modCount++;
                return true;
            } else if (longVal < MAX_PRE_GROW_SIZE
                    && longVal < (length * GROW_FACTOR)
                    && ensureCapacity((int) longVal)) {
                length = longVal;
                modCount++;
                return true;
            } else {
                denseOnly = false;
            }
        }
        if (longVal < length) {
            // remove all properties between longVal and length
            if (length - longVal > 0x1000) {
                // assume that the representation is sparse
                Object[] e = getIds(compoundOp, false, false); // will only find in object itself
                for (Object id : e) {
                    if (id instanceof String) {
                        // > MAXINT will appear as string
                        String strId = (String) id;
                        long index = toArrayIndex(strId);
                        if (index >= longVal) deleteInternal(compoundOp, strId);
                    } else {
                        int index = ((Integer) id).intValue();
                        if (index >= longVal) deleteInternal(compoundOp, index);
                    }
                }
            } else {
                // assume a dense representation
                for (long i = longVal; i < length; i++) {
                    deleteElem(compoundOp, this, i);
                }
            }
        }
        length = longVal;
        modCount++;
        return true;
    }

    private static long checkLength(Object val) {
        double d = ScriptRuntime.toNumber(val);
        long longVal = ScriptRuntime.toUint32(val);
        if (longVal != d) {
            String msg = ScriptRuntime.getMessageById("msg.arraylength.bad");
            throw ScriptRuntime.rangeError(msg);
        }
        return longVal;
    }

    /* Support for generic Array-ish objects.  Most of the Array
     * functions try to be generic; anything that has a length
     * property is assumed to be an array.
     * getLengthProperty returns 0 if obj does not have the length property
     * or its value is not convertible to a number.
     */
    static long getLengthProperty(Context cx, Scriptable obj) {
        // These will give numeric lengths within Uint32 range.
        if (obj instanceof NativeString) {
            return ((NativeString) obj).getLength();
        }
        if (obj instanceof NativeArray) {
            return ((NativeArray) obj).getLength();
        }
        if (obj instanceof XMLObject) {
            Callable lengthFunc = (Callable) obj.get("length", obj);
            return ((Number) lengthFunc.call(cx, obj, obj, ScriptRuntime.emptyArgs)).longValue();
        }

        Object len = ScriptableObject.getProperty(obj, "length");
        if (len == Scriptable.NOT_FOUND) {
            // toUint32(undefined) == 0
            return 0;
        }

        double doubleLen = ScriptRuntime.toNumber(len);

        // ToLength
        if (doubleLen > NativeNumber.MAX_SAFE_INTEGER) {
            return (long) NativeNumber.MAX_SAFE_INTEGER;
        }
        if (doubleLen < 0) {
            return 0;
        }
        return (long) doubleLen;
    }

    private static Object setLengthProperty(Context cx, Scriptable target, long length) {
        Object len = ScriptRuntime.wrapNumber((double) length);
        ScriptableObject.putProperty(target, "length", len);
        return len;
    }

    /* Utility functions to encapsulate index > Integer.MAX_VALUE
     * handling.  Also avoids unnecessary object creation that would
     * be necessary to use the general ScriptRuntime.get/setElem
     * functions... though this is probably premature optimization.
     */
    private static void deleteElem(Scriptable target, long index) {
        int i = (int) index;
        if (i == index) {
            target.delete(i);
        } else {
            target.delete(Long.toString(index));
        }
    }

    /* This version explicitly checks whether the target is  sealed. The other implementation which does not take a compound op does not do so explicitly, but it does rely on the underlying `delete` implementation doing that check. */
    private static void deleteElem(
            CompoundOperationMap compoundOp, NativeArray target, long index) {
        int i = (int) index;
        if (i == index) {
            checkNotSealed(target, null, i);
            target.deleteInternal(compoundOp, i);
        } else {
            var strIndex = Long.toString(index);
            checkNotSealed(target, strIndex, 0);
            compoundOp.compute(target, strIndex, 0, ScriptableObject::checkSlotRemoval);
        }
    }

    static Object getElem(Context cx, Scriptable target, long index) {
        Object elem = getRawElem(target, index);
        return (elem != Scriptable.NOT_FOUND ? elem : Undefined.instance);
    }

    private static void defineElemOrThrow(Context cx, Scriptable target, long index, Object value) {
        if (index > NativeNumber.MAX_SAFE_INTEGER) {
            throw ScriptRuntime.typeErrorById("msg.arraylength.too.big", String.valueOf(index));
        } else {
            ArrayLikeAbstractOperations.defineElem(cx, target, index, value);
        }
    }

    private static void setElem(Context cx, Scriptable target, long index, Object value) {
        if (index > Integer.MAX_VALUE) {
            String id = Long.toString(index);
            ScriptableObject.putProperty(target, id, value);
        } else {
            ScriptableObject.putProperty(target, (int) index, value);
        }
    }

    // Similar as setElem(), but triggers deleteElem() if value is NOT_FOUND
    private static void setRawElem(Context cx, Scriptable target, long index, Object value) {
        if (value == NOT_FOUND) {
            deleteElem(target, index);
        } else {
            setElem(cx, target, index, value);
        }
    }

    private static String js_toString(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return toStringHelper(
                cx, scope, thisObj, cx.hasFeature(Context.FEATURE_TO_STRING_AS_SOURCE), false);
    }

    private static String js_toLocaleString(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return toStringHelper(cx, scope, thisObj, false, true);
    }

    private static String js_toSource(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return toStringHelper(cx, scope, thisObj, true, false);
    }

    private static String toStringHelper(
            Context cx, Scriptable scope, Scriptable thisObj, boolean toSource, boolean toLocale) {
        Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

        /* It's probably redundant to handle long lengths in this
         * function; StringBuilders are limited to 2^31 in java.
         */
        long length = getLengthProperty(cx, o);

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
            cx.iterating = new HashSet<Scriptable>();
        } else {
            toplevel = false;
            iterating = cx.iterating.contains(o);
        }

        // Make sure cx.iterating is set to null when done
        // so we don't leak memory
        try {
            if (!iterating) {
                // stop recursion
                cx.iterating.add(o);

                // make toSource print null and undefined values in recent versions
                boolean skipUndefinedAndNull =
                        !toSource || cx.getLanguageVersion() < Context.VERSION_1_5;
                for (i = 0; i < length; i++) {
                    if (i > 0) result.append(separator);
                    Object elem = getRawElem(o, i);
                    if (elem == NOT_FOUND
                            || (skipUndefinedAndNull
                                    && (elem == null || elem == Undefined.instance))) {
                        haslast = false;
                        continue;
                    }
                    haslast = true;

                    if (toSource) {
                        result.append(ScriptRuntime.uneval(cx, scope, elem));

                    } else if (elem instanceof String) {
                        result.append((String) elem);

                    } else {
                        if (toLocale) {
                            var fun =
                                    ScriptRuntime.getPropAndThis(elem, "toLocaleString", cx, scope);
                            elem = fun.call(cx, scope, ScriptRuntime.emptyArgs);
                        }
                        result.append(ScriptRuntime.toString(elem));
                    }
                }

                // processing of thisObj done, remove it from the recursion detector
                // to allow thisObj to be again in the array later on
                cx.iterating.remove(o);
            }
        } finally {
            if (toplevel) {
                cx.iterating = null;
            }
        }

        if (toSource) {
            // for [,,].length behavior; we want toString to be symmetric.
            if (!haslast && i > 0) result.append(", ]");
            else result.append(']');
        }
        return result.toString();
    }

    /** See ECMA 15.4.4.3 */
    private static String js_join(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

        long llength = getLengthProperty(cx, o);
        int length = (int) llength;
        if (llength != length) {
            throw Context.reportRuntimeErrorById(
                    "msg.arraylength.too.big", String.valueOf(llength));
        }
        // if no args, use "," as separator
        String separator =
                (args.length < 1 || args[0] == Undefined.instance)
                        ? ","
                        : ScriptRuntime.toString(args[0]);
        if (o instanceof NativeArray) {
            NativeArray na = (NativeArray) o;
            if (na.denseOnly) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < length; i++) {
                    if (i != 0) {
                        sb.append(separator);
                    }
                    if (i < na.dense.length) {
                        Object temp = na.dense[i];
                        if (temp != null
                                && temp != Undefined.instance
                                && temp != Scriptable.NOT_FOUND) {
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
            Object temp = getElem(cx, o, i);
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

    /** See ECMA 15.4.4.4 */
    private static Scriptable js_reverse(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

        if (o instanceof NativeArray) {
            NativeArray na = (NativeArray) o;
            if (na.denseOnly) {
                for (int i = 0, j = ((int) na.length) - 1; i < j; i++, j--) {
                    Object temp = na.dense[i];
                    na.dense[i] = na.dense[j];
                    na.dense[j] = temp;
                }
                return o;
            }
        }
        long len = getLengthProperty(cx, o);

        long half = len / 2;
        for (long i = 0; i < half; i++) {
            long j = len - i - 1;
            Object temp1 = getRawElem(o, i);
            Object temp2 = getRawElem(o, j);
            setRawElem(cx, o, i, temp2);
            setRawElem(cx, o, j, temp1);
        }
        return o;
    }

    /** See ECMA 15.4.4.5 */
    private static Scriptable js_sort(
            final Context cx,
            final Scriptable scope,
            final Scriptable thisObj,
            final Object[] args) {
        Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);
        Comparator<Object> comparator =
                ArrayLikeAbstractOperations.getSortComparator(cx, scope, args);
        return sort(cx, o, comparator);
    }

    private static Scriptable sort(Context cx, Scriptable o, Comparator<Object> comparator) {
        long llength = getLengthProperty(cx, o);
        final int length = (int) llength;
        if (llength != length) {
            throw Context.reportRuntimeErrorById(
                    "msg.arraylength.too.big", String.valueOf(llength));
        }
        // copy the JS array into a working array, so it can be
        // sorted cheaply.
        final Object[] working = new Object[length];
        for (int i = 0; i != length; ++i) {
            working[i] = getRawElem(o, i);
        }

        // Java's 'Arrays.sort' is guaranteed to be stable so we can use it; however,
        // if the comparator is not consistent, it throws an IllegalArgumentException.
        // In case where the comparator is not consistent, the ECMAScript specification states
        // that sort order is implementation-defined, so we can just return the original array.
        try {
            Arrays.sort(working, comparator);
        } catch (IllegalArgumentException e) {
            return o;
        }

        // copy the working array back into thisObj
        for (int i = 0; i < length; ++i) {
            setRawElem(cx, o, i, working[i]);
        }

        return o;
    }

    private static Object js_push(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

        if (o instanceof NativeArray) {
            NativeArray na = (NativeArray) o;
            if (na.denseOnly && na.ensureCapacity((int) na.length + args.length)) {
                for (Object arg : args) {
                    na.dense[(int) na.length++] = arg;
                    na.modCount++;
                }
                return ScriptRuntime.wrapNumber((double) na.length);
            }
        }
        long length = getLengthProperty(cx, o);
        for (int i = 0; i < args.length; i++) {
            setElem(cx, o, length + i, args[i]);
        }

        length += args.length;
        Object lengthObj = setLengthProperty(cx, o, length);

        /*
         * If JS1.2, follow Perl4 by returning the last thing pushed.
         * Otherwise, return the new array length.
         */
        if (cx.getLanguageVersion() == Context.VERSION_1_2)
            // if JS1.2 && no arguments, return undefined.
            return args.length == 0 ? Undefined.instance : args[args.length - 1];

        return lengthObj;
    }

    private static Object js_pop(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

        Object result;
        if (o instanceof NativeArray) {
            NativeArray na = (NativeArray) o;
            if (na.denseOnly && na.length > 0) {
                na.length--;
                na.modCount++;
                result = na.dense[(int) na.length];
                na.dense[(int) na.length] = NOT_FOUND;
                return result;
            }
        }
        long length = getLengthProperty(cx, o);
        if (length > 0) {
            length--;

            // Get the to-be-deleted property's value.
            result = getElem(cx, o, length);

            // We need to delete the last property, because 'thisObj' may not
            // have setLength which does that for us.
            deleteElem(o, length);
        } else {
            result = Undefined.instance;
        }
        // necessary to match js even when length < 0; js pop will give a
        // length property to any target it is called on.
        setLengthProperty(cx, o, length);

        return result;
    }

    private static Object js_shift(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

        if (o instanceof NativeArray) {
            NativeArray na = (NativeArray) o;
            if (na.denseOnly && na.length > 0) {
                na.length--;
                na.modCount++;
                Object result = na.dense[0];
                System.arraycopy(na.dense, 1, na.dense, 0, (int) na.length);
                na.dense[(int) na.length] = NOT_FOUND;
                return result == NOT_FOUND ? Undefined.instance : result;
            }
        }
        Object result;
        long length = getLengthProperty(cx, o);
        if (length > 0) {
            long i = 0;
            length--;

            // Get the to-be-deleted property's value.
            result = getElem(cx, o, i);

            /*
             * Slide down the array above the first element.  Leave i
             * set to point to the last element.
             */
            if (length > 0) {
                for (i = 1; i <= length; i++) {
                    Object temp = getRawElem(o, i);
                    setRawElem(cx, o, i - 1, temp);
                }
            }
            // We need to delete the last property, because 'thisObj' may not
            // have setLength which does that for us.
            deleteElem(o, length);
        } else {
            result = Undefined.instance;
        }
        setLengthProperty(cx, o, length);
        return result;
    }

    private static Object js_unshift(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

        if (o instanceof NativeArray) {
            NativeArray na = (NativeArray) o;
            if (na.denseOnly && na.ensureCapacity((int) na.length + args.length)) {
                System.arraycopy(na.dense, 0, na.dense, args.length, (int) na.length);
                System.arraycopy(args, 0, na.dense, 0, args.length);
                na.length += args.length;
                na.modCount++;
                return ScriptRuntime.wrapNumber((double) na.length);
            }
        }
        long length = getLengthProperty(cx, o);
        int argc = args.length;

        if (argc > 0) {
            if (length + argc > NativeNumber.MAX_SAFE_INTEGER) {
                throw ScriptRuntime.typeErrorById("msg.arraylength.too.big", length + argc);
            }

            /*  Slide up the array to make room for args at the bottom */
            if (length > 0) {
                for (long last = length - 1; last >= 0; last--) {
                    Object temp = getRawElem(o, last);
                    setRawElem(cx, o, last + argc, temp);
                }
            }

            /* Copy from argv to the bottom of the array. */
            for (int i = 0; i < args.length; i++) {
                setElem(cx, o, i, args[i]);
            }
        }
        /* Follow Perl by returning the new array length. */
        length += argc;
        return setLengthProperty(cx, o, length);
    }

    private static Object js_splice(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

        NativeArray na = null;
        Object result = ArrayLikeAbstractOperations.arraySpeciesCreate(cx, scope, o, 0);
        NativeArray nar = null;
        boolean denseFrom = false;
        boolean denseRes = false;

        if (o instanceof NativeArray) {
            na = (NativeArray) o;
            denseFrom = na.denseOnly;
        }
        if (result instanceof NativeArray) {
            nar = (NativeArray) result;
            denseRes = nar.denseOnly;
        }

        /* create an empty Array to return. */
        scope = getTopLevelScope(scope);
        int argc = args.length;
        if (argc == 0) return cx.newArray(scope, 0);
        long length = getLengthProperty(cx, o);

        /* Convert the first argument into a starting index. */
        long begin =
                ArrayLikeAbstractOperations.toSliceIndex(ScriptRuntime.toInteger(args[0]), length);
        argc--;

        /* Convert the second argument into count */
        long actualDeleteCount;
        if (args.length == 1) {
            actualDeleteCount = length - begin;
        } else {
            double dcount = ScriptRuntime.toInteger(args[1]);
            if (dcount < 0) {
                actualDeleteCount = 0;
            } else if (dcount > (length - begin)) {
                actualDeleteCount = length - begin;
            } else {
                actualDeleteCount = (long) dcount;
            }
            argc--;
        }

        long end = begin + actualDeleteCount;
        long delta = argc - actualDeleteCount;

        if (length + delta > NativeNumber.MAX_SAFE_INTEGER) {
            throw ScriptRuntime.typeErrorById("msg.arraylength.too.big", length + delta);
        }
        if (actualDeleteCount > Integer.MAX_VALUE) {
            String msg = ScriptRuntime.getMessageById("msg.arraylength.bad");
            throw ScriptRuntime.rangeError(msg);
        }

        /* If there are elements to remove, put them into the return value. */
        if (actualDeleteCount != 0) {
            if (actualDeleteCount == 1 && (cx.getLanguageVersion() == Context.VERSION_1_2)) {
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
                result = getElem(cx, o, begin);
            } else {
                if (denseFrom && denseRes) {
                    int intLen = (int) (end - begin);
                    Object[] copy = new Object[intLen];
                    System.arraycopy(na.dense, (int) begin, copy, 0, intLen);
                    nar.dense = copy;
                    try (var map = nar.startCompoundOp(true)) {
                        nar.setLength(map, intLen);
                    }
                } else {
                    for (long last = begin; last != end; last++) {
                        Object temp = getRawElem(o, last);
                        if (temp != NOT_FOUND) {
                            ArrayLikeAbstractOperations.defineElem(
                                    cx, (ScriptableObject) result, last - begin, temp);
                        }
                    }
                    // Need to set length for sparse result array
                    setLengthProperty(cx, (ScriptableObject) result, end - begin);
                }
            }
        } else { // (actualDeleteCount == 0)
            if (cx.getLanguageVersion() == Context.VERSION_1_2) {
                /* Emulate C JS1.2; if no elements are removed, return undefined. */
                result = Undefined.instance;
            }
        }

        /* Find the direction (up or down) to copy and make way for argv. */
        if (denseFrom
                && length + delta < Integer.MAX_VALUE
                && na.ensureCapacity((int) (length + delta))) {
            System.arraycopy(
                    na.dense, (int) end, na.dense, (int) (begin + argc), (int) (length - end));
            if (argc > 0) {
                System.arraycopy(args, 2, na.dense, (int) begin, argc);
            }
            if (delta < 0) {
                Arrays.fill(na.dense, (int) (length + delta), (int) length, NOT_FOUND);
            }
            na.length = length + delta;
            na.modCount++;
            return result;
        }

        if (delta > 0) {
            for (long last = length - 1; last >= end; last--) {
                Object temp = getRawElem(o, last);
                setRawElem(cx, o, last + delta, temp);
            }
        } else if (delta < 0) {
            for (long last = end; last < length; last++) {
                Object temp = getRawElem(o, last);
                setRawElem(cx, o, last + delta, temp);
            }
            // Do this backwards because some implementations might use a
            // non-sparse array and therefore might not be able to handle
            // deleting elements "in the middle". This makes us compatible
            // with older Rhino releases.
            for (long k = length - 1; k >= length + delta; --k) {
                deleteElem(o, k);
            }
        }

        /* Copy from argv into the hole to complete the splice. */
        int argoffset = args.length - argc;
        for (int i = 0; i < argc; i++) {
            setElem(cx, o, begin + i, args[i + argoffset]);
        }

        /* Update length in case we deleted elements from the end. */
        setLengthProperty(cx, o, length + delta);
        return result;
    }

    private static boolean isConcatSpreadable(Context cx, Scriptable scope, Object val) {
        // First, look for the new @@isConcatSpreadable test as per ECMAScript 6 and up
        if (val instanceof Scriptable) {
            final Object spreadable =
                    ScriptableObject.getProperty((Scriptable) val, SymbolKey.IS_CONCAT_SPREADABLE);
            if ((spreadable != Scriptable.NOT_FOUND) && !Undefined.isUndefined(spreadable)) {
                // If @@isConcatSpreadable was undefined, we have to fall back to testing for an
                // array.
                // Otherwise, we found some value
                return ScriptRuntime.toBoolean(spreadable);
            }
        }

        if (cx.getLanguageVersion() < Context.VERSION_ES6) {
            // Otherwise, for older Rhino versions, fall back to the old algorithm, which treats
            // things with the Array constructor as arrays. However, this is contrary to ES6!
            final Constructable ctor = ScriptRuntime.getExistingCtor(cx, scope, "Array");
            if (ScriptRuntime.instanceOf(val, ctor, cx)) {
                return true;
            }
        }

        // Otherwise, it's only spreadable if it's a native array
        return js_isArray(val);
    }

    // Concat elements of "arg" into the destination, with optimizations for native,
    // dense arrays.
    private static long concatSpreadArg(
            Context cx, Scriptable result, Scriptable arg, long offset) {
        long srclen = getLengthProperty(cx, arg);
        long newlen = srclen + offset;

        if (newlen > NativeNumber.MAX_SAFE_INTEGER) {
            throw ScriptRuntime.typeErrorById("msg.arraylength.too.big", newlen);
        }

        // First, optimize for a pair of native, dense arrays
        if ((newlen <= Integer.MAX_VALUE) && (result instanceof NativeArray)) {
            final NativeArray denseResult = (NativeArray) result;
            if (denseResult.denseOnly && (arg instanceof NativeArray)) {
                final NativeArray denseArg = (NativeArray) arg;
                if (denseArg.denseOnly) {
                    // Now we can optimize
                    denseResult.ensureCapacity((int) newlen);
                    System.arraycopy(
                            denseArg.dense, 0, denseResult.dense, (int) offset, (int) srclen);
                    return newlen;
                }
                // We could also optimize here if we are copying to a dense target from a non-dense
                // native array. However, if the source array is very sparse then the result will be
                // very bad -- so don't.
            }
        }

        // If we get here then we have to do things the generic way
        long dstpos = offset;
        for (long srcpos = 0; srcpos < srclen; srcpos++, dstpos++) {
            final Object temp = getRawElem(arg, srcpos);
            if (temp != Scriptable.NOT_FOUND) {
                ArrayLikeAbstractOperations.defineElem(cx, result, dstpos, temp);
            }
        }
        return newlen;
    }

    private static long doConcat(
            Context cx, Scriptable scope, Scriptable result, Object arg, long offset) {
        if (isConcatSpreadable(cx, scope, arg)) {
            return concatSpreadArg(cx, result, (Scriptable) arg, offset);
        }
        ArrayLikeAbstractOperations.defineElem(cx, result, offset, arg);
        return offset + 1;
    }

    /*
     * See Ecma 262v3 15.4.4.4
     */
    private static Scriptable js_concat(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

        // create an empty Array to return.
        scope = getTopLevelScope(scope);
        final Scriptable result = ArrayLikeAbstractOperations.arraySpeciesCreate(cx, scope, o, 0);

        long length = doConcat(cx, scope, result, o, 0);
        for (Object arg : args) {
            length = doConcat(cx, scope, result, arg, length);
        }

        setLengthProperty(cx, result, length);
        return result;
    }

    private static Scriptable js_slice(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

        long len = getLengthProperty(cx, o);

        long begin, end;
        if (args.length == 0) {
            begin = 0;
            end = len;
        } else {
            begin = ArrayLikeAbstractOperations.toSliceIndex(ScriptRuntime.toInteger(args[0]), len);
            if (args.length == 1 || args[1] == Undefined.instance) {
                end = len;
            } else {
                end =
                        ArrayLikeAbstractOperations.toSliceIndex(
                                ScriptRuntime.toInteger(args[1]), len);
            }
        }

        if (end - begin > Integer.MAX_VALUE) {
            String msg = ScriptRuntime.getMessageById("msg.arraylength.bad");
            throw ScriptRuntime.rangeError(msg);
        }

        Scriptable result = ArrayLikeAbstractOperations.arraySpeciesCreate(cx, scope, o, 0);
        for (long slot = begin; slot < end; slot++) {
            Object temp = getRawElem(o, slot);
            if (temp != NOT_FOUND) {
                ArrayLikeAbstractOperations.defineElem(cx, result, slot - begin, temp);
            }
        }
        setLengthProperty(cx, result, Math.max(0, end - begin));

        return result;
    }

    private static Object js_indexOf(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object compareTo = args.length > 0 ? args[0] : Undefined.instance;

        Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);
        long length = getLengthProperty(cx, o);
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
        long start;
        if (args.length < 2) {
            // default
            start = 0;
        } else {
            start = (long) ScriptRuntime.toInteger(args[1]);
            if (start < 0) {
                start += length;
                if (start < 0) start = 0;
            }
            if (start > length - 1) return NEGATIVE_ONE;
        }
        if (o instanceof NativeArray) {
            NativeArray na = (NativeArray) o;
            if (na.denseOnly) {
                Scriptable proto = na.getPrototype();
                for (int i = (int) start; i < length; i++) {
                    Object val = na.dense[i];
                    if (val == NOT_FOUND && proto != null) {
                        val = ScriptableObject.getProperty(proto, i);
                    }
                    if (val != NOT_FOUND && ScriptRuntime.shallowEq(val, compareTo)) {
                        return Long.valueOf(i);
                    }
                }
                return NEGATIVE_ONE;
            }
        }
        for (long i = start; i < length; i++) {
            Object val = getRawElem(o, i);
            if (val != NOT_FOUND && ScriptRuntime.shallowEq(val, compareTo)) {
                return Long.valueOf(i);
            }
        }
        return NEGATIVE_ONE;
    }

    private static Object js_lastIndexOf(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object compareTo = args.length > 0 ? args[0] : Undefined.instance;

        Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);
        long length = getLengthProperty(cx, o);
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
        long start;
        if (args.length < 2) {
            // default
            start = length - 1;
        } else {
            start = (long) ScriptRuntime.toInteger(args[1]);
            if (start >= length) start = length - 1;
            else if (start < 0) start += length;
            if (start < 0) return NEGATIVE_ONE;
        }
        if (o instanceof NativeArray) {
            NativeArray na = (NativeArray) o;
            if (na.denseOnly) {
                Scriptable proto = na.getPrototype();
                for (int i = (int) start; i >= 0; i--) {
                    Object val = na.dense[i];
                    if (val == NOT_FOUND && proto != null) {
                        val = ScriptableObject.getProperty(proto, i);
                    }
                    if (val != NOT_FOUND && ScriptRuntime.shallowEq(val, compareTo)) {
                        return Long.valueOf(i);
                    }
                }
                return NEGATIVE_ONE;
            }
        }
        for (long i = start; i >= 0; i--) {
            Object val = getRawElem(o, i);
            if (val != NOT_FOUND && ScriptRuntime.shallowEq(val, compareTo)) {
                return Long.valueOf(i);
            }
        }
        return NEGATIVE_ONE;
    }

    /*
       See ECMA-262 22.1.3.13
    */
    private static Boolean js_includes(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {

        Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);
        long len = getLengthProperty(cx, o);
        if (len == 0) return Boolean.FALSE;

        long k;
        if (args.length < 2) {
            k = 0;
        } else {
            k = (long) ScriptRuntime.toInteger(args[1]);
            if (k < 0) {
                k += len;
                if (k < 0) k = 0;
            }
            if (k > len - 1) return Boolean.FALSE;
        }

        Object compareTo = args.length > 0 ? args[0] : Undefined.instance;
        if (o instanceof NativeArray) {
            NativeArray na = (NativeArray) o;
            if (na.denseOnly) {
                Scriptable proto = na.getPrototype();
                for (int i = (int) k; i < len; i++) {
                    Object elementK = na.dense[i];
                    if (elementK == NOT_FOUND && proto != null) {
                        elementK = ScriptableObject.getProperty(proto, i);
                    }
                    if (elementK == NOT_FOUND) {
                        elementK = Undefined.instance;
                    }
                    if (ScriptRuntime.sameZero(elementK, compareTo)) {
                        return Boolean.TRUE;
                    }
                }
                return Boolean.FALSE;
            }
        }
        for (; k < len; k++) {
            Object elementK = getRawElem(o, k);
            if (elementK == NOT_FOUND) {
                elementK = Undefined.instance;
            }
            if (ScriptRuntime.sameZero(elementK, compareTo)) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    private static Object js_fill(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);
        long len = getLengthProperty(cx, o);

        long relativeStart = 0;
        if (args.length >= 2) {
            relativeStart = (long) ScriptRuntime.toInteger(args[1]);
        }
        final long k;
        if (relativeStart < 0) {
            k = Math.max((len + relativeStart), 0);
        } else {
            k = Math.min(relativeStart, len);
        }

        long relativeEnd = len;
        if (args.length >= 3 && !Undefined.isUndefined(args[2])) {
            relativeEnd = (long) ScriptRuntime.toInteger(args[2]);
        }
        final long fin;
        if (relativeEnd < 0) {
            fin = Math.max((len + relativeEnd), 0);
        } else {
            fin = Math.min(relativeEnd, len);
        }

        Object value = args.length > 0 ? args[0] : Undefined.instance;
        for (long i = k; i < fin; i++) {
            setRawElem(cx, thisObj, i, value);
        }

        return thisObj;
    }

    private static Object js_copyWithin(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);
        long len = getLengthProperty(cx, o);

        Object targetArg = (args.length >= 1) ? args[0] : Undefined.instance;
        long relativeTarget = (long) ScriptRuntime.toInteger(targetArg);
        long to;
        if (relativeTarget < 0) {
            to = Math.max((len + relativeTarget), 0);
        } else {
            to = Math.min(relativeTarget, len);
        }

        Object startArg = (args.length >= 2) ? args[1] : Undefined.instance;
        long relativeStart = (long) ScriptRuntime.toInteger(startArg);
        long from;
        if (relativeStart < 0) {
            from = Math.max((len + relativeStart), 0);
        } else {
            from = Math.min(relativeStart, len);
        }

        long relativeEnd = len;
        if (args.length >= 3 && !Undefined.isUndefined(args[2])) {
            relativeEnd = (long) ScriptRuntime.toInteger(args[2]);
        }
        final long fin;
        if (relativeEnd < 0) {
            fin = Math.max((len + relativeEnd), 0);
        } else {
            fin = Math.min(relativeEnd, len);
        }

        long count = Math.min(fin - from, len - to);
        int direction = 1;
        if (from < to && to < from + count) {
            direction = -1;
            from = from + count - 1;
            to = to + count - 1;
        }

        // Optimize for a native array. If properties were overridden with setters
        // and other non-default options then we won't get here.
        if ((o instanceof NativeArray) && (count <= Integer.MAX_VALUE)) {
            NativeArray na = (NativeArray) o;
            if (na.denseOnly) {
                for (; count > 0; count--) {
                    na.dense[(int) to] = na.dense[(int) from];
                    from += direction;
                    to += direction;
                }

                return thisObj;
            }
        }

        for (; count > 0; count--) {
            final Object temp = getRawElem(o, from);
            if ((temp == Scriptable.NOT_FOUND) || Undefined.isUndefined(temp)) {
                deleteElem(o, to);
            } else {
                setElem(cx, o, to, temp);
            }

            from += direction;
            to += direction;
        }

        return thisObj;
    }

    private static Object js_at(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);
        long len = getLengthProperty(cx, o);

        long relativeIndex = 0;
        if (args.length >= 1) {
            relativeIndex = (long) ScriptRuntime.toInteger(args[0]);
        }
        long k = (relativeIndex >= 0) ? relativeIndex : len + relativeIndex;
        if ((k < 0) || (k >= len)) {
            return Undefined.instance;
        }
        return getElem(cx, thisObj, k);
    }

    private static Object js_flat(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);
        double depth;
        if (args.length < 1 || Undefined.isUndefined(args[0])) {
            depth = 1;
        } else {
            depth = ScriptRuntime.toInteger(args[0]);
        }

        return flat(cx, scope, o, depth);
    }

    private static Scriptable flat(Context cx, Scriptable scope, Scriptable source, double depth) {
        long length = getLengthProperty(cx, source);

        Scriptable result;
        result = ArrayLikeAbstractOperations.arraySpeciesCreate(cx, scope, source, 0);
        long j = 0;
        for (long i = 0; i < length; i++) {
            Object elem = getRawElem(source, i);
            if (elem == Scriptable.NOT_FOUND) {
                continue;
            }
            if (depth >= 1 && js_isArray(elem)) {
                Scriptable arr = flat(cx, scope, (Scriptable) elem, depth - 1);
                long arrLength = getLengthProperty(cx, arr);
                for (long k = 0; k < arrLength; k++) {
                    Object temp = getRawElem(arr, k);
                    defineElemOrThrow(cx, result, j++, temp);
                }
            } else {
                defineElemOrThrow(cx, result, j++, elem);
            }
        }
        setLengthProperty(cx, result, j);
        return result;
    }

    private static Object js_flatMap(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);
        Object callbackArg = args.length > 0 ? args[0] : Undefined.instance;

        Function f = ArrayLikeAbstractOperations.getCallbackArg(cx, callbackArg);
        Scriptable parent = ScriptableObject.getTopLevelScope(f);
        Scriptable thisArg;
        if (args.length < 2 || args[1] == null || args[1] == Undefined.instance) {
            thisArg = parent;
        } else {
            thisArg = ScriptRuntime.toObject(cx, scope, args[1]);
        }

        long length = getLengthProperty(cx, o);

        Scriptable result = ArrayLikeAbstractOperations.arraySpeciesCreate(cx, scope, o, 0);
        long j = 0;
        for (long i = 0; i < length; i++) {
            Object elem = getRawElem(o, i);
            if (elem == Scriptable.NOT_FOUND) {
                continue;
            }
            Object[] innerArgs = new Object[] {elem, Long.valueOf(i), o};
            Object mapCall = f.call(cx, parent, thisArg, innerArgs);
            if (js_isArray(mapCall)) {
                Scriptable arr = (Scriptable) mapCall;
                long arrLength = getLengthProperty(cx, arr);
                for (long k = 0; k < arrLength; k++) {
                    Object temp = getRawElem(arr, k);
                    defineElemOrThrow(cx, result, j++, temp);
                }
            } else {
                defineElemOrThrow(cx, result, j++, mapCall);
            }
        }
        setLengthProperty(cx, result, j);
        return result;
    }

    private static Object js_every(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return ArrayLikeAbstractOperations.iterativeMethod(
                cx,
                ARRAY_TAG,
                "every",
                IterativeOperation.EVERY,
                scope,
                thisObj,
                args,
                NativeArray::getLengthProperty);
    }

    private static Object js_filter(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return ArrayLikeAbstractOperations.iterativeMethod(
                cx,
                ARRAY_TAG,
                "filter",
                IterativeOperation.FILTER,
                scope,
                thisObj,
                args,
                NativeArray::getLengthProperty);
    }

    private static Object js_forEach(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return ArrayLikeAbstractOperations.iterativeMethod(
                cx,
                ARRAY_TAG,
                "forEach",
                IterativeOperation.FOR_EACH,
                scope,
                thisObj,
                args,
                NativeArray::getLengthProperty);
    }

    private static Object js_map(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return ArrayLikeAbstractOperations.iterativeMethod(
                cx,
                ARRAY_TAG,
                "map",
                IterativeOperation.MAP,
                scope,
                thisObj,
                args,
                NativeArray::getLengthProperty);
    }

    private static Object js_some(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return ArrayLikeAbstractOperations.iterativeMethod(
                cx,
                ARRAY_TAG,
                "some",
                IterativeOperation.SOME,
                scope,
                thisObj,
                args,
                NativeArray::getLengthProperty);
    }

    private static Object js_find(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return ArrayLikeAbstractOperations.iterativeMethod(
                cx,
                ARRAY_TAG,
                "find",
                IterativeOperation.FIND,
                scope,
                thisObj,
                args,
                NativeArray::getLengthProperty);
    }

    private static Object js_findIndex(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return ArrayLikeAbstractOperations.iterativeMethod(
                cx,
                ARRAY_TAG,
                "findIndex",
                IterativeOperation.FIND_INDEX,
                scope,
                thisObj,
                args,
                NativeArray::getLengthProperty);
    }

    private static Object js_findLast(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return ArrayLikeAbstractOperations.iterativeMethod(
                cx,
                ARRAY_TAG,
                "findLast",
                IterativeOperation.FIND_LAST,
                scope,
                thisObj,
                args,
                NativeArray::getLengthProperty);
    }

    private static Object js_findLastIndex(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return ArrayLikeAbstractOperations.iterativeMethod(
                cx,
                ARRAY_TAG,
                "findLastIndex",
                IterativeOperation.FIND_LAST_INDEX,
                scope,
                thisObj,
                args,
                NativeArray::getLengthProperty);
    }

    private static Object js_reduce(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return ArrayLikeAbstractOperations.reduceMethod(
                cx, ReduceOperation.REDUCE, scope, thisObj, args);
    }

    private static Object js_reduceRight(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return ArrayLikeAbstractOperations.reduceMethod(
                cx, ReduceOperation.REDUCE_RIGHT, scope, thisObj, args);
    }

    private static Object js_keys(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        thisObj = ScriptRuntime.toObject(cx, scope, thisObj);
        return new NativeArrayIterator(
                scope, thisObj, NativeArrayIterator.ARRAY_ITERATOR_TYPE.KEYS);
    }

    private static Object js_entries(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        thisObj = ScriptRuntime.toObject(cx, scope, thisObj);
        return new NativeArrayIterator(
                scope, thisObj, NativeArrayIterator.ARRAY_ITERATOR_TYPE.ENTRIES);
    }

    private static Object js_values(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        thisObj = ScriptRuntime.toObject(cx, scope, thisObj);
        return new NativeArrayIterator(
                scope, thisObj, NativeArrayIterator.ARRAY_ITERATOR_TYPE.VALUES);
    }

    private static Object js_isArrayMethod(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return Boolean.valueOf(args.length > 0 && js_isArray(args[0]));
    }

    private static boolean js_isArray(Object o) {
        if (!(o instanceof Scriptable)) {
            return false;
        }
        if (o instanceof NativeProxy) {
            return js_isArray(((NativeProxy) o).getTargetThrowIfRevoked());
        }
        return "Array".equals(((Scriptable) o).getClassName());
    }

    private static Object js_toSorted(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Comparator<Object> comparator =
                ArrayLikeAbstractOperations.getSortComparator(cx, scope, args);

        Scriptable source = ScriptRuntime.toObject(cx, scope, thisObj);
        long len = getLengthProperty(cx, source);

        if (len > Integer.MAX_VALUE) {
            String msg = ScriptRuntime.getMessageById("msg.arraylength.bad");
            throw ScriptRuntime.rangeError(msg);
        }
        Scriptable result = cx.newArray(scope, (int) len);

        for (int k = 0; k < len; ++k) {
            Object fromValue = getElem(cx, source, k);
            setElem(cx, result, k, fromValue);
        }

        sort(cx, result, comparator);
        return result;
    }

    private static Object js_toReversed(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Scriptable source = ScriptRuntime.toObject(cx, scope, thisObj);
        long len = getLengthProperty(cx, source);

        if (len > Integer.MAX_VALUE) {
            String msg = ScriptRuntime.getMessageById("msg.arraylength.bad");
            throw ScriptRuntime.rangeError(msg);
        }
        Scriptable result = cx.newArray(scope, (int) len);

        for (int k = 0; k < len; ++k) {
            int from = (int) len - k - 1;
            Object fromValue = getElem(cx, source, from);
            setElem(cx, result, k, fromValue);
        }

        return result;
    }

    private static Object js_toSpliced(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Scriptable source = ScriptRuntime.toObject(cx, scope, thisObj);
        long len = getLengthProperty(cx, source);

        long actualStart = 0;
        if (args.length > 0) {
            actualStart =
                    ArrayLikeAbstractOperations.toSliceIndex(ScriptRuntime.toInteger(args[0]), len);
        }

        long insertCount = args.length > 2 ? args.length - 2 : 0;

        long actualSkipCount;
        if (args.length == 0) {
            actualSkipCount = 0;
        } else if (args.length == 1) {
            actualSkipCount = len - actualStart;
        } else {
            long sc = ScriptRuntime.toLength(args, 1);
            actualSkipCount = Math.max(0, Math.min(sc, len - actualStart));
        }

        long newLen = len + insertCount - actualSkipCount;
        if (newLen > NativeNumber.MAX_SAFE_INTEGER) {
            throw ScriptRuntime.typeErrorById("msg.arraylength.too.big", newLen);
        }
        if (newLen > Integer.MAX_VALUE) {
            String msg = ScriptRuntime.getMessageById("msg.arraylength.bad");
            throw ScriptRuntime.rangeError(msg);
        }

        Scriptable result = cx.newArray(scope, (int) newLen);

        long i = 0;
        long r = actualStart + actualSkipCount;

        while (i < actualStart) {
            Object e = getElem(cx, source, i);
            setElem(cx, result, i, e);
            i++;
        }

        for (int j = 2; j < args.length; j++) {
            setElem(cx, result, i, args[j]);
            i++;
        }

        while (i < newLen) {
            Object e = getElem(cx, source, r);
            setElem(cx, result, i, e);
            i++;
            r++;
        }

        return result;
    }

    private static Object js_with(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Scriptable source = ScriptRuntime.toObject(cx, scope, thisObj);

        long len = getLengthProperty(cx, source);
        long relativeIndex = args.length > 0 ? (int) ScriptRuntime.toInteger(args[0]) : 0;
        long actualIndex = relativeIndex >= 0 ? relativeIndex : len + relativeIndex;

        if (actualIndex < 0 || actualIndex >= len) {
            throw ScriptRuntime.rangeError("index out of range");
        }
        if (len > Integer.MAX_VALUE) {
            String msg = ScriptRuntime.getMessageById("msg.arraylength.bad");
            throw ScriptRuntime.rangeError(msg);
        }

        Scriptable result = cx.newArray(scope, (int) len);
        for (long k = 0; k < len; ++k) {
            Object value;
            if (k == actualIndex) {
                value = args.length > 1 ? args[1] : Undefined.instance;
            } else {
                value = getElem(cx, source, k);
            }
            setElem(cx, result, k, value);
        }

        return result;
    }

    // methods to implement java.util.List

    @Override
    public boolean contains(Object o) {
        return indexOf(o) > -1;
    }

    @Override
    public Object[] toArray() {
        return toArray(ScriptRuntime.emptyArgs);
    }

    @Override
    public Object[] toArray(Object[] a) {
        int len = size();
        Object[] array =
                a.length >= len
                        ? a
                        : (Object[])
                                java.lang.reflect.Array.newInstance(
                                        a.getClass().getComponentType(), len);
        for (int i = 0; i < len; i++) {
            array[i] = get(i);
        }
        return array;
    }

    @Override
    public boolean containsAll(Collection c) {
        for (Object aC : c) if (!contains(aC)) return false;
        return true;
    }

    @Override
    public int size() {
        long longLen = length;
        if (longLen > Integer.MAX_VALUE) {
            throw new IllegalStateException(
                    "list.length (" + length + ") exceeds Integer.MAX_VALUE");
        }
        return (int) longLen;
    }

    @Override
    public boolean isEmpty() {
        return length == 0;
    }

    public Object get(long index) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("Index: " + index + ", length: " + length);
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

    @Override
    public Object get(int index) {
        return get((long) index);
    }

    @Override
    public int indexOf(Object o) {
        int len = size();
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

    @Override
    public int lastIndexOf(Object o) {
        int len = size();
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

    @Override
    public Iterator iterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator listIterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator listIterator(final int start) {
        final int len = size();

        if (start < 0 || start > len) {
            throw new IndexOutOfBoundsException("Index: " + start + ", length: " + len);
        }

        return new ListIterator() {

            int cursor = start;
            int modCount = NativeArray.this.modCount;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public Object next() {
                checkModCount(modCount);
                if (cursor == len) {
                    throw new NoSuchElementException();
                }
                return get(cursor++);
            }

            @Override
            public boolean hasPrevious() {
                return cursor > 0;
            }

            @Override
            public Object previous() {
                checkModCount(modCount);
                if (cursor == 0) {
                    throw new NoSuchElementException();
                }
                return get(--cursor);
            }

            @Override
            public int nextIndex() {
                return cursor;
            }

            @Override
            public int previousIndex() {
                return cursor - 1;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void add(Object o) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void set(Object o) {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public boolean add(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(int index, Object element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(int index, Collection c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object set(int index, Object element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object remove(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List subList(int fromIndex, int toIndex) {
        if (fromIndex < 0) throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
        if (toIndex > size()) throw new IndexOutOfBoundsException("toIndex = " + toIndex);
        if (fromIndex > toIndex)
            throw new IllegalArgumentException(
                    "fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");

        return new AbstractList() {
            private int mc = NativeArray.this.modCount;

            @Override
            public Object get(int index) {
                checkModCount(mc);
                return NativeArray.this.get(index + fromIndex);
            }

            @Override
            public int size() {
                checkModCount(mc);
                return toIndex - fromIndex;
            }
        };
    }

    private void checkModCount(int modCount) {
        if (this.modCount != modCount) {
            throw new ConcurrentModificationException();
        }
    }

    /** Internal representation of the JavaScript array's length property. */
    private long length;

    /** Attributes of the array's length property */
    private int lengthAttr = DONTENUM | PERMANENT;

    /** modCount required for subList/iterators */
    private transient int modCount;

    /**
     * Fast storage for dense arrays. Sparse arrays will use the superclass's hashtable storage
     * scheme.
     */
    private Object[] dense;

    /** True if all numeric properties are stored in {@code dense}. */
    private boolean denseOnly;

    /** The maximum size of {@code dense} that will be allocated initially. */
    private static int maximumInitialCapacity = 10000;

    /** The default capacity for {@code dense}. */
    private static final int DEFAULT_INITIAL_CAPACITY = 10;

    /** The factor to grow {@code dense} by. */
    private static final double GROW_FACTOR = 1.5;

    private static final int MAX_PRE_GROW_SIZE = (int) (Integer.MAX_VALUE / GROW_FACTOR);
}
