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

    static void init(Scriptable s, boolean sealed) {
        LambdaConstructor ctor =
                new LambdaConstructor(
                        s,
                        CLASS_NAME,
                        1,
                        NativeObject::js_constructorCall,
                        NativeObject::js_constructor) {
                    @Override
                    public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
                        return js_constructor(cx, scope, args);
                    }
                };

        defOnCtor(ctor, s, "getPrototypeOf", 1, NativeObject::js_getPrototypeOf);
        if (Context.getCurrentContext().version >= Context.VERSION_ES6) {
            defOnCtor(ctor, s, "setPrototypeOf", 2, NativeObject::js_setPrototypeOf);
            defOnCtor(ctor, s, "entries", 1, NativeObject::js_entries);
            defOnCtor(ctor, s, "fromEntries", 1, NativeObject::js_fromEntries);
            defOnCtor(ctor, s, "values", 1, NativeObject::js_values);
            defOnCtor(ctor, s, "hasOwn", 1, NativeObject::js_hasOwn);
        }
        defOnCtor(ctor, s, "keys", 1, NativeObject::js_keys);
        defOnCtor(ctor, s, "getOwnPropertyNames", 1, NativeObject::js_getOwnPropertyNames);
        defOnCtor(ctor, s, "getOwnPropertySymbols", 1, NativeObject::js_getOwnPropertySymbols);
        defOnCtor(ctor, s, "getOwnPropertyDescriptor", 2, NativeObject::js_getOwnPropDesc);
        defOnCtor(ctor, s, "getOwnPropertyDescriptors", 1, NativeObject::js_getOwnPropDescs);
        defOnCtor(ctor, s, "defineProperty", 3, NativeObject::js_defineProperty);
        defOnCtor(ctor, s, "isExtensible", 1, NativeObject::js_isExtensible);
        defOnCtor(ctor, s, "preventExtensions", 1, NativeObject::js_preventExtensions);
        defOnCtor(ctor, s, "defineProperties", 2, NativeObject::js_defineProperties);
        defOnCtor(ctor, s, "create", 2, NativeObject::js_create);
        defOnCtor(ctor, s, "isSealed", 1, NativeObject::js_isSealed);
        defOnCtor(ctor, s, "isFrozen", 1, NativeObject::js_isFrozen);
        defOnCtor(ctor, s, "seal", 1, NativeObject::js_seal);
        defOnCtor(ctor, s, "freeze", 1, NativeObject::js_freeze);
        defOnCtor(ctor, s, "assign", 2, NativeObject::js_assign);
        defOnCtor(ctor, s, "is", 2, NativeObject::js_is);
        defOnCtor(ctor, s, "groupBy", 2, NativeObject::js_groupBy);

        defOnProto(ctor, s, "toString", 0, NativeObject::js_toString);
        defOnProto(ctor, s, "toLocaleString", 0, NativeObject::js_toLocaleString);
        defOnProto(ctor, s, "__lookupGetter__", 1, NativeObject::js_lookupGetter);
        defOnProto(ctor, s, "__lookupSetter__", 1, NativeObject::js_lookupSetter);
        defOnProto(ctor, s, "__defineGetter__", 2, NativeObject::js_defineGetter);
        defOnProto(ctor, s, "__defineSetter__", 2, NativeObject::js_defineSetter);
        defOnProto(ctor, s, "hasOwnProperty", 1, NativeObject::js_hasOwnProperty);
        defOnProto(ctor, s, "propertyIsEnumerable", 1, NativeObject::js_propertyIsEnumerable);
        defOnProto(ctor, s, "valueOf", 0, NativeObject::js_valueOf);
        defOnProto(ctor, s, "isPrototypeOf", 1, NativeObject::js_isPrototypeOf);
        defOnProto(ctor, s, "toSource", 0, ScriptRuntime::defaultObjectToSource);

        ctor.setPrototypePropertyAttributes(PERMANENT | READONLY | DONTENUM);
        ScriptableObject.defineProperty(s, CLASS_NAME, ctor, DONTENUM);
        if (sealed) {
            ctor.sealObject();
            ((NativeObject) ctor.getPrototypeProperty()).sealObject();
        }
    }

    private static void defOnCtor(
            LambdaConstructor constructor,
            Scriptable scope,
            String name,
            int length,
            SerializableCallable target) {
        constructor.defineConstructorMethod(
                scope, name, length, null, target, DONTENUM, DONTENUM | READONLY);
    }

    private static void defOnProto(
            LambdaConstructor constructor,
            Scriptable scope,
            String name,
            int length,
            SerializableCallable target) {
        constructor.definePrototypeMethod(
                scope, name, length, null, target, DONTENUM, DONTENUM | READONLY);
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
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (args.length == 0 || args[0] == null || Undefined.isUndefined(args[0])) {
            return cx.newObject(scope);
        }
        return ScriptRuntime.toObject(cx, scope, args[0]);
    }

    private static Scriptable js_constructor(Context cx, Scriptable scope, Object[] args) {
        if (args.length == 0 || args[0] == null || Undefined.isUndefined(args[0])) {
            return cx.newObject(scope);
        }
        return ScriptRuntime.toObject(cx, scope, args[0]);
    }

    private static Object js_toLocaleString(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (thisObj == null) {
            throw ScriptRuntime.notFunctionError(null);
        }
        Object toString = ScriptableObject.getProperty(thisObj, "toString");
        if (!(toString instanceof Callable)) {
            throw ScriptRuntime.notFunctionError(toString);
        }
        Callable fun = (Callable) toString;
        return fun.call(cx, scope, thisObj, ScriptRuntime.emptyArgs);
    }

    private static Object js_toString(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (cx.hasFeature(Context.FEATURE_TO_STRING_AS_SOURCE)) {
            String s =
                    ScriptRuntime.defaultObjectToSource(
                            cx, scope,
                            thisObj, args);
            int L = s.length();
            if (L != 0 && s.charAt(0) == '(' && s.charAt(L - 1) == ')') {
                // Strip () that surrounds toSource
                s = s.substring(1, L - 1);
            }
            return s;
        }
        return ScriptRuntime.defaultObjectToString(thisObj);
    }

    private static Object js_valueOf(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (cx.getLanguageVersion() >= Context.VERSION_1_8
                && (thisObj == null || Undefined.isUndefined(thisObj))) {
            throw ScriptRuntime.typeErrorById(
                    "msg." + (thisObj == null ? "null" : "undef") + ".to.object");
        }
        return thisObj;
    }

    private static Object js_hasOwnProperty(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (cx.getLanguageVersion() >= Context.VERSION_1_8
                && (thisObj == null || Undefined.isUndefined(thisObj))) {
            throw ScriptRuntime.typeErrorById(
                    "msg." + (thisObj == null ? "null" : "undef") + ".to.object");
        }

        Object arg = args.length < 1 ? Undefined.instance : args[0];

        return AbstractEcmaObjectOperations.hasOwnProperty(cx, thisObj, arg);
    }

    private static Object js_propertyIsEnumerable(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (cx.getLanguageVersion() >= Context.VERSION_1_8
                && (thisObj == null || Undefined.isUndefined(thisObj))) {
            throw ScriptRuntime.typeErrorById(
                    "msg." + (thisObj == null ? "null" : "undef") + ".to.object");
        }

        boolean result;
        Object arg = args.length < 1 ? Undefined.instance : args[0];

        if (arg instanceof Symbol) {
            result = ((SymbolScriptable) thisObj).has((Symbol) arg, thisObj);
            result = result && isEnumerable((Symbol) arg, thisObj);
        } else {
            StringIdOrIndex s = ScriptRuntime.toStringIdOrIndex(arg);
            // When checking if a property is enumerable, a missing property should
            // return "false" instead of
            // throwing an exception.  See: https://github.com/mozilla/rhino/issues/415
            try {
                if (s.stringId == null) {
                    result = thisObj.has(s.index, thisObj);
                    result = result && isEnumerable(s.index, thisObj);
                } else {
                    result = thisObj.has(s.stringId, thisObj);
                    result = result && isEnumerable(s.stringId, thisObj);
                }
            } catch (EvaluatorException ee) {
                if (ee.getMessage()
                        .startsWith(
                                ScriptRuntime.getMessageById(
                                        "msg.prop.not.found",
                                        s.stringId == null
                                                ? Integer.toString(s.index)
                                                : s.stringId))) {
                    result = false;
                } else {
                    throw ee;
                }
            }
        }
        return ScriptRuntime.wrapBoolean(result);
    }

    private static Object js_isPrototypeOf(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
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

    private static Object js_defineGetter(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return js_defineGetterOrSetter(cx, scope, false, thisObj, args);
    }

    private static Object js_defineSetter(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return js_defineGetterOrSetter(cx, scope, true, thisObj, args);
    }

    private static Object js_defineGetterOrSetter(
            Context cx, Scriptable scope, boolean isSetter, Scriptable thisObj, Object[] args) {
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
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return js_lookupGetterOrSetter(cx, scope, false, thisObj, args);
    }

    private static Object js_lookupSetter(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return js_lookupGetterOrSetter(cx, scope, true, thisObj, args);
    }

    private static Object js_lookupGetterOrSetter(
            Context cx, Scriptable scope, boolean isSetter, Scriptable thisObj, Object[] args) {
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
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        Scriptable obj = getCompatibleObject(cx, scope, arg);
        return obj.getPrototype();
    }

    private static Object js_setPrototypeOf(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (args.length < 2) {
            throw ScriptRuntime.typeErrorById(
                    "msg.method.missing.parameter",
                    "Object.setPrototypeOf",
                    "2",
                    Integer.toString(args.length));
        }
        Scriptable proto = (args[1] == null) ? null : ensureScriptable(args[1]);
        if (proto instanceof Symbol) {
            throw ScriptRuntime.typeErrorById("msg.arg.not.object", ScriptRuntime.typeof(proto));
        }

        final Object arg0 = args[0];
        if (cx.getLanguageVersion() >= Context.VERSION_ES6) {
            ScriptRuntimeES6.requireObjectCoercible(cx, arg0, OBJECT_TAG, "setPrototypeOf");
        }
        if (!(arg0 instanceof ScriptableObject)) {
            return arg0;
        }
        ScriptableObject obj = (ScriptableObject) arg0;
        if (!obj.isExtensible()) {
            throw ScriptRuntime.typeErrorById("msg.not.extensible");
        }

        // cycle detection
        Scriptable prototypeProto = proto;
        while (prototypeProto != null) {
            if (prototypeProto == obj) {
                throw ScriptRuntime.typeErrorById(
                        "msg.object.cyclic.prototype", obj.getClass().getSimpleName());
            }
            prototypeProto = prototypeProto.getPrototype();
        }
        obj.setPrototype(proto);
        return obj;
    }

    private static Object js_keys(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        Scriptable obj = getCompatibleObject(cx, scope, arg);
        Object[] ids = obj.getIds();
        for (int i = 0; i < ids.length; i++) {
            ids[i] = ScriptRuntime.toString(ids[i]);
        }
        return cx.newArray(scope, ids);
    }

    private static Object js_entries(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        Scriptable obj = getCompatibleObject(cx, scope, arg);
        Object[] ids = obj.getIds();
        int j = 0;
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] instanceof Integer) {
                int intId = (Integer) ids[i];
                if (obj.has(intId, obj) && isEnumerable(intId, obj)) {
                    String stringId = ScriptRuntime.toString(ids[i]);
                    Object[] entry = new Object[] {stringId, obj.get(intId, obj)};
                    ids[j++] = cx.newArray(scope, entry);
                }
            } else {
                String stringId = ScriptRuntime.toString(ids[i]);
                if (obj.has(stringId, obj) && isEnumerable(stringId, obj)) {
                    Object[] entry = new Object[] {stringId, obj.get(stringId, obj)};
                    ids[j++] = cx.newArray(scope, entry);
                }
            }
        }
        if (j != ids.length) {
            ids = Arrays.copyOf(ids, j);
        }
        return cx.newArray(scope, ids);
    }

    private static Object js_fromEntries(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        arg = getCompatibleObject(cx, scope, arg);
        Scriptable obj = cx.newObject(scope);
        ScriptRuntime.loadFromIterable(
                cx,
                scope,
                arg,
                (key, value) -> {
                    if (key instanceof Integer) {
                        obj.put((Integer) key, obj, value);
                    } else if (key instanceof Symbol && obj instanceof SymbolScriptable) {
                        ((SymbolScriptable) obj).put((Symbol) key, obj, value);
                    } else {
                        obj.put(ScriptRuntime.toString(key), obj, value);
                    }
                });
        return obj;
    }

    private static Object js_values(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        Scriptable obj = getCompatibleObject(cx, scope, arg);
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
        return cx.newArray(scope, ids);
    }

    private static Object js_hasOwn(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        Object propertyName = args.length < 2 ? Undefined.instance : args[1];
        return AbstractEcmaObjectOperations.hasOwnProperty(cx, arg, propertyName);
    }

    private static Object js_getOwnPropertyNames(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        Scriptable s = getCompatibleObject(cx, scope, arg);
        ScriptableObject obj = ensureScriptableObject(s);
        Object[] ids = obj.getIds(true, false);
        for (int i = 0; i < ids.length; i++) {
            ids[i] = ScriptRuntime.toString(ids[i]);
        }
        return cx.newArray(scope, ids);
    }

    private static Object js_getOwnPropertySymbols(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        Scriptable s = getCompatibleObject(cx, scope, arg);
        ScriptableObject obj = ensureScriptableObject(s);
        Object[] ids = obj.getIds(true, true);
        ArrayList<Object> syms = new ArrayList<>();
        for (Object o : ids) {
            if (o instanceof Symbol) {
                syms.add(o);
            }
        }
        return cx.newArray(scope, syms.toArray());
    }

    private static Object js_getOwnPropDesc(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        // TODO(norris): There's a deeper issue here if
        // arg instanceof Scriptable. Should we create a new
        // interface to admit the new ECMAScript 5 operations?
        Scriptable s = getCompatibleObject(cx, scope, arg);
        ScriptableObject obj = ensureScriptableObject(s);
        Object nameArg = args.length < 2 ? Undefined.instance : args[1];
        Scriptable desc = obj.getOwnPropertyDescriptor(cx, nameArg);
        return desc == null ? Undefined.instance : desc;
    }

    private static Object js_getOwnPropDescs(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        Scriptable s = getCompatibleObject(cx, scope, arg);
        ScriptableObject obj = ensureScriptableObject(s);

        ScriptableObject descs = (ScriptableObject) cx.newObject(scope);
        for (Object key : obj.getIds(true, true)) {
            Scriptable desc = obj.getOwnPropertyDescriptor(cx, key);
            if (desc == null) {
                continue;
            } else if (key instanceof Symbol) {
                descs.put((Symbol) key, descs, desc);
            } else if (key instanceof Integer) {
                descs.put((Integer) key, descs, desc);
            } else {
                descs.put(ScriptRuntime.toString(key), descs, desc);
            }
        }
        return descs;
    }

    private static Object js_defineProperty(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        ScriptableObject obj = ensureScriptableObject(arg);
        Object name = args.length < 2 ? Undefined.instance : args[1];
        Object descArg = args.length < 3 ? Undefined.instance : args[2];
        ScriptableObject desc = ensureScriptableObject(descArg);
        obj.defineOwnProperty(cx, name, desc);
        return obj;
    }

    private static Object js_isExtensible(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        if (cx.getLanguageVersion() >= Context.VERSION_ES6 && !(arg instanceof ScriptableObject)) {
            return Boolean.FALSE;
        }

        ScriptableObject obj = ensureScriptableObject(arg);
        return Boolean.valueOf(obj.isExtensible());
    }

    private static Object js_preventExtensions(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
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
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        ScriptableObject obj = ensureScriptableObject(arg);
        Object propsObj = args.length < 2 ? Undefined.instance : args[1];
        Scriptable props = Context.toObject(propsObj, scope);
        obj.defineOwnProperties(cx, ensureScriptableObject(props));
        return obj;
    }

    private static Object js_create(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        Scriptable obj = (arg == null) ? null : ensureScriptable(arg);

        ScriptableObject newObject = new NativeObject();
        newObject.setParentScope(scope);
        newObject.setPrototype(obj);

        if (args.length > 1 && !Undefined.isUndefined(args[1])) {
            Scriptable props = Context.toObject(args[1], scope);
            newObject.defineOwnProperties(cx, ensureScriptableObject(props));
        }

        return newObject;
    }

    private static Object js_isSealed(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        if (cx.getLanguageVersion() >= Context.VERSION_ES6 && !(arg instanceof ScriptableObject)) {
            return Boolean.TRUE;
        }

        return AbstractEcmaObjectOperations.testIntegrityLevel(
                cx, arg, AbstractEcmaObjectOperations.INTEGRITY_LEVEL.SEALED);
    }

    private static Object js_isFrozen(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object arg = args.length < 1 ? Undefined.instance : args[0];
        if (cx.getLanguageVersion() >= Context.VERSION_ES6 && !(arg instanceof ScriptableObject)) {
            return Boolean.TRUE;
        }

        return AbstractEcmaObjectOperations.testIntegrityLevel(
                cx, arg, AbstractEcmaObjectOperations.INTEGRITY_LEVEL.FROZEN);
    }

    private static Object js_seal(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
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
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
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
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Scriptable targetObj;
        if (args.length > 0) {
            targetObj = ScriptRuntime.toObject(cx, scope, args[0]);
        } else {
            targetObj = ScriptRuntime.toObject(cx, scope, Undefined.instance);
        }
        for (int i = 1; i < args.length; i++) {
            if ((args[i] == null) || Undefined.isUndefined(args[i])) {
                continue;
            }
            Scriptable sourceObj = ScriptRuntime.toObject(cx, scope, args[i]);
            Object[] ids = sourceObj.getIds();
            for (Object key : ids) {
                if (key instanceof Integer) {
                    int intId = (Integer) key;
                    if (sourceObj.has(intId, sourceObj) && isEnumerable(intId, sourceObj)) {
                        Object val = sourceObj.get(intId, sourceObj);
                        AbstractEcmaObjectOperations.put(cx, targetObj, intId, val, true);
                    }
                } else {
                    String stringId = ScriptRuntime.toString(key);
                    if (sourceObj.has(stringId, sourceObj) && isEnumerable(stringId, sourceObj)) {
                        Object val = sourceObj.get(stringId, sourceObj);
                        AbstractEcmaObjectOperations.put(cx, targetObj, stringId, val, true);
                    }
                }
            }
        }
        return targetObj;
    }

    private static Object js_is(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object a1 = args.length < 1 ? Undefined.instance : args[0];
        Object a2 = args.length < 2 ? Undefined.instance : args[1];
        return ScriptRuntime.wrapBoolean(ScriptRuntime.same(a1, a2));
    }

    private static Object js_groupBy(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object items = args.length < 1 ? Undefined.instance : args[0];
        Object callback = args.length < 2 ? Undefined.instance : args[1];

        Map<Object, List<Object>> groups =
                AbstractEcmaObjectOperations.groupBy(
                        cx,
                        scope,
                        OBJECT_TAG,
                        "groupBy",
                        items,
                        callback,
                        AbstractEcmaObjectOperations.KEY_COERCION.PROPERTY);

        NativeObject obj = (NativeObject) cx.newObject(scope);
        obj.setPrototype(null);

        for (Map.Entry<Object, List<Object>> entry : groups.entrySet()) {
            Scriptable elements = cx.newArray(scope, entry.getValue().toArray());

            ScriptableObject desc = (ScriptableObject) cx.newObject(scope);
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

    private static final int ConstructorId_getPrototypeOf = -1,
            ConstructorId_keys = -2,
            ConstructorId_getOwnPropertyNames = -3,
            ConstructorId_getOwnPropertyDescriptor = -4,
            ConstructorId_getOwnPropertyDescriptors = -5,
            ConstructorId_defineProperty = -6,
            ConstructorId_isExtensible = -7,
            ConstructorId_preventExtensions = -8,
            ConstructorId_defineProperties = -9,
            ConstructorId_create = -10,
            ConstructorId_isSealed = -11,
            ConstructorId_isFrozen = -12,
            ConstructorId_seal = -13,
            ConstructorId_freeze = -14,
            ConstructorId_getOwnPropertySymbols = -15,
            ConstructorId_assign = -16,
            ConstructorId_is = -17,

            // ES6
            ConstructorId_setPrototypeOf = -18,
            ConstructorId_entries = -19,
            ConstructorId_fromEntries = -20,
            ConstructorId_values = -21,
            ConstructorId_hasOwn = -22,
            ConstructorId_groupBy = -23,
            Id_constructor = 1,
            Id_toString = 2,
            Id_toLocaleString = 3,
            Id_valueOf = 4,
            Id_hasOwnProperty = 5,
            Id_propertyIsEnumerable = 6,
            Id_isPrototypeOf = 7,
            Id_toSource = 8,
            Id___defineGetter__ = 9,
            Id___defineSetter__ = 10,
            Id___lookupGetter__ = 11,
            Id___lookupSetter__ = 12,
            MAX_PROTOTYPE_ID = 12;
}
