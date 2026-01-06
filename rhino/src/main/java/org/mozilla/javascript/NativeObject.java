/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import org.mozilla.javascript.ScriptRuntime.StringIdOrIndex;

/**
 * This class implements the Object native object. See ECMA 15.2.
 *
 * @author Norris Boyd
 */
public class NativeObject extends ScriptableObject implements Map {
    private static final long serialVersionUID = -6345305608474346996L;

    private static final Object OBJECT_TAG = "Object";
    private static final String CLASS_NAME = "Object";

    public static final String PROTO_PROPERTY = "__proto__";
    public static final String PARENT_PROPERTY = "__parent__";

    private static ClassDescriptor LEGACY_DESCRIPTOR;
    private static ClassDescriptor ES6_DESCRIPTOR;

    static {
        var builder =
                new ClassDescriptor.Builder(
                        CLASS_NAME,
                        1,
                        NativeObject::js_constructorCall,
                        NativeObject::js_constructor);

        builder.withCtorMethod("getPrototypeOf", 1, NativeObject::js_getPrototypeOf);
        builder.withCtorMethod("keys", 1, NativeObject::js_keys);
        builder.withCtorMethod("getOwnPropertyNames", 1, NativeObject::js_getOwnPropertyNames);
        builder.withCtorMethod("getOwnPropertySymbols", 1, NativeObject::js_getOwnPropertySymbols);
        builder.withCtorMethod("getOwnPropertyDescriptor", 2, NativeObject::js_getOwnPropDesc);
        builder.withCtorMethod("getOwnPropertyDescriptors", 1, NativeObject::js_getOwnPropDescs);
        builder.withCtorMethod("defineProperty", 3, NativeObject::js_defineProperty);
        builder.withCtorMethod("isExtensible", 1, NativeObject::js_isExtensible);
        builder.withCtorMethod("preventExtensions", 1, NativeObject::js_preventExtensions);
        builder.withCtorMethod("defineProperties", 2, NativeObject::js_defineProperties);
        builder.withCtorMethod("create", 2, NativeObject::js_create);
        builder.withCtorMethod("isSealed", 1, NativeObject::js_isSealed);
        builder.withCtorMethod("isFrozen", 1, NativeObject::js_isFrozen);
        builder.withCtorMethod("seal", 1, NativeObject::js_seal);
        builder.withCtorMethod("freeze", 1, NativeObject::js_freeze);
        builder.withCtorMethod("assign", 2, NativeObject::js_assign);
        builder.withCtorMethod("is", 2, NativeObject::js_is);
        builder.withCtorMethod("groupBy", 2, NativeObject::js_groupBy);
        builder.withProtoMethod("toString", 0, NativeObject::js_toString);
        builder.withProtoMethod("toLocaleString", 0, NativeObject::js_toLocaleString);
        builder.withProtoMethod("__lookupGetter__", 1, NativeObject::js_lookupGetter);
        builder.withProtoMethod("__lookupSetter__", 1, NativeObject::js_lookupSetter);
        builder.withProtoMethod("__defineGetter__", 2, NativeObject::js_defineGetter);
        builder.withProtoMethod("__defineSetter__", 2, NativeObject::js_defineSetter);
        builder.withProtoMethod("hasOwnProperty", 1, NativeObject::js_hasOwnProperty);
        builder.withProtoMethod("propertyIsEnumerable", 1, NativeObject::js_propertyIsEnumerable);
        builder.withProtoMethod("valueOf", 0, NativeObject::js_valueOf);
        builder.withProtoMethod("isPrototypeOf", 1, NativeObject::js_isPrototypeOf);
        builder.withProtoMethod("toSource", 0, ScriptRuntime::defaultObjectToSource);

        LEGACY_DESCRIPTOR = builder.build();
        ES6_DESCRIPTOR =
                builder.withCtorMethod("setPrototypeOf", 2, NativeObject::js_setPrototypeOf)
                        .withCtorMethod("entries", 1, NativeObject::js_entries)
                        .withCtorMethod("fromEntries", 1, NativeObject::js_fromEntries)
                        .withCtorMethod("values", 1, NativeObject::js_values)
                        .withCtorMethod("hasOwn", 1, NativeObject::js_hasOwn)
                        .withPrototypeProperty(
                                PROTO_PROPERTY,
                                NativeObject::js_protoGetter,
                                NativeObject::js_protoSetter,
                                DONTENUM | READONLY)
                        .build();
    }

    static JSFunction init(Context cx, Scriptable s, boolean sealed) {
        var desc = cx.version >= Context.VERSION_ES6 ? ES6_DESCRIPTOR : LEGACY_DESCRIPTOR;
        return desc.buildConstructor(cx, s, new NativeObject(), sealed);
    }

    @Override
    public String getClassName() {
        return "Object";
    }

    @Override
    public String toString() {
        return ScriptRuntime.defaultObjectToString(this);
    }

    private static Scriptable js_constructorCall(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        if (args.length == 0 || args[0] == null || Undefined.isUndefined(args[0])) {
            return cx.newObject(f.getDeclarationScope());
        }
        return ScriptRuntime.toObject(cx, f.getDeclarationScope(), args[0]);
    }

    private static Scriptable js_constructor(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        if (args.length == 0 || args[0] == null || Undefined.isUndefined(args[0])) {
            return cx.newObject(f.getDeclarationScope());
        }
        return ScriptRuntime.toObject(cx, f.getDeclarationScope(), args[0]);
    }

    private static Object js_toLocaleString(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        if (thisObj == null) {
            throw ScriptRuntime.notFunctionError(null);
        }
        var so = ScriptRuntime.toObject(s, thisObj);
        Object toString = ScriptableObject.getProperty(so, "toString");
        if (!(toString instanceof Callable)) {
            throw ScriptRuntime.notFunctionError(toString);
        }
        Callable fun = (Callable) toString;
        return fun.call(cx, f.getDeclarationScope(), so, ScriptRuntime.emptyArgs);
    }

    private static Object js_toString(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        if (cx.hasFeature(Context.FEATURE_TO_STRING_AS_SOURCE)) {
            String str = ScriptRuntime.defaultObjectToSource(cx, f, nt, s, thisObj, args);
            int L = str.length();
            if (L != 0 && str.charAt(0) == '(' && str.charAt(L - 1) == ')') {
                // Strip () that surrounds toSource
                str = str.substring(1, L - 1);
            }
            return str;
        }
        return ScriptRuntime.defaultObjectToString((Scriptable) thisObj);
    }

    private static Object js_valueOf(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        if (cx.getLanguageVersion() >= Context.VERSION_1_8
                && (thisObj == null || Undefined.isUndefined(thisObj))) {
            throw ScriptRuntime.typeErrorById(
                    "msg." + (thisObj == null ? "null" : "undef") + ".to.object");
        }
        return thisObj;
    }

    private static Object js_hasOwnProperty(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        if (cx.getLanguageVersion() >= Context.VERSION_1_8
                && (thisObj == null || Undefined.isUndefined(thisObj))) {
            throw ScriptRuntime.typeErrorById(
                    "msg." + (thisObj == null ? "null" : "undef") + ".to.object");
        }

        Object arg = args.length < 1 ? Undefined.instance : args[0];

        return AbstractEcmaObjectOperations.hasOwnProperty(cx, thisObj, arg);
    }

    private static Object js_propertyIsEnumerable(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        if (cx.getLanguageVersion() >= Context.VERSION_1_8
                && (thisObj == null || Undefined.isUndefined(thisObj))) {
            throw ScriptRuntime.typeErrorById(
                    "msg." + (thisObj == null ? "null" : "undef") + ".to.object");
        }

        boolean result;
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        Scriptable so = ScriptRuntime.toObject(s, thisObj);

        if (arg instanceof Symbol) {
            result = ((SymbolScriptable) so).has((Symbol) arg, so);
            result = result && isEnumerable((Symbol) arg, thisObj);
        } else {
            StringIdOrIndex soi = ScriptRuntime.toStringIdOrIndex(arg);
            // When checking if a property is enumerable, a missing property should
            // return "false" instead of
            // throwing an exception.  See: https://github.com/mozilla/rhino/issues/415
            try {
                if (soi.stringId == null) {
                    result = so.has(soi.index, so);
                    result = result && isEnumerable(soi.index, so);
                } else {
                    result = so.has(soi.stringId, so);
                    result = result && isEnumerable(soi.stringId, so);
                }
            } catch (EvaluatorException ee) {
                if (ee.getMessage()
                        .startsWith(
                                ScriptRuntime.getMessageById(
                                        "msg.prop.not.found",
                                        soi.stringId == null
                                                ? Integer.toString(soi.index)
                                                : soi.stringId))) {
                    result = false;
                } else {
                    throw ee;
                }
            }
        }
        return ScriptRuntime.wrapBoolean(result);
    }

    private static Object js_isPrototypeOf(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        if (cx.getLanguageVersion() >= Context.VERSION_1_8
                && (thisObj == null || Undefined.isUndefined(thisObj))) {
            throw ScriptRuntime.typeErrorById(
                    "msg." + (thisObj == null ? "null" : "undef") + ".to.object");
        }

        boolean result = false;
        if (args.length != 0 && args[0] instanceof Scriptable) {
            Scriptable v = (Scriptable) args[0];
            do {
                v = v.getPrototype();
                if (v == thisObj) {
                    result = true;
                    break;
                }
            } while (v != null);
        }
        return ScriptRuntime.wrapBoolean(result);
    }

    private static Object js_protoGetter(Scriptable thisObj) {
        /*
        Let O be ? ToObject(this value).
        2. Return ? O.[[GetPrototypeOf]]().
        */
        ScriptableObject o = (ScriptableObject) ScriptRuntime.toObject(thisObj, thisObj);
        return o.getPrototype();
    }

    public static void js_protoSetter(Scriptable thisObj, Object proto) {
        /*
        Let O be ? RequireObjectCoercible(this value).
        2. If proto is not an Object and proto is not null, return undefined.
        3. If O is not an Object, return undefined.
        4. Let status be ? O.[[SetPrototypeOf]](proto).
        5. If status is false, throw a TypeError exception.
        6. Return undefined.
        */

        Object o =
                (ScriptableObject)
                        ScriptRuntimeES6.requireObjectCoercible(
                                null, thisObj, CLASS_NAME, PROTO_PROPERTY);
        if (!(proto instanceof Scriptable) && proto != null) {
            return /* undefined */;
        }

        if (ScriptRuntime.isSymbol(proto)) {
            return;
        }
        if (!(o instanceof Scriptable) || ScriptRuntime.isSymbol(o)) {
            return;
        }
        setPrototypeOf(o, (Scriptable) proto);
    }

    private static Object js_defineGetter(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        return js_defineGetterOrSetter(cx, s, false, thisObj, args);
    }

    private static Object js_defineSetter(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        return js_defineGetterOrSetter(cx, s, true, thisObj, args);
    }

    private static Object js_defineGetterOrSetter(
            Context cx, Scriptable scope, boolean isSetter, Object thisObj, Object[] args) {
        if (args.length < 2 || !(args[1] instanceof Callable)) {
            Object badArg = (args.length >= 2 ? args[1] : Undefined.instance);
            throw ScriptRuntime.notFunctionError(badArg);
        }
        if (!(thisObj instanceof ScriptableObject)) {
            throw Context.reportRuntimeErrorById(
                    "msg.extend.scriptable",
                    thisObj == null ? "null" : thisObj.getClass().getName(),
                    String.valueOf(args[0]));
        }
        ScriptableObject so = (ScriptableObject) thisObj;
        StringIdOrIndex s = ScriptRuntime.toStringIdOrIndex(args[0]);
        int index = s.stringId != null ? 0 : s.index;
        Callable getterOrSetter = (Callable) args[1];
        so.setGetterOrSetter(s.stringId, index, getterOrSetter, isSetter);
        if (so instanceof NativeArray) ((NativeArray) so).setDenseOnly(false);

        return Undefined.instance;
    }

    private static Object js_lookupGetter(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        return js_lookupGetterOrSetter(cx, s, false, thisObj, args);
    }

    private static Object js_lookupSetter(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        return js_lookupGetterOrSetter(cx, s, true, thisObj, args);
    }

    private static Object js_lookupGetterOrSetter(
            Context cx, Scriptable scope, boolean isSetter, Object thisObj, Object[] args) {
        if (args.length < 1 || !(thisObj instanceof ScriptableObject)) return Undefined.instance;

        ScriptableObject so = (ScriptableObject) thisObj;
        StringIdOrIndex s = ScriptRuntime.toStringIdOrIndex(args[0]);
        int index = s.stringId != null ? 0 : s.index;
        Object gs;
        for (; ; ) {
            gs = so.getGetterOrSetter(s.stringId, index, scope, isSetter);
            if (gs != null) {
                break;
            }
            // If there is no getter or setter for the object itself,
            // how about the prototype?
            Scriptable v = so.getPrototype();
            if (v == null) {
                break;
            }
            if (v instanceof ScriptableObject) {
                so = (ScriptableObject) v;
            } else {
                break;
            }
        }
        if (gs != null) {
            return gs;
        }
        return Undefined.instance;
    }

    private static Object js_getPrototypeOf(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        Scriptable obj = getCompatibleObject(cx, s, arg);
        return obj.getPrototype();
    }

    private static Object js_setPrototypeOf(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        if (args.length < 2) {
            throw ScriptRuntime.typeErrorById(
                    "msg.method.missing.parameter",
                    "Object.setPrototypeOf",
                    "2",
                    Integer.toString(args.length));
        }
        Scriptable proto = (args[1] == null) ? null : ensureScriptable(args[1]);
        if (ScriptRuntime.isSymbol(proto)) {
            throw ScriptRuntime.typeErrorById("msg.arg.not.object", ScriptRuntime.typeof(proto));
        }

        final Object arg0 = args[0];
        if (cx.getLanguageVersion() >= Context.VERSION_ES6) {
            ScriptRuntimeES6.requireObjectCoercible(cx, arg0, OBJECT_TAG, "setPrototypeOf");
        }
        return setPrototypeOf(arg0, proto);
    }

    private static Object setPrototypeOf(Object thisObj, Scriptable proto) {
        if (!(thisObj instanceof ScriptableObject)) {
            return thisObj;
        }
        ScriptableObject thisScriptable = (ScriptableObject) thisObj;
        if (thisScriptable.getPrototype() == proto) {
            return thisObj;
        }
        if (!thisScriptable.isExtensible()) {
            throw ScriptRuntime.typeErrorById("msg.not.extensible");
        }

        // cycle detection
        Scriptable prototypeProto = proto;
        while (prototypeProto != null) {
            if (prototypeProto == thisScriptable) {
                throw ScriptRuntime.typeErrorById(
                        "msg.object.cyclic.prototype", thisScriptable.getClass().getSimpleName());
            }
            prototypeProto = prototypeProto.getPrototype();
        }
        thisScriptable.setPrototype(proto);
        return thisScriptable;
    }

    private static Object js_keys(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        Scriptable obj = getCompatibleObject(cx, f.getDeclarationScope(), arg);
        Object[] ids = obj.getIds();
        for (int i = 0; i < ids.length; i++) {
            ids[i] = ScriptRuntime.toString(ids[i]);
        }
        return cx.newArray(f.getDeclarationScope(), ids);
    }

    private static Object js_entries(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        Scriptable obj = getCompatibleObject(cx, s, arg);
        Object[] ids = obj.getIds();
        int j = 0;
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] instanceof Integer) {
                int intId = (Integer) ids[i];
                if (obj.has(intId, obj) && isEnumerable(intId, obj)) {
                    String stringId = ScriptRuntime.toString(ids[i]);
                    Object[] entry = new Object[] {stringId, obj.get(intId, obj)};
                    ids[j++] = cx.newArray(s, entry);
                }
            } else {
                String stringId = ScriptRuntime.toString(ids[i]);
                if (obj.has(stringId, obj) && isEnumerable(stringId, obj)) {
                    Object[] entry = new Object[] {stringId, obj.get(stringId, obj)};
                    ids[j++] = cx.newArray(s, entry);
                }
            }
        }
        if (j != ids.length) {
            ids = Arrays.copyOf(ids, j);
        }
        return cx.newArray(s, ids);
    }

    private static Object js_fromEntries(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        arg = getCompatibleObject(cx, f.getDeclarationScope(), arg);
        Scriptable obj = cx.newObject(f.getDeclarationScope());
        ScriptRuntime.loadFromIterable(
                cx,
                s,
                arg,
                (key, value) -> {
                    if (key instanceof Integer) {
                        obj.put((Integer) key, obj, value);
                    } else if (key instanceof Symbol && obj instanceof SymbolScriptable) {
                        // using instanceof is correct here
                        // (see org.mozilla.javascript.tests.es6.Symbol3Test.fromEntries)
                        ((SymbolScriptable) obj).put((Symbol) key, obj, value);
                    } else {
                        obj.put(ScriptRuntime.toString(key), obj, value);
                    }
                });
        return obj;
    }

    private static Object js_values(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        Scriptable obj = getCompatibleObject(cx, s, arg);
        Object[] ids = obj.getIds();
        int j = 0;
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] instanceof Integer) {
                int intId = (Integer) ids[i];
                if (obj.has(intId, obj) && isEnumerable(intId, obj)) {
                    ids[j++] = obj.get(intId, obj);
                }
            } else {
                String stringId = ScriptRuntime.toString(ids[i]);
                // getter may remove keys
                if (obj.has(stringId, obj) && isEnumerable(stringId, obj)) {
                    ids[j++] = obj.get(stringId, obj);
                }
            }
        }
        if (j != ids.length) {
            ids = Arrays.copyOf(ids, j);
        }
        return cx.newArray(s, ids);
    }

    private static Object js_hasOwn(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        Object propertyName = args.length < 2 ? Undefined.instance : args[1];
        return AbstractEcmaObjectOperations.hasOwnProperty(cx, arg, propertyName);
    }

    private static Object js_getOwnPropertyNames(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        Scriptable s2 = getCompatibleObject(cx, s, arg);
        ScriptableObject obj = ensureScriptableObject(s2);
        Object[] ids;
        try (var map = obj.startCompoundOp(false)) {
            ids = obj.getIds(map, true, false);
        }
        for (int i = 0; i < ids.length; i++) {
            ids[i] = ScriptRuntime.toString(ids[i]);
        }
        return cx.newArray(s, ids);
    }

    private static Object js_getOwnPropertySymbols(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        Scriptable s2 = getCompatibleObject(cx, s, arg);
        ScriptableObject obj = ensureScriptableObject(s2);
        Object[] ids;
        try (var map = obj.startCompoundOp(false)) {
            ids = obj.getIds(map, true, true);
        }
        ArrayList<Object> syms = new ArrayList<>();
        for (Object o : ids) {
            if (o instanceof Symbol) {
                syms.add(o);
            }
        }
        return cx.newArray(s, syms.toArray());
    }

    private static Object js_getOwnPropDesc(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        // TODO(norris): There's a deeper issue here if
        // arg instanceof Scriptable. Should we create a new
        // interface to admit the new ECMAScript 5 operations?
        Scriptable s2 = getCompatibleObject(cx, s, arg);
        ScriptableObject obj = ensureScriptableObject(s2);
        Object nameArg = args.length < 2 ? Undefined.instance : args[1];
        var desc = obj.getOwnPropertyDescriptor(cx, nameArg);
        return desc == null ? Undefined.instance : desc.toObject(f.getDeclarationScope());
    }

    private static Object js_getOwnPropDescs(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        Scriptable s2 = getCompatibleObject(cx, s, arg);
        ScriptableObject obj = ensureScriptableObject(s2);

        ScriptableObject descs = (ScriptableObject) cx.newObject(f.getDeclarationScope());
        Object[] ids;
        try (var map = obj.startCompoundOp(false)) {
            ids = obj.getIds(map, true, true);
        }
        for (Object key : ids) {
            var desc = obj.getOwnPropertyDescriptor(cx, key);
            if (desc == null) {
                continue;
            } else if (key instanceof Symbol) {
                descs.put((Symbol) key, descs, desc.toObject(s));
            } else if (key instanceof Integer) {
                descs.put((Integer) key, descs, desc.toObject(s));
            } else {
                descs.put(ScriptRuntime.toString(key), descs, desc.toObject(s));
            }
        }
        return descs;
    }

    private static Object js_defineProperty(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        ScriptableObject obj = ensureScriptableObject(arg);
        Object name = args.length < 2 ? Undefined.instance : args[1];
        Object descArg = args.length < 3 ? Undefined.instance : args[2];
        var desc = new DescriptorInfo(ensureScriptableObject(descArg));
        ScriptableObject.checkPropertyDefinition(desc);
        obj.defineOwnProperty(cx, name, desc);
        return obj;
    }

    private static Object js_isExtensible(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        if (cx.getLanguageVersion() >= Context.VERSION_ES6 && !(arg instanceof ScriptableObject)) {
            return Boolean.FALSE;
        }

        ScriptableObject obj = ensureScriptableObject(arg);
        return Boolean.valueOf(obj.isExtensible());
    }

    private static Object js_preventExtensions(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        if (cx.getLanguageVersion() >= Context.VERSION_ES6 && !(arg instanceof ScriptableObject)) {
            return arg;
        }

        ScriptableObject obj = ensureScriptableObject(arg);
        if (!obj.preventExtensions()) {
            throw ScriptRuntime.typeError("Object.preventExtensions is not allowed");
        }
        return obj;
    }

    private static Object js_defineProperties(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        ScriptableObject obj = ensureScriptableObject(arg);
        Object propsObj = args.length < 2 ? Undefined.instance : args[1];
        Scriptable props = Context.toObject(propsObj, s);
        obj.defineOwnProperties(cx, ensureScriptableObject(props));
        return obj;
    }

    private static Object js_create(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        Scriptable obj = (arg == null) ? null : ensureScriptable(arg);

        ScriptableObject newObject = new NativeObject();
        newObject.setParentScope(s);
        newObject.setPrototype(obj);

        if (args.length > 1 && !Undefined.isUndefined(args[1])) {
            Scriptable props = Context.toObject(args[1], s);
            newObject.defineOwnProperties(cx, ensureScriptableObject(props));
        }

        return newObject;
    }

    private static Object js_isSealed(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        if (cx.getLanguageVersion() >= Context.VERSION_ES6 && !(arg instanceof ScriptableObject)) {
            return Boolean.TRUE;
        }

        return AbstractEcmaObjectOperations.testIntegrityLevel(
                cx, arg, AbstractEcmaObjectOperations.INTEGRITY_LEVEL.SEALED);
    }

    private static Object js_isFrozen(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        if (cx.getLanguageVersion() >= Context.VERSION_ES6 && !(arg instanceof ScriptableObject)) {
            return Boolean.TRUE;
        }

        return AbstractEcmaObjectOperations.testIntegrityLevel(
                cx, arg, AbstractEcmaObjectOperations.INTEGRITY_LEVEL.FROZEN);
    }

    private static Object js_seal(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        if (cx.getLanguageVersion() >= Context.VERSION_ES6 && !(arg instanceof ScriptableObject)) {
            return arg;
        }

        boolean status =
                AbstractEcmaObjectOperations.setIntegrityLevel(
                        cx, arg, AbstractEcmaObjectOperations.INTEGRITY_LEVEL.SEALED);
        if (!status) {
            throw ScriptRuntime.typeError("Object is not sealable");
        }
        return arg;
    }

    private static Object js_freeze(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        if (cx.getLanguageVersion() >= Context.VERSION_ES6 && !(arg instanceof ScriptableObject)) {
            return arg;
        }

        boolean status =
                AbstractEcmaObjectOperations.setIntegrityLevel(
                        cx, arg, AbstractEcmaObjectOperations.INTEGRITY_LEVEL.FROZEN);
        if (!status) {
            throw ScriptRuntime.typeError("Object is not freezable");
        }

        return arg;
    }

    private static Object js_assign(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        Scriptable targetObj;
        if (args.length > 0) {
            targetObj = ScriptRuntime.toObject(cx, s, args[0]);
        } else {
            targetObj = ScriptRuntime.toObject(cx, s, Undefined.instance);
        }
        for (int i = 1; i < args.length; i++) {
            if ((args[i] == null) || Undefined.isUndefined(args[i])) {
                continue;
            }
            Scriptable sourceObj = ScriptRuntime.toObject(cx, s, args[i]);
            Object[] ids;
            if (sourceObj instanceof ScriptableObject) {
                var scriptable = (ScriptableObject) sourceObj;
                try (var map = scriptable.startCompoundOp(false)) {
                    ids = scriptable.getIds(map, false, true);
                }
            } else {
                ids = sourceObj.getIds();
            }
            for (Object key : ids) {
                if (key instanceof Integer) {
                    int intId = (Integer) key;
                    if (sourceObj.has(intId, sourceObj) && isEnumerable(intId, sourceObj)) {
                        Object val = sourceObj.get(intId, sourceObj);
                        AbstractEcmaObjectOperations.put(cx, targetObj, intId, val, true);
                    }
                } else if (key instanceof String) {
                    String stringId = ScriptRuntime.toString(key);
                    if (sourceObj.has(stringId, sourceObj) && isEnumerable(stringId, sourceObj)) {
                        Object val = sourceObj.get(stringId, sourceObj);
                        AbstractEcmaObjectOperations.put(cx, targetObj, stringId, val, true);
                    }
                }
            }

            // This is a separate loop for Symbols, as they must be
            // copied over after string properties
            if (sourceObj instanceof ScriptableObject) {
                for (Object key : ids) {
                    if (key instanceof Symbol) {
                        Symbol sym = (Symbol) key;
                        if (((ScriptableObject) sourceObj).has(sym, sourceObj)
                                && isEnumerable(sym, sourceObj)) {
                            Object val = ((ScriptableObject) sourceObj).get(sym, sourceObj);
                            AbstractEcmaObjectOperations.put(cx, targetObj, sym, val, true);
                        }
                    }
                }
            }
        }
        return targetObj;
    }

    private static Object js_is(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        Object a1 = args.length < 1 ? Undefined.instance : args[0];
        Object a2 = args.length < 2 ? Undefined.instance : args[1];
        return ScriptRuntime.wrapBoolean(ScriptRuntime.same(a1, a2));
    }

    private static Object js_groupBy(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        Object items = args.length < 1 ? Undefined.instance : args[0];
        Object callback = args.length < 2 ? Undefined.instance : args[1];

        Map<Object, List<Object>> groups =
                AbstractEcmaObjectOperations.groupBy(
                        cx,
                        s,
                        OBJECT_TAG,
                        "groupBy",
                        items,
                        callback,
                        AbstractEcmaObjectOperations.KEY_COERCION.PROPERTY);

        NativeObject obj = (NativeObject) cx.newObject(s);
        obj.setPrototype(null);

        for (Map.Entry<Object, List<Object>> entry : groups.entrySet()) {
            Scriptable elements = cx.newArray(s, entry.getValue().toArray());

            ScriptableObject desc = (ScriptableObject) cx.newObject(f.getDeclarationScope());
            desc.put("enumerable", desc, Boolean.TRUE);
            desc.put("configurable", desc, Boolean.TRUE);
            desc.put("value", desc, elements);

            obj.defineOwnProperty(cx, entry.getKey(), desc);
        }

        return obj;
    }

    private static boolean isEnumerable(int index, Object obj) {
        if (obj instanceof ScriptableObject) {
            ScriptableObject so = (ScriptableObject) obj;
            try {
                int attrs = so.getAttributes(index);
                return (attrs & ScriptableObject.DONTENUM) == 0;
            } catch (RhinoException re) {
                // Not all ScriptableObject implementations implement
                // "getAttributes" for all properties
                return true;
            }
        } else {
            return true;
        }
    }

    private static boolean isEnumerable(String key, Object obj) {
        if (obj instanceof ScriptableObject) {
            ScriptableObject so = (ScriptableObject) obj;
            try {
                int attrs = so.getAttributes(key);
                return (attrs & ScriptableObject.DONTENUM) == 0;
            } catch (RhinoException re) {
                return true;
            }
        } else {
            return true;
        }
    }

    private static boolean isEnumerable(Symbol sym, Object obj) {
        if (obj instanceof ScriptableObject) {
            ScriptableObject so = (ScriptableObject) obj;
            try {
                int attrs = so.getAttributes(sym);
                return (attrs & ScriptableObject.DONTENUM) == 0;
            } catch (RhinoException re) {
                return true;
            }
        } else {
            return true;
        }
    }

    private static Scriptable getCompatibleObject(Context cx, Scriptable scope, Object arg) {
        if (cx.getLanguageVersion() >= Context.VERSION_ES6) {
            Scriptable s = ScriptRuntime.toObject(cx, scope, arg);
            return ensureScriptable(s);
        }
        return ensureScriptable(arg);
    }

    // methods implementing java.util.Map

    @Override
    public boolean containsKey(Object key) {
        if (key instanceof String) {
            return has((String) key, this);
        } else if (key instanceof Number) {
            return has(((Number) key).intValue(), this);
        }
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        for (Object obj : values()) {
            if (Objects.equals(value, obj)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object remove(Object key) {
        Object value = get(key);
        if (key instanceof String) {
            delete((String) key);
        } else if (key instanceof Number) {
            delete(((Number) key).intValue());
        }
        return value;
    }

    @Override
    public Set<Object> keySet() {
        return new KeySet();
    }

    @Override
    public Collection<Object> values() {
        return new ValueCollection();
    }

    @Override
    public Set<Map.Entry<Object, Object>> entrySet() {
        return new EntrySet();
    }

    @Override
    public Object put(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    class EntrySet extends AbstractSet<Entry<Object, Object>> {
        @Override
        public Iterator<Entry<Object, Object>> iterator() {
            return new Iterator<Map.Entry<Object, Object>>() {
                Object[] ids = getIds();
                Object key = null;
                int index = 0;

                @Override
                public boolean hasNext() {
                    return index < ids.length;
                }

                @Override
                public Map.Entry<Object, Object> next() {
                    final Object ekey = key = ids[index++];
                    final Object value = get(key);
                    return new Map.Entry<Object, Object>() {
                        @Override
                        public Object getKey() {
                            return ekey;
                        }

                        @Override
                        public Object getValue() {
                            return value;
                        }

                        @Override
                        public Object setValue(Object value) {
                            throw new UnsupportedOperationException();
                        }

                        @Override
                        public boolean equals(Object other) {
                            if (!(other instanceof Map.Entry)) {
                                return false;
                            }
                            Map.Entry<?, ?> e = (Map.Entry<?, ?>) other;
                            return (ekey == null ? e.getKey() == null : ekey.equals(e.getKey()))
                                    && (value == null
                                            ? e.getValue() == null
                                            : value.equals(e.getValue()));
                        }

                        @Override
                        public int hashCode() {
                            return (ekey == null ? 0 : ekey.hashCode())
                                    ^ (value == null ? 0 : value.hashCode());
                        }

                        @Override
                        public String toString() {
                            return ekey + "=" + value;
                        }
                    };
                }

                @Override
                public void remove() {
                    if (key == null) {
                        throw new IllegalStateException();
                    }
                    NativeObject.this.remove(key);
                    key = null;
                }
            };
        }

        @Override
        public int size() {
            return NativeObject.this.size();
        }
    }

    class KeySet extends AbstractSet<Object> {

        @Override
        public boolean contains(Object key) {
            return containsKey(key);
        }

        @Override
        public Iterator<Object> iterator() {
            return new Iterator<Object>() {
                Object[] ids = getIds();
                Object key;
                int index = 0;

                @Override
                public boolean hasNext() {
                    return index < ids.length;
                }

                @Override
                public Object next() {
                    try {
                        return (key = ids[index++]);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        key = null;
                        throw new NoSuchElementException();
                    }
                }

                @Override
                public void remove() {
                    if (key == null) {
                        throw new IllegalStateException();
                    }
                    NativeObject.this.remove(key);
                    key = null;
                }
            };
        }

        @Override
        public int size() {
            return NativeObject.this.size();
        }
    }

    class ValueCollection extends AbstractCollection<Object> {

        @Override
        public Iterator<Object> iterator() {
            return new Iterator<Object>() {
                Object[] ids = getIds();
                Object key;
                int index = 0;

                @Override
                public boolean hasNext() {
                    return index < ids.length;
                }

                @Override
                public Object next() {
                    return get((key = ids[index++]));
                }

                @Override
                public void remove() {
                    if (key == null) {
                        throw new IllegalStateException();
                    }
                    NativeObject.this.remove(key);
                    key = null;
                }
            };
        }

        @Override
        public int size() {
            return NativeObject.this.size();
        }
    }
}
