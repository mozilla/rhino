/* -*- Mode: java; tab-width: 4; indent-tabs-mode: 1; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static org.mozilla.javascript.ClassDescriptor.Builder.value;
import static org.mozilla.javascript.ClassDescriptor.Destination.CTOR;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import org.mozilla.javascript.ScriptRuntime.StringIdOrIndex;

/**
 * This class implements the Reflect object.
 *
 * @author Ronald Brill
 */
final class NativeReflect extends ScriptableObject {
    @Serial private static final long serialVersionUID = 2920773905356325445L;

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

    /**
     * see <a href="https://262.ecma-international.org/12.0/#sec-reflect.apply">28.1.1 Reflect.apply
     * ( target, thisArgument, argumentsList )</a>
     */
    private static Object apply(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        /*
         * 1. If IsCallable(target) is false, throw a TypeError exception.
         * 2. Let args be ? CreateListFromArrayLike(argumentsList).
         * 3. Perform PrepareForTailCall().
         * 4. Return ? Call(target, thisArgument, args).
         */
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
        Scriptable argumentsList = ScriptableObject.ensureScriptable(args[2]);

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
        if (newTargetPrototype != null && ctor instanceof BaseFunction) {
            BaseFunction ctorBaseFunction = (BaseFunction) ctor;
            Scriptable result = ctorBaseFunction.createObject(cx, s);
            if (result != null) {
                if (ctorBaseFunction.isConstructor()
                        && !(newTargetPrototype instanceof NativeArray)) {
                    Scriptable newScriptable = ctorBaseFunction.construct(cx, s, callArgs);

                    if (newScriptable instanceof NativeProxy) {
                        newScriptable.setPrototype(ctorBaseFunction.getClassPrototype());
                    } else {
                        newScriptable.setPrototype((Scriptable) newTargetPrototype);
                    }
                    return newScriptable;
                }

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

    /**
     * see <a href="https://262.ecma-international.org/12.0/#sec-reflect.defineproperty">28.1.3
     * Reflect.defineProperty ( target, propertyKey, attributes )</a>
     */
    private static Object defineProperty(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        /*
         * 1. If Type(target) is not Object, throw a TypeError exception.
         * 2. Let key be ? ToPropertyKey(propertyKey).
         * 3. Let desc be ? ToPropertyDescriptor(attributes).
         * 4. Return ? target.[[DefineOwnProperty]](key, desc).
         */
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

    /**
     * see <a href="https://262.ecma-international.org/12.0/#sec-reflect.deleteproperty">28.1.4
     * Reflect.deleteProperty ( target, propertyKey )</a>
     */
    private static Object deleteProperty(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        /*
         * 1. If Type(target) is not Object, throw a TypeError exception.
         * 2. Let key be ? ToPropertyKey(propertyKey).
         * 3. Return ? target.[[Delete]](key).
         */
        ScriptableObject target = checkTarget(args);

        if (args.length > 1) {
            if (ScriptRuntime.isSymbol(args[1])) {
                return ScriptableObject.deleteProperty(target, (Symbol) args[1]);
            }
            return ScriptableObject.deleteProperty(target, ScriptRuntime.toString(args[1]));
        }

        return false;
    }

    /**
     * see <a href="https://262.ecma-international.org/12.0/#sec-reflect.get">28.1.5 Reflect.get (
     * target, propertyKey [ , receiver ] )</a>
     */
    private static Object get(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        /*
         * 1. If Type(target) is not Object, throw a TypeError exception.
         * 2. Let key be ? ToPropertyKey(propertyKey).
         * 3. If receiver is not present, then
         *    a. Set receiver to target.
         * 4. Return ? target.[[Get]](key, receiver).
         */
        ScriptableObject target = checkTarget(args);

        if (args.length < 2) {
            return Undefined.SCRIPTABLE_UNDEFINED;
        }

        Scriptable receiver =
                (args.length > 2 && args[2] instanceof Scriptable) ? (Scriptable) args[2] : target;

        Object prop;
        if (ScriptRuntime.isSymbol(args[1])) {
            prop = ScriptableObject.getSuperProperty(target, receiver, (Symbol) args[1]);
        } else if (args[1] instanceof Number) {
            prop =
                    ScriptableObject.getSuperProperty(
                            target, receiver, ScriptRuntime.toIndex(args[1]));
        } else {
            prop =
                    ScriptableObject.getSuperProperty(
                            target, receiver, ScriptRuntime.toString(args[1]));
        }
        return prop == Scriptable.NOT_FOUND ? Undefined.SCRIPTABLE_UNDEFINED : prop;
    }

    /**
     * see <a
     * href="https://262.ecma-international.org/12.0/#sec-reflect.getownpropertydescriptor">28.1.6
     * Reflect.getOwnPropertyDescriptor ( target, propertyKey )</a>
     */
    private static Scriptable getOwnPropertyDescriptor(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        /*
         * 1. If Type(target) is not Object, throw a TypeError exception.
         * 2. Let key be ? ToPropertyKey(propertyKey).
         * 3. Let desc be ? target.[[GetOwnProperty]](key).
         * 4. Return FromPropertyDescriptor(desc).
         */
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

    /**
     * see <a href="https://262.ecma-international.org/12.0/#sec-reflect.getprototypeof">28.1.7
     * Reflect.getPrototypeOf ( target )</a>
     */
    private static Scriptable getPrototypeOf(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        /*
         * 1. If Type(target) is not Object, throw a TypeError exception.
         * 2. Return ? target.[[GetPrototypeOf]]().
         */
        ScriptableObject target = checkTarget(args);

        return target.getPrototype();
    }

    /**
     * see <a href="https://262.ecma-international.org/12.0/#sec-reflect.has">28.1.8 Reflect.has (
     * target, propertyKey )</a>
     */
    private static Object has(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        /*
         * 1. If Type(target) is not Object, throw a TypeError exception.
         * 2. Let key be ? ToPropertyKey(propertyKey).
         * 3. Return ? target.[[HasProperty]](key).
         */
        ScriptableObject target = checkTarget(args);

        if (args.length > 1) {
            if (ScriptRuntime.isSymbol(args[1])) {
                return ScriptableObject.hasProperty(target, (Symbol) args[1]);
            }

            return ScriptableObject.hasProperty(target, ScriptRuntime.toString(args[1]));
        }
        return false;
    }

    /**
     * see <a href="https://262.ecma-international.org/12.0/#sec-reflect.isextensible">28.1.9
     * Reflect.isExtensible ( target )</a>
     */
    private static Object isExtensible(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        /*
         * 1. If Type(target) is not Object, throw a TypeError exception.
         * 2. Return ? IsExtensible(target).
         */
        ScriptableObject target = checkTarget(args);
        return target.isExtensible();
    }

    /**
     * see <a href="https://262.ecma-international.org/12.0/#sec-reflect.ownkeys">28.1.10
     * Reflect.ownKeys ( target )</a>
     */
    private static Scriptable ownKeys(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        /*
         * 1. If Type(target) is not Object, throw a TypeError exception.
         * 2. Let keys be ? target.[[OwnPropertyKeys]]().
         * 3. Return CreateArrayFromList(keys).
         */
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

    /**
     * see <a href="https://262.ecma-international.org/12.0/#sec-reflect.preventextensions">28.1.11
     * Reflect.preventExtensions ( target )</a>
     */
    private static Object preventExtensions(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        /*
         * 1. If Type(target) is not Object, throw a TypeError exception.
         * 2. Return ? target.[[PreventExtensions]]().
         */
        ScriptableObject target = checkTarget(args);

        return target.preventExtensions();
    }

    /**
     * see <a href="https://262.ecma-international.org/12.0/#sec-reflect.set">28.1.12 Reflect.set (
     * target, propertyKey, V [ , receiver ] )</a>
     */
    private static Object set(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        /*
         * 1. If Type(target) is not Object, throw a TypeError exception.
         * 2. Let key be ? ToPropertyKey(propertyKey).
         * 3. If receiver is not present, then
         *    a. Set receiver to target.
         * 4. Return ? target.[[Set]](key, V, receiver).
         */
        ScriptableObject target = checkTarget(args);
        if (args.length < 2) {
            return true;
        }

        // A missing value must be treated as undefined
        final Object value = args.length > 2 ? args[2] : Undefined.instance;

        Scriptable receiver = args.length > 3 ? ScriptableObject.ensureScriptable(args[3]) : target;

        // OrdinarySet is only needed when receiver != target.
        // When receiver == target we must call target.[[Set]] directly via put(),
        // which correctly invokes any proxy set trap on target.
        // Inlining OrdinarySet when receiver == target would
        // bypass the proxy trap and apply the plain-object non-writable check
        // incorrectly (e.g. a configurable+non-writable property where the trap
        // returns true would wrongly return false).
        if (receiver != target) {
            if (target instanceof NativeProxy) {
                if (ScriptRuntime.isSymbol(args[1])) {
                    target.put((Symbol) args[1], receiver, value);
                } else {
                    StringIdOrIndex soi = ScriptRuntime.toStringIdOrIndex(args[1]);
                    if (soi.stringId == null) {
                        target.put(soi.index, receiver, value);
                    } else {
                        target.put(soi.stringId, receiver, value);
                    }
                }
                return true;
            }

            AbstractEcmaObjectOperations.ordinarySet(cx, s, target, args[1], args[2], receiver);
            return true;
        }

        // receiver == target (or target has no own property P):
        // delegate to [[Set]] on the receiver, which correctly fires any proxy
        // set trap when receiver/target is a Proxy.
        if (ScriptRuntime.isSymbol(args[1])) {
            target.put((Symbol) args[1], receiver, value);
        } else {
            StringIdOrIndex soi = ScriptRuntime.toStringIdOrIndex(args[1]);
            if (soi.stringId == null) {
                target.put(soi.index, receiver, value);
            } else {
                target.put(soi.stringId, receiver, value);
            }
        }

        return true;
    }

    /**
     * see <a href="https://262.ecma-international.org/12.0/#sec-reflect.setprototypeof">28.1.13
     * Reflect.setPrototypeOf ( target, proto )</a>
     */
    private static Object setPrototypeOf(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        /*
         * 1. If Type(target) is not Object, throw a TypeError exception.
         * 2. If Type(proto) is not Object and proto is not null, throw a TypeError exception.
         * 3. Return ? target.[[SetPrototypeOf]](proto).
         */
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

        Scriptable proto = ScriptableObject.ensureScriptable(args[1]);
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
