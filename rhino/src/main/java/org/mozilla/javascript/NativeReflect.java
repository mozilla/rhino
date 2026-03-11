/* -*- Mode: java; tab-width: 4; indent-tabs-mode: 1; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static org.mozilla.javascript.ClassDescriptor.Builder.value;
import static org.mozilla.javascript.ClassDescriptor.Destination.CTOR;

import java.util.ArrayList;
import java.util.List;
import org.mozilla.javascript.ScriptRuntime.StringIdOrIndex;

/**
 * This class implements the Reflect object.
 *
 * @author Ronald Brill
 */
final class NativeReflect extends ScriptableObject {
    private static final long serialVersionUID = 2920773905356325445L;

    private static final String REFLECT_TAG = "Reflect";

    private static final ClassDescriptor DESCRIPTOR;

    static {
        DESCRIPTOR =
                new ClassDescriptor.Builder(REFLECT_TAG)
                        .withMethod(CTOR, "apply", 3, NativeReflect::apply)
                        .withMethod(CTOR, "construct", 2, NativeReflect::construct)
                        .withMethod(CTOR, "defineProperty", 3, NativeReflect::defineProperty)
                        .withMethod(CTOR, "deleteProperty", 2, NativeReflect::deleteProperty)
                        .withMethod(CTOR, "get", 2, NativeReflect::get)
                        .withMethod(
                                CTOR,
                                "getOwnPropertyDescriptor",
                                2,
                                NativeReflect::getOwnPropertyDescriptor)
                        .withMethod(CTOR, "getPrototypeOf", 1, NativeReflect::getPrototypeOf)
                        .withMethod(CTOR, "has", 2, NativeReflect::has)
                        .withMethod(CTOR, "isExtensible", 1, NativeReflect::isExtensible)
                        .withMethod(CTOR, "ownKeys", 1, NativeReflect::ownKeys)
                        .withMethod(CTOR, "preventExtensions", 1, NativeReflect::preventExtensions)
                        .withMethod(CTOR, "set", 3, NativeReflect::set)
                        .withMethod(CTOR, "setPrototypeOf", 2, NativeReflect::setPrototypeOf)
                        .withProp(
                                CTOR,
                                SymbolKey.TO_STRING_TAG,
                                value(REFLECT_TAG, DONTENUM | READONLY))
                        .build();
    }

    public static Object init(Context cx, VarScope scope, boolean sealed) {
        return DESCRIPTOR.populateGlobal(cx, scope, new NativeObject(), sealed);
    }

    private NativeReflect() {}

    @Override
    public String getClassName() {
        return "Reflect";
    }

    private static Object apply(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        if (args.length < 3) {
            throw ScriptRuntime.typeErrorById(
                    "msg.method.missing.parameter",
                    "Reflect.apply",
                    "3",
                    Integer.toString(args.length));
        }

        Scriptable callable = ScriptableObject.ensureScriptable(args[0]);

        if (args[1] instanceof Scriptable) {
            thisObj = (Scriptable) args[1];
        } else if (ScriptRuntime.isPrimitive(args[1])) {
            thisObj = cx.newObject(s, "Object", new Object[] {args[1]});
        }

        if (ScriptRuntime.isSymbol(args[2])) {
            throw ScriptRuntime.typeErrorById("msg.arg.not.object", ScriptRuntime.typeof(args[2]));
        }
        ScriptableObject argumentsList = ScriptableObject.ensureScriptableObject(args[2]);

        return ScriptRuntime.applyOrCall(
                true, cx, s, callable, new Object[] {thisObj, argumentsList});
    }

    /**
     * see <a href="https://262.ecma-international.org/12.0/#sec-reflect.construct">28.1.2
     * Reflect.construct (target, argumentsList[, newTarget])</a>
     */
    private static Scriptable construct(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        /*
         * 1. If IsConstructor(target) is false, throw a TypeError exception.
         * 2. If newTarget is not present, set newTarget to target.
         * 3. Else if IsConstructor(newTarget) is false, throw a TypeError exception.
         * 4. Let args be ? CreateListFromArrayLike(argumentsList).
         * 5. Return ? Construct(target, args, newTarget).
         */
        if (args.length < 1) {
            throw ScriptRuntime.typeErrorById(
                    "msg.method.missing.parameter",
                    "Reflect.construct",
                    "3",
                    Integer.toString(args.length));
        }

        if (!AbstractEcmaObjectOperations.isConstructor(cx, args[0])) {
            throw ScriptRuntime.typeErrorById("msg.not.ctor", ScriptRuntime.typeof(args[0]));
        }

        Constructable ctor = (Constructable) args[0];
        if (args.length < 2) {
            return ctor.construct(cx, s, ScriptRuntime.emptyArgs);
        }

        if (args.length > 2 && !AbstractEcmaObjectOperations.isConstructor(cx, args[2])) {
            throw ScriptRuntime.typeErrorById("msg.not.ctor", ScriptRuntime.typeof(args[2]));
        }

        Object[] callArgs = ScriptRuntime.getApplyArguments(cx, args[1]);

        Object newTargetPrototype = null;
        if (args.length > 2) {
            Scriptable newTarget = ScriptableObject.ensureScriptable(args[2]);

            if (newTarget instanceof BaseFunction) {
                newTargetPrototype = ((BaseFunction) newTarget).getPrototypeProperty();
            } else {
                newTargetPrototype = newTarget.get("prototype", newTarget);
            }

            if (!(newTargetPrototype instanceof Scriptable)
                    || ScriptRuntime.isSymbol(newTargetPrototype)
                    || Undefined.isUndefined(newTargetPrototype)) {
                newTargetPrototype = null;
            }
        }

        // our Constructable interface does not support the newTarget;
        // therefore we use a cloned implementation that fixes
        // the prototype before executing call(..).
        if (ctor instanceof BaseFunction && newTargetPrototype != null) {
            BaseFunction ctorBaseFunction = (BaseFunction) ctor;
            Scriptable result = ctorBaseFunction.createObject(cx, s);
            if (result != null) {
                result.setPrototype((Scriptable) newTargetPrototype);

                Object val = ctorBaseFunction.call(cx, s, result, callArgs);
                if (val instanceof Scriptable) {
                    return (Scriptable) val;
                }

                return result;
            }
        }

        Scriptable newScriptable = ctor.construct(cx, s, callArgs);
        if (newTargetPrototype != null) {
            newScriptable.setPrototype((Scriptable) newTargetPrototype);
        }

        return newScriptable;
    }

    private static Object defineProperty(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        if (args.length < 3) {
            throw ScriptRuntime.typeErrorById(
                    "msg.method.missing.parameter",
                    "Reflect.defineProperty",
                    "3",
                    Integer.toString(args.length));
        }

        ScriptableObject target = checkTarget(args);
        DescriptorInfo desc = new DescriptorInfo(ScriptableObject.ensureScriptableObject(args[2]));

        Object key = args[1];

        try {
            if (key instanceof Symbol) {
                return target.defineOwnProperty(cx, key, desc);
            } else {
                String propertyKey =
                        ScriptRuntime.toString(
                                ScriptRuntime.toPrimitive(key, ScriptRuntime.StringClass));
                return target.defineOwnProperty(cx, propertyKey, desc);
            }

        } catch (EcmaError e) {
            return false;
        }
    }

    private static Object deleteProperty(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        ScriptableObject target = checkTarget(args);

        if (args.length > 1) {
            if (ScriptRuntime.isSymbol(args[1])) {
                return ScriptableObject.deleteProperty(target, (Symbol) args[1]);
            }
            return ScriptableObject.deleteProperty(target, ScriptRuntime.toString(args[1]));
        }

        return false;
    }

    private static Object get(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        ScriptableObject target = checkTarget(args);

        if (args.length > 1) {
            if (ScriptRuntime.isSymbol(args[1])) {
                Object prop = ScriptableObject.getProperty(target, (Symbol) args[1]);
                return prop == Scriptable.NOT_FOUND ? Undefined.SCRIPTABLE_UNDEFINED : prop;
            }
            if (args[1] instanceof Number) {
                Object prop = ScriptableObject.getProperty(target, ScriptRuntime.toIndex(args[1]));
                return prop == Scriptable.NOT_FOUND ? Undefined.SCRIPTABLE_UNDEFINED : prop;
            }

            Object prop = ScriptableObject.getProperty(target, ScriptRuntime.toString(args[1]));
            return prop == Scriptable.NOT_FOUND ? Undefined.SCRIPTABLE_UNDEFINED : prop;
        }
        return Undefined.SCRIPTABLE_UNDEFINED;
    }

    private static Scriptable getOwnPropertyDescriptor(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        ScriptableObject target = checkTarget(args);

        if (args.length > 1) {
            if (ScriptRuntime.isSymbol(args[1])) {
                var desc = target.getOwnPropertyDescriptor(cx, args[1]);
                return desc == null ? Undefined.SCRIPTABLE_UNDEFINED : desc.toObject(s);
            }

            var desc = target.getOwnPropertyDescriptor(cx, ScriptRuntime.toString(args[1]));
            return desc == null ? Undefined.SCRIPTABLE_UNDEFINED : desc.toObject(s);
        }
        return Undefined.SCRIPTABLE_UNDEFINED;
    }

    private static Scriptable getPrototypeOf(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        ScriptableObject target = checkTarget(args);

        return target.getPrototype();
    }

    private static Object has(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        ScriptableObject target = checkTarget(args);

        if (args.length > 1) {
            if (ScriptRuntime.isSymbol(args[1])) {
                return ScriptableObject.hasProperty(target, (Symbol) args[1]);
            }

            return ScriptableObject.hasProperty(target, ScriptRuntime.toString(args[1]));
        }
        return false;
    }

    private static Object isExtensible(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        ScriptableObject target = checkTarget(args);
        return target.isExtensible();
    }

    private static Scriptable ownKeys(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        ScriptableObject target = checkTarget(args);

        final List<Object> strings = new ArrayList<>();
        final List<Object> symbols = new ArrayList<>();

        Object[] ids;
        try (var map = target.startCompoundOp(false)) {
            ids = target.getIds(map, true, true);
        }
        for (Object o : ids) {
            if (o instanceof Symbol) {
                symbols.add(o);
            } else {
                strings.add(ScriptRuntime.toString(o));
            }
        }

        Object[] keys = new Object[strings.size() + symbols.size()];
        System.arraycopy(strings.toArray(), 0, keys, 0, strings.size());
        System.arraycopy(symbols.toArray(), 0, keys, strings.size(), symbols.size());

        return cx.newArray(s, keys);
    }

    private static Object preventExtensions(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        ScriptableObject target = checkTarget(args);

        return target.preventExtensions();
    }

    private static Object set(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        ScriptableObject target = checkTarget(args);
        if (args.length < 2) {
            return true;
        }

        ScriptableObject receiver =
                args.length > 3 ? ScriptableObject.ensureScriptableObject(args[3]) : target;
        if (receiver != target) {
            DescriptorInfo descriptor = target.getOwnPropertyDescriptor(cx, args[1]);
            if (descriptor != null) {
                Object setter = descriptor.setter;
                if (setter != null && setter != NOT_FOUND) {
                    ((Function) setter).call(cx, s, receiver, new Object[] {args[2]});
                    return true;
                }

                if (descriptor.isConfigurable(false)) {
                    return false;
                }
            }
        }

        if (ScriptRuntime.isSymbol(args[1])) {
            receiver.put((Symbol) args[1], receiver, args[2]);
        } else {
            StringIdOrIndex soi = ScriptRuntime.toStringIdOrIndex(args[1]);
            if (soi.stringId == null) {
                receiver.put(soi.index, receiver, args[2]);
            } else {
                receiver.put(soi.stringId, receiver, args[2]);
            }
        }

        return true;
    }

    private static Object setPrototypeOf(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        if (args.length < 2) {
            throw ScriptRuntime.typeErrorById(
                    "msg.method.missing.parameter",
                    "Reflect.js_setPrototypeOf",
                    "2",
                    Integer.toString(args.length));
        }

        ScriptableObject target = checkTarget(args);

        if (target.getPrototype() == args[1]) {
            return true;
        }

        if (!target.isExtensible()) {
            return false;
        }

        if (args[1] == null) {
            target.setPrototype(null);
            return true;
        }

        if (ScriptRuntime.isSymbol(args[1])) {
            throw ScriptRuntime.typeErrorById("msg.arg.not.object", ScriptRuntime.typeof(args[0]));
        }

        ScriptableObject proto = ScriptableObject.ensureScriptableObject(args[1]);
        if (target.getPrototype() == proto) {
            return true;
        }

        // avoid cycles
        Scriptable p = proto;
        while (p != null) {
            if (target == p) {
                return false;
            }
            p = p.getPrototype();
        }

        target.setPrototype(proto);
        return true;
    }

    private static ScriptableObject checkTarget(Object[] args) {
        if (args.length == 0 || args[0] == null || args[0] == Undefined.instance) {
            Object argument = args.length == 0 ? Undefined.instance : args[0];
            throw ScriptRuntime.typeErrorById(
                    "msg.no.properties", ScriptRuntime.toString(argument));
        }

        if (ScriptRuntime.isSymbol(args[0])) {
            throw ScriptRuntime.typeErrorById("msg.arg.not.object", ScriptRuntime.typeof(args[0]));
        }
        return ScriptableObject.ensureScriptableObject(args[0]);
    }
}
