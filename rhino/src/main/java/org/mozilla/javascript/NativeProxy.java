/* -*- Mode: java; tab-width: 4; indent-tabs-mode: 1; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * This class implements the Proxy object.
 *
 * @author Ronald Brill
 */
final class NativeProxy extends ScriptableObject implements Callable, Constructable {
    private static final long serialVersionUID = 6676871870513494844L;

    private static final String PROXY_TAG = "Proxy";

    private static final String TRAP_GET_PROTOTYPE_OF = "getPrototypeOf";
    private static final String TRAP_SET_PROTOTYPE_OF = "setPrototypeOf";
    private static final String TRAP_IS_EXTENSIBLE = "isExtensible";
    private static final String TRAP_PREVENT_EXTENSIONS = "preventExtensions";
    private static final String TRAP_GET_OWN_PROPERTY_DESCRIPTOR = "getOwnPropertyDescriptor";
    private static final String TRAP_DEFINE_PROPERTY = "defineProperty";
    private static final String TRAP_HAS = "has";
    private static final String TRAP_GET = "get";
    private static final String TRAP_SET = "set";
    private static final String TRAP_DELETE_PROPERTY = "deleteProperty";
    private static final String TRAP_OWN_KEYS = "ownKeys";
    private static final String TRAP_APPLY = "apply";
    private static final String TRAP_CONSTRUCT = "construct";

    private ScriptableObject targetObj;
    private Scriptable handlerObj;
    private final String typeOf;

    private static final class Revoker implements SerializableCallable {
        private NativeProxy revocableProxy = null;

        public Revoker(NativeProxy proxy) {
            revocableProxy = proxy;
        }

        @Override
        public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            if (revocableProxy != null) {
                revocableProxy.handlerObj = null;
                revocableProxy.targetObj = null;
                revocableProxy = null;
            }
            return Undefined.instance;
        }
    }

    public static Object init(Context cx, Scriptable scope, boolean sealed) {
        LambdaConstructor constructor =
                new LambdaConstructor(
                        scope,
                        PROXY_TAG,
                        2,
                        LambdaConstructor.CONSTRUCTOR_NEW,
                        NativeProxy::constructor) {

                    @Override
                    public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
                        NativeProxy obj =
                                (NativeProxy) getTargetConstructor().construct(cx, scope, args);
                        // avoid getting trapped
                        obj.setPrototypeDirect(getClassPrototype());
                        obj.setParentScope(scope);
                        return obj;
                    }
                };
        constructor.setPrototypeProperty(null);

        constructor.defineConstructorMethod(
                scope, "revocable", 2, NativeProxy::revocable, DONTENUM, DONTENUM | READONLY);
        if (sealed) {
            constructor.sealObject();
        }
        return constructor;
    }

    private NativeProxy(ScriptableObject target, Scriptable handler) {
        this.targetObj = target;
        this.handlerObj = handler;

        if (target == null || !(target instanceof Callable)) {
            typeOf = super.getTypeOf();
        } else {
            typeOf = target.getTypeOf();
        }
    }

    @Override
    public String getClassName() {
        ScriptableObject target = getTargetThrowIfRevoked();
        return target.getClassName();
    }

    /**
     * see
     * https://262.ecma-international.org/12.0/#sec-proxy-object-internal-methods-and-internal-slots-construct-argumentslist-newtarget
     */
    @Override
    public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
        /*
         * 1. Let handler be O.[[ProxyHandler]].
         * 2. If handler is null, throw a TypeError exception.
         * 3. Assert: Type(handler) is Object.
         * 4. Let target be O.[[ProxyTarget]].
         * 5. Assert: IsConstructor(target) is true.
         * 6. Let trap be ? GetMethod(handler, "construct").
         * 7. If trap is undefined, then
         *     a. Return ? Construct(target, argumentsList, newTarget).
         * 8. Let argArray be ! CreateArrayFromList(argumentsList).
         * 9. Let newObj be ? Call(trap, handler, « target, argArray, newTarget »).
         * 10. If Type(newObj) is not Object, throw a TypeError exception.
         * 11. Return newObj.
         */
        ScriptableObject target = getTargetThrowIfRevoked();

        Callable trap = getTrap(TRAP_CONSTRUCT);
        if (trap != null) {
            Object result = callTrap(trap, new Object[] {target, args, this});
            if (!(result instanceof Scriptable) || ScriptRuntime.isSymbol(result)) {
                throw ScriptRuntime.typeError("Constructor trap has to return a scriptable.");
            }
            return (ScriptableObject) result;
        }

        return ((Constructable) target).construct(cx, scope, args);
    }

    /**
     * https://262.ecma-international.org/12.0/#sec-proxy-object-internal-methods-and-internal-slots-hasproperty-p
     */
    @Override
    public boolean has(String name, Scriptable start) {
        /*
         * 1. Assert: IsPropertyKey(P) is true.
         * 2. Let handler be O.[[ProxyHandler]].
         * 3. If handler is null, throw a TypeError exception.
         * 4. Assert: Type(handler) is Object.
         * 5. Let target be O.[[ProxyTarget]].
         * 6. Let trap be ? GetMethod(handler, "has").
         * 7. If trap is undefined, then
         *     a. Return ? target.[[HasProperty]](P).
         * 8. Let booleanTrapResult be ! ToBoolean(? Call(trap, handler, « target, P »)).
         * 9. If booleanTrapResult is false, then
         *      a. Let targetDesc be ? target.[[GetOwnProperty]](P).
         *     b. If targetDesc is not undefined, then
         *         i. If targetDesc.[[Configurable]] is false, throw a TypeError exception.
         *         ii. Let extensibleTarget be ? IsExtensible(target).
         *         iii. If extensibleTarget is false, throw a TypeError exception.
         * 10. Return booleanTrapResult.
         */
        ScriptableObject target = getTargetThrowIfRevoked();

        Callable trap = getTrap(TRAP_HAS);
        if (trap != null) {

            boolean booleanTrapResult =
                    ScriptRuntime.toBoolean(callTrap(trap, new Object[] {target, name}));
            if (!booleanTrapResult) {
                ScriptableObject targetDesc =
                        target.getOwnPropertyDescriptor(Context.getContext(), name);
                if (targetDesc != null) {
                    if (Boolean.FALSE.equals(targetDesc.get("configurable"))
                            || !target.isExtensible()) {
                        throw ScriptRuntime.typeError(
                                "proxy can't report an existing own property '"
                                        + name
                                        + "' as non-existent on a non-extensible object");
                    }
                }
            }
            return booleanTrapResult;
        }

        if (start == this) {
            start = target;
        }
        return target.has(name, start);
    }

    /**
     * see
     * https://262.ecma-international.org/12.0/#sec-proxy-object-internal-methods-and-internal-slots-hasproperty-p
     */
    @Override
    public boolean has(int index, Scriptable start) {
        /*
         * 1. Assert: IsPropertyKey(P) is true.
         * 2. Let handler be O.[[ProxyHandler]].
         * 3. If handler is null, throw a TypeError exception.
         * 4. Assert: Type(handler) is Object.
         * 5. Let target be O.[[ProxyTarget]].
         * 6. Let trap be ? GetMethod(handler, "has").
         * 7. If trap is undefined, then
         *     a. Return ? target.[[HasProperty]](P).
         * 8. Let booleanTrapResult be ! ToBoolean(? Call(trap, handler, « target, P »)).
         * 9. If booleanTrapResult is false, then
         *     a. Let targetDesc be ? target.[[GetOwnProperty]](P).
         *     b. If targetDesc is not undefined, then
         *         i. If targetDesc.[[Configurable]] is false, throw a TypeError exception.
         *         ii. Let extensibleTarget be ? IsExtensible(target).
         *         iii. If extensibleTarget is false, throw a TypeError exception.
         * 10. Return booleanTrapResult.
         */
        ScriptableObject target = getTargetThrowIfRevoked();

        Callable trap = getTrap(TRAP_HAS);
        if (trap != null) {
            boolean booleanTrapResult =
                    ScriptRuntime.toBoolean(
                            callTrap(trap, new Object[] {target, ScriptRuntime.toString(index)}));
            if (!booleanTrapResult) {
                ScriptableObject targetDesc =
                        target.getOwnPropertyDescriptor(Context.getContext(), index);
                if (targetDesc != null) {
                    if (Boolean.FALSE.equals(targetDesc.get("configurable"))
                            || !target.isExtensible()) {
                        throw ScriptRuntime.typeError(
                                "proxy can't check an existing property ' + name + ' existance on an not configurable or not extensible object");
                    }
                }
            }

            return booleanTrapResult;
        }

        if (start == this) {
            start = target;
        }
        return target.has(index, start);
    }

    /**
     * see
     * https://262.ecma-international.org/12.0/#sec-proxy-object-internal-methods-and-internal-slots-hasproperty-p
     */
    @Override
    public boolean has(Symbol key, Scriptable start) {
        ScriptableObject target = getTargetThrowIfRevoked();

        Callable trap = getTrap(TRAP_HAS);
        if (trap != null) {
            boolean booleanTrapResult =
                    ScriptRuntime.toBoolean(callTrap(trap, new Object[] {target, key}));
            if (!booleanTrapResult) {
                ScriptableObject targetDesc =
                        target.getOwnPropertyDescriptor(Context.getContext(), key);
                if (targetDesc != null) {
                    if (Boolean.FALSE.equals(targetDesc.get("configurable"))
                            || !target.isExtensible()) {
                        throw ScriptRuntime.typeError(
                                "proxy can't check an existing property ' + name + ' existance on an not configurable or not extensible object");
                    }
                }
            }

            return booleanTrapResult;
        }

        if (start == this) {
            start = target;
        }
        SymbolScriptable symbolScriptableTarget = ensureSymbolScriptable(target);
        return symbolScriptableTarget.has(key, start);
    }

    /**
     * see
     * https://262.ecma-international.org/12.0/#sec-proxy-object-internal-methods-and-internal-slots-ownpropertykeys
     */
    @Override
    Object[] getIds(boolean getNonEnumerable, boolean getSymbols) {
        /*
        * 1. Let handler be O.[[ProxyHandler]].
        * 2. If handler is null, throw a TypeError exception.
        * 3. Assert: Type(handler) is Object.
        * 4. Let target be O.[[ProxyTarget]].
        * 5. Let trap be ? GetMethod(handler, "ownKeys").
        * 6. If trap is undefined, then
        *    a. Return ? target.[[OwnPropertyKeys]]().
        * 7. Let trapResultArray be ? Call(trap, handler, « target »).
        * 8. Let trapResult be ? CreateListFromArrayLike(trapResultArray, « String, Symbol »).
        * 9. If trapResult contains any duplicate entries, throw a TypeError exception.
        * 10. Let extensibleTarget be ? IsExtensible(target).
        * 11. Let targetKeys be ? target.[[OwnPropertyKeys]]().
        * 12. Assert: targetKeys is a List whose elements are only String and Symbol values.
        * 13. Assert: targetKeys contains no duplicate entries.
        * 14. Let targetConfigurableKeys be a new empty List.
        * 15. Let targetNonconfigurableKeys be a new empty List.
        * 16. For each element key of targetKeys, do
        *     a. Let desc be ? target.[[GetOwnProperty]](key).
        *     b. If desc is not undefined and desc.[[Configurable]] is false, then
        *         i. i. Append key as an element of targetNonconfigurableKeys.
        *     c. Else,
                  i. i. Append key as an element of targetConfigurableKeys.
        * 17. If extensibleTarget is true and targetNonconfigurableKeys is empty, then
        *     a. Return trapResult.
        * 18. Let uncheckedResultKeys be a List whose elements are the elements of trapResult.
        * 19. For each element key of targetNonconfigurableKeys, do
        *     a. a. If key is not an element of uncheckedResultKeys, throw a TypeError exception.
        *     b. Remove key from uncheckedResultKeys.
        * 20. If extensibleTarget is true, return trapResult.
        * 21. For each element key of targetConfigurableKeys, do
        *     a. a. If key is not an element of uncheckedResultKeys, throw a TypeError exception.
        *     b. Remove key from uncheckedResultKeys.
        * 22. If uncheckedResultKeys is not empty, throw a TypeError exception.
        * 23. Return trapResult.
        */
        ScriptableObject target = getTargetThrowIfRevoked();

        Callable trap = getTrap(TRAP_OWN_KEYS);
        if (trap != null) {
            Object res = callTrap(trap, new Object[] {target});
            if (!(res instanceof Scriptable)) {
                throw ScriptRuntime.typeError("ownKeys trap must be an object");
            }
            if (!ScriptRuntime.isArrayLike((Scriptable) res)) {
                throw ScriptRuntime.typeError("ownKeys trap must be an array like object");
            }

            Context cx = Context.getContext();

            List<Object> trapResult =
                    AbstractEcmaObjectOperations.createListFromArrayLike(
                            cx,
                            (Scriptable) res,
                            (o) ->
                                    o instanceof CharSequence
                                            || o instanceof NativeString
                                            || ScriptRuntime.isSymbol(o),
                            "proxy [[OwnPropertyKeys]] must return an array with only string and symbol elements");

            boolean extensibleTarget = target.isExtensible();
            // don't use the provided values here we have to check all
            Object[] targetKeys = target.getIds(true, true);

            HashSet<Object> uncheckedResultKeys = new HashSet<Object>(trapResult);
            if (uncheckedResultKeys.size() != trapResult.size()) {
                throw ScriptRuntime.typeError("ownKeys trap result must not contain duplicates");
            }

            ArrayList<Object> targetConfigurableKeys = new ArrayList<>();
            ArrayList<Object> targetNonconfigurableKeys = new ArrayList<>();
            for (Object targetKey : targetKeys) {
                ScriptableObject desc = target.getOwnPropertyDescriptor(cx, targetKey);
                if (desc != null && Boolean.FALSE.equals(desc.get("configurable"))) {
                    targetNonconfigurableKeys.add(targetKey);
                } else {
                    targetConfigurableKeys.add(targetKey);
                }
            }

            if (extensibleTarget && targetNonconfigurableKeys.size() == 0) {
                return trapResult.toArray();
            }

            for (Object key : targetNonconfigurableKeys) {
                if (!uncheckedResultKeys.contains(key)) {
                    throw ScriptRuntime.typeError(
                            "proxy can't skip a non-configurable property '" + key + "'");
                }
                uncheckedResultKeys.remove(key);
            }
            if (extensibleTarget) {
                return trapResult.toArray();
            }

            for (Object key : targetConfigurableKeys) {
                if (!uncheckedResultKeys.contains(key)) {
                    throw ScriptRuntime.typeError(
                            "proxy can't skip a configurable property " + key);
                }
                uncheckedResultKeys.remove(key);
            }

            if (uncheckedResultKeys.size() > 0) {
                throw ScriptRuntime.typeError("proxy can't skip properties");
            }

            // target is not extensible, fall back to the target call
        }

        return target.getIds(getNonEnumerable, getSymbols);
    }

    /**
     * see
     * https://262.ecma-international.org/12.0/#sec-proxy-object-internal-methods-and-internal-slots-get-p-receiver
     */
    @Override
    public Object get(String name, Scriptable start) {
        /*
         * 1. Assert: IsPropertyKey(P) is true.
         * 2. Let handler be O.[[ProxyHandler]].
         * 3. If handler is null, throw a TypeError exception.
         * 4. Assert: Type(handler) is Object.
         * 5. Let target be O.[[ProxyTarget]].
         * 6. Let trap be ? GetMethod(handler, "get").
         * 7. If trap is undefined, then
         *     a. Return ? target.[[Get]](P, Receiver).
         * 8. Let trapResult be ? Call(trap, handler, « target, P, Receiver »).
         * 9. Let targetDesc be ? target.[[GetOwnProperty]](P).
         * 10. If targetDesc is not undefined and targetDesc.[[Configurable]] is false, then
         *     a. If IsDataDescriptor(targetDesc) is true and targetDesc.[[Writable]] is false, then
         *         i. If SameValue(trapResult, targetDesc.[[Value]]) is false, throw a TypeError exception.
         *     b. If IsAccessorDescriptor(targetDesc) is true and targetDesc.[[Get]] is undefined, then
         *         i. If trapResult is not undefined, throw a TypeError exception.
         * 11. Return trapResult.
         */
        ScriptableObject target = getTargetThrowIfRevoked();

        Callable trap = getTrap(TRAP_GET);
        if (trap != null) {
            Object trapResult = callTrap(trap, new Object[] {target, name, this});

            ScriptableObject targetDesc =
                    target.getOwnPropertyDescriptor(Context.getContext(), name);
            if (targetDesc != null
                    && !Undefined.isUndefined(targetDesc)
                    && Boolean.FALSE.equals(targetDesc.get("configurable"))) {
                if (ScriptableObject.isDataDescriptor(targetDesc)
                        && Boolean.FALSE.equals(targetDesc.get("writable"))) {
                    if (!Objects.equals(trapResult, targetDesc.get("value"))) {
                        throw ScriptRuntime.typeError(
                                "proxy get has to return the same value as the plain call");
                    }
                }
                if (ScriptableObject.isAccessorDescriptor(targetDesc)
                        && Undefined.isUndefined(targetDesc.get("get"))) {
                    if (!Undefined.isUndefined(trapResult)) {
                        throw ScriptRuntime.typeError(
                                "proxy get has to return the same value as the plain call");
                    }
                }
            }
            return trapResult;
        }

        if (start == this) {
            start = target;
        }
        return target.get(name, start);
    }

    /**
     * see
     * https://262.ecma-international.org/12.0/#sec-proxy-object-internal-methods-and-internal-slots-get-p-receiver
     */
    @Override
    public Object get(int index, Scriptable start) {
        /*
         * 1. Assert: IsPropertyKey(P) is true.
         * 2. Let handler be O.[[ProxyHandler]].
         * 3. If handler is null, throw a TypeError exception.
         * 4. Assert: Type(handler) is Object.
         * 5. Let target be O.[[ProxyTarget]].
         * 6. Let trap be ? GetMethod(handler, "get").
         * 7. If trap is undefined, then
         *     a. Return ? target.[[Get]](P, Receiver).
         * 8. Let trapResult be ? Call(trap, handler, « target, P, Receiver »).
         * 9. Let targetDesc be ? target.[[GetOwnProperty]](P).
         * 10. If targetDesc is not undefined and targetDesc.[[Configurable]] is false, then
         *     a. If IsDataDescriptor(targetDesc) is true and targetDesc.[[Writable]] is false, then
         *         i. If SameValue(trapResult, targetDesc.[[Value]]) is false, throw a TypeError exception.
         *     b. If IsAccessorDescriptor(targetDesc) is true and targetDesc.[[Get]] is undefined, then
         *         i. If trapResult is not undefined, throw a TypeError exception.
         * 11. Return trapResult.
         */
        ScriptableObject target = getTargetThrowIfRevoked();

        Callable trap = getTrap(TRAP_GET);
        if (trap != null) {
            Object trapResult =
                    callTrap(trap, new Object[] {target, ScriptRuntime.toString(index), this});

            ScriptableObject targetDesc =
                    target.getOwnPropertyDescriptor(Context.getContext(), index);
            if (targetDesc != null
                    && !Undefined.isUndefined(targetDesc)
                    && Boolean.FALSE.equals(targetDesc.get("configurable"))) {
                if (ScriptableObject.isDataDescriptor(targetDesc)
                        && Boolean.FALSE.equals(targetDesc.get("writable"))) {
                    if (!Objects.equals(trapResult, targetDesc.get("value"))) {
                        throw ScriptRuntime.typeError(
                                "proxy get has to return the same value as the plain call");
                    }
                }
                if (ScriptableObject.isAccessorDescriptor(targetDesc)
                        && Undefined.isUndefined(targetDesc.get("get"))) {
                    if (!Undefined.isUndefined(trapResult)) {
                        throw ScriptRuntime.typeError(
                                "proxy get has to return the same value as the plain call");
                    }
                }
            }
            return trapResult;
        }

        if (start == this) {
            start = target;
        }
        return target.get(index, start);
    }

    /**
     * see
     * https://262.ecma-international.org/12.0/#sec-proxy-object-internal-methods-and-internal-slots-get-p-receiver
     */
    @Override
    public Object get(Symbol key, Scriptable start) {
        /*
         * 1. Assert: IsPropertyKey(P) is true.
         * 2. Let handler be O.[[ProxyHandler]].
         * 3. If handler is null, throw a TypeError exception.
         * 4. Assert: Type(handler) is Object.
         * 5. Let target be O.[[ProxyTarget]].
         * 6. Let trap be ? GetMethod(handler, "get").
         * 7. If trap is undefined, then
         *     a. Return ? target.[[Get]](P, Receiver).
         * 8. Let trapResult be ? Call(trap, handler, « target, P, Receiver »).
         * 9. Let targetDesc be ? target.[[GetOwnProperty]](P).
         * 10. If targetDesc is not undefined and targetDesc.[[Configurable]] is false, then
         *     a. If IsDataDescriptor(targetDesc) is true and targetDesc.[[Writable]] is false, then
         *         i. If SameValue(trapResult, targetDesc.[[Value]]) is false, throw a TypeError exception.
         *     b. If IsAccessorDescriptor(targetDesc) is true and targetDesc.[[Get]] is undefined, then
         *         i. If trapResult is not undefined, throw a TypeError exception.
         * 11. Return trapResult.
         */
        ScriptableObject target = getTargetThrowIfRevoked();

        Callable trap = getTrap(TRAP_GET);
        if (trap != null) {
            Object trapResult = callTrap(trap, new Object[] {target, key, this});

            ScriptableObject targetDesc =
                    target.getOwnPropertyDescriptor(Context.getContext(), key);
            if (targetDesc != null
                    && !Undefined.isUndefined(targetDesc)
                    && Boolean.FALSE.equals(targetDesc.get("configurable"))) {
                if (ScriptableObject.isDataDescriptor(targetDesc)
                        && Boolean.FALSE.equals(targetDesc.get("writable"))) {
                    if (!Objects.equals(trapResult, targetDesc.get("value"))) {
                        throw ScriptRuntime.typeError(
                                "proxy get has to return the same value as the plain call");
                    }
                }
                if (ScriptableObject.isAccessorDescriptor(targetDesc)
                        && Undefined.isUndefined(targetDesc.get("get"))) {
                    if (!Undefined.isUndefined(trapResult)) {
                        throw ScriptRuntime.typeError(
                                "proxy get has to return the same value as the plain call");
                    }
                }
            }
            return trapResult;
        }

        if (start == this) {
            start = target;
        }
        SymbolScriptable symbolScriptableTarget = ensureSymbolScriptable(target);
        return symbolScriptableTarget.get(key, start);
    }

    /**
     * https://262.ecma-international.org/12.0/#sec-proxy-object-internal-methods-and-internal-slots-set-p-v-receiver
     */
    @Override
    public void put(String name, Scriptable start, Object value) {
        /*
         * 1. Assert: IsPropertyKey(P) is true.
         * 2. Let handler be O.[[ProxyHandler]].
         * 3. If handler is null, throw a TypeError exception.
         * 4. Assert: Type(handler) is Object.
         * 5. Let target be O.[[ProxyTarget]].
         * 6. Let trap be ? GetMethod(handler, "set").
         * 7. If trap is undefined, then
         *     a. Return ? target.[[Set]](P, V, Receiver).
         * 8. Let booleanTrapResult be ! ToBoolean(? Call(trap, handler, « target, P, V, Receiver »)).
         * 9. If booleanTrapResult is false, return false.
         * 10. Let targetDesc be ? target.[[GetOwnProperty]](P).
         * 11. If targetDesc is not undefined and targetDesc.[[Configurable]] is false, then
         *     a. If IsDataDescriptor(targetDesc) is true and targetDesc.[[Writable]] is false, then
         *         i. If SameValue(V, targetDesc.[[Value]]) is false, throw a TypeError exception.
         *     b. If IsAccessorDescriptor(targetDesc) is true, then
         *         i. If targetDesc.[[Set]] is undefined, throw a TypeError exception.
         * 12. Return true.
         */
        ScriptableObject target = getTargetThrowIfRevoked();

        Callable trap = getTrap(TRAP_SET);
        if (trap != null) {
            boolean booleanTrapResult =
                    ScriptRuntime.toBoolean(callTrap(trap, new Object[] {target, name, value}));
            if (!booleanTrapResult) {
                return; // false
            }

            ScriptableObject targetDesc =
                    target.getOwnPropertyDescriptor(Context.getContext(), name);
            if (targetDesc != null
                    && !Undefined.isUndefined(targetDesc)
                    && Boolean.FALSE.equals(targetDesc.get("configurable"))) {
                if (ScriptableObject.isDataDescriptor(targetDesc)
                        && Boolean.FALSE.equals(targetDesc.get("writable"))) {
                    if (!Objects.equals(value, targetDesc.get("value"))) {
                        throw ScriptRuntime.typeError(
                                "proxy set has to use the same value as the plain call");
                    }
                }
                if (ScriptableObject.isAccessorDescriptor(targetDesc)
                        && Undefined.isUndefined(targetDesc.get("set"))) {
                    throw ScriptRuntime.typeError("proxy set has to be available");
                }
            }
            return; // true
        }

        if (start == this) {
            start = target;
        }
        target.put(name, start, value);
    }

    /**
     * https://262.ecma-international.org/12.0/#sec-proxy-object-internal-methods-and-internal-slots-set-p-v-receiver
     */
    @Override
    public void put(int index, Scriptable start, Object value) {
        /*
         * 1. Assert: IsPropertyKey(P) is true.
         * 2. Let handler be O.[[ProxyHandler]].
         * 3. If handler is null, throw a TypeError exception.
         * 4. Assert: Type(handler) is Object.
         * 5. Let target be O.[[ProxyTarget]].
         * 6. Let trap be ? GetMethod(handler, "set").
         * 7. If trap is undefined, then
         *     a. Return ? target.[[Set]](P, V, Receiver).
         * 8. Let booleanTrapResult be ! ToBoolean(? Call(trap, handler, « target, P, V, Receiver »)).
         * 9. If booleanTrapResult is false, return false.
         * 10. Let targetDesc be ? target.[[GetOwnProperty]](P).
         * 11. If targetDesc is not undefined and targetDesc.[[Configurable]] is false, then
         *     a. If IsDataDescriptor(targetDesc) is true and targetDesc.[[Writable]] is false, then
         *         i. If SameValue(V, targetDesc.[[Value]]) is false, throw a TypeError exception.
         *     b. If IsAccessorDescriptor(targetDesc) is true, then
         *         i. If targetDesc.[[Set]] is undefined, throw a TypeError exception.
         * 12. Return true.
         */
        ScriptableObject target = getTargetThrowIfRevoked();

        Callable trap = getTrap(TRAP_SET);
        if (trap != null) {
            boolean booleanTrapResult =
                    ScriptRuntime.toBoolean(
                            callTrap(
                                    trap,
                                    new Object[] {target, ScriptRuntime.toString(index), value}));
            if (!booleanTrapResult) {
                return; // false
            }

            ScriptableObject targetDesc =
                    target.getOwnPropertyDescriptor(Context.getContext(), index);
            if (targetDesc != null
                    && !Undefined.isUndefined(targetDesc)
                    && Boolean.FALSE.equals(targetDesc.get("configurable"))) {
                if (ScriptableObject.isDataDescriptor(targetDesc)
                        && Boolean.FALSE.equals(targetDesc.get("writable"))) {
                    if (!Objects.equals(value, targetDesc.get("value"))) {
                        throw ScriptRuntime.typeError(
                                "proxy set has to use the same value as the plain call");
                    }
                }
                if (ScriptableObject.isAccessorDescriptor(targetDesc)
                        && Undefined.isUndefined(targetDesc.get("set"))) {
                    throw ScriptRuntime.typeError("proxy set has to be available");
                }
            }
            return; // true
        }

        if (start == this) {
            start = target;
        }
        target.put(index, start, value);
    }

    /**
     * https://262.ecma-international.org/12.0/#sec-proxy-object-internal-methods-and-internal-slots-set-p-v-receiver
     */
    @Override
    public void put(Symbol key, Scriptable start, Object value) {
        /*
         * 1. Assert: IsPropertyKey(P) is true.
         * 2. Let handler be O.[[ProxyHandler]].
         * 3. If handler is null, throw a TypeError exception.
         * 4. Assert: Type(handler) is Object.
         * 5. Let target be O.[[ProxyTarget]].
         * 6. Let trap be ? GetMethod(handler, "set").
         * 7. If trap is undefined, then
         *     a. Return ? target.[[Set]](P, V, Receiver).
         * 8. Let booleanTrapResult be ! ToBoolean(? Call(trap, handler, « target, P, V, Receiver »)).
         * 9. If booleanTrapResult is false, return false.
         * 10. Let targetDesc be ? target.[[GetOwnProperty]](P).
         * 11. If targetDesc is not undefined and targetDesc.[[Configurable]] is false, then
         *     a. If IsDataDescriptor(targetDesc) is true and targetDesc.[[Writable]] is false, then
         *         i. If SameValue(V, targetDesc.[[Value]]) is false, throw a TypeError exception.
         *     b. If IsAccessorDescriptor(targetDesc) is true, then
         *         i. If targetDesc.[[Set]] is undefined, throw a TypeError exception.
         * 12. Return true.
         */
        ScriptableObject target = getTargetThrowIfRevoked();

        Callable trap = getTrap(TRAP_SET);
        if (trap != null) {
            boolean booleanTrapResult =
                    ScriptRuntime.toBoolean(callTrap(trap, new Object[] {target, key, value}));
            if (!booleanTrapResult) {
                return; // false
            }

            ScriptableObject targetDesc =
                    target.getOwnPropertyDescriptor(Context.getContext(), key);
            if (targetDesc != null
                    && !Undefined.isUndefined(targetDesc)
                    && Boolean.FALSE.equals(targetDesc.get("configurable"))) {
                if (ScriptableObject.isDataDescriptor(targetDesc)
                        && Boolean.FALSE.equals(targetDesc.get("writable"))) {
                    if (!Objects.equals(value, targetDesc.get("value"))) {
                        throw ScriptRuntime.typeError(
                                "proxy set has to use the same value as the plain call");
                    }
                }
                if (ScriptableObject.isAccessorDescriptor(targetDesc)
                        && Undefined.isUndefined(targetDesc.get("set"))) {
                    throw ScriptRuntime.typeError("proxy set has to be available");
                }
            }
            return; // true
        }

        if (start == this) {
            start = target;
        }
        SymbolScriptable symbolScriptableTarget = ensureSymbolScriptable(target);
        symbolScriptableTarget.put(key, start, value);
    }

    /**
     * see
     * https://262.ecma-international.org/12.0/#sec-proxy-object-internal-methods-and-internal-slots-delete-p
     */
    @Override
    public void delete(String name) {
        /*
         * 1. Assert: IsPropertyKey(P) is true.
         * 2. Let handler be O.[[ProxyHandler]].
         * 3. If handler is null, throw a TypeError exception.
         * 4. Assert: Type(handler) is Object.
         * 5. Let target be O.[[ProxyTarget]].
         * 6. Let trap be ? GetMethod(handler, "deleteProperty").
         * 7. If trap is undefined, then
         *     a. Return ? target.[[Delete]](P).
         * 8. Let booleanTrapResult be ! ToBoolean(? Call(trap, handler, « target, P »)).
         * 9. If booleanTrapResult is false, return false.
         * 10. Let targetDesc be ? target.[[GetOwnProperty]](P).
         * 11. If targetDesc is undefined, return true.
         * 12. If targetDesc.[[Configurable]] is false, throw a TypeError exception.
         * 13. Let extensibleTarget be ? IsExtensible(target).
         * 14. If extensibleTarget is false, throw a TypeError exception.
         * 15. Return true.
         */
        ScriptableObject target = getTargetThrowIfRevoked();

        Callable trap = getTrap(TRAP_DELETE_PROPERTY);
        if (trap != null) {
            boolean booleanTrapResult =
                    ScriptRuntime.toBoolean(callTrap(trap, new Object[] {target, name}));
            if (!booleanTrapResult) {
                return; // false
            }

            ScriptableObject targetDesc =
                    target.getOwnPropertyDescriptor(Context.getContext(), name);
            if (targetDesc == null) {
                return; // true
            }
            if (Boolean.FALSE.equals(targetDesc.get("configurable")) || !target.isExtensible()) {
                throw ScriptRuntime.typeError(
                        "proxy can't delete an existing own property ' + name + ' on an not configurable or not extensible object");
            }

            return; // true
        }

        target.delete(name);
    }

    /**
     * see
     * https://262.ecma-international.org/12.0/#sec-proxy-object-internal-methods-and-internal-slots-delete-p
     */
    @Override
    public void delete(int index) {
        /*
         * 1. Assert: IsPropertyKey(P) is true.
         * 2. Let handler be O.[[ProxyHandler]].
         * 3. If handler is null, throw a TypeError exception.
         * 4. Assert: Type(handler) is Object.
         * 5. Let target be O.[[ProxyTarget]].
         * 6. Let trap be ? GetMethod(handler, "deleteProperty").
         * 7. If trap is undefined, then
         *     a. Return ? target.[[Delete]](P).
         * 8. Let booleanTrapResult be ! ToBoolean(? Call(trap, handler, « target, P »)).
         * 9. If booleanTrapResult is false, return false.
         * 10. Let targetDesc be ? target.[[GetOwnProperty]](P).
         * 11. If targetDesc is undefined, return true.
         * 12. If targetDesc.[[Configurable]] is false, throw a TypeError exception.
         * 13. Let extensibleTarget be ? IsExtensible(target).
         * 14. If extensibleTarget is false, throw a TypeError exception.
         * 15. Return true.
         */
        ScriptableObject target = getTargetThrowIfRevoked();

        Callable trap = getTrap(TRAP_DELETE_PROPERTY);
        if (trap != null) {
            boolean booleanTrapResult =
                    ScriptRuntime.toBoolean(
                            callTrap(trap, new Object[] {target, ScriptRuntime.toString(index)}));
            if (!booleanTrapResult) {
                return; // false
            }

            ScriptableObject targetDesc =
                    target.getOwnPropertyDescriptor(Context.getContext(), index);
            if (targetDesc == null) {
                return; // true
            }
            if (Boolean.FALSE.equals(targetDesc.get("configurable")) || !target.isExtensible()) {
                throw ScriptRuntime.typeError(
                        "proxy can't delete an existing own property ' + name + ' on an not configurable or not extensible object");
            }

            return; // true
        }

        target.delete(index);
    }

    /**
     * see
     * https://262.ecma-international.org/12.0/#sec-proxy-object-internal-methods-and-internal-slots-delete-p
     */
    @Override
    public void delete(Symbol key) {
        /*
         * 1. Assert: IsPropertyKey(P) is true.
         * 2. Let handler be O.[[ProxyHandler]].
         * 3. If handler is null, throw a TypeError exception.
         * 4. Assert: Type(handler) is Object.
         * 5. Let target be O.[[ProxyTarget]].
         * 6. Let trap be ? GetMethod(handler, "deleteProperty").
         * 7. If trap is undefined, then
         *     a. Return ? target.[[Delete]](P).
         * 8. Let booleanTrapResult be ! ToBoolean(? Call(trap, handler, « target, P »)).
         * 9. If booleanTrapResult is false, return false.
         * 10. Let targetDesc be ? target.[[GetOwnProperty]](P).
         * 11. If targetDesc is undefined, return true.
         * 12. If targetDesc.[[Configurable]] is false, throw a TypeError exception.
         * 13. Let extensibleTarget be ? IsExtensible(target).
         * 14. If extensibleTarget is false, throw a TypeError exception.
         * 15. Return true.
         */
        ScriptableObject target = getTargetThrowIfRevoked();

        Callable trap = getTrap(TRAP_DELETE_PROPERTY);
        if (trap != null) {
            boolean booleanTrapResult =
                    ScriptRuntime.toBoolean(callTrap(trap, new Object[] {target, key}));
            if (!booleanTrapResult) {
                return; // false
            }

            ScriptableObject targetDesc =
                    target.getOwnPropertyDescriptor(Context.getContext(), key);
            if (targetDesc == null) {
                return; // true
            }
            if (Boolean.FALSE.equals(targetDesc.get("configurable")) || !target.isExtensible()) {
                throw ScriptRuntime.typeError(
                        "proxy can't delete an existing own property ' + name + ' on an not configurable or not extensible object");
            }

            return; // true
        }

        SymbolScriptable symbolScriptableTarget = ensureSymbolScriptable(target);
        symbolScriptableTarget.delete(key);
    }

    /**
     * see
     * https://262.ecma-international.org/12.0/#sec-proxy-object-internal-methods-and-internal-slots-getownproperty-p
     */
    @Override
    protected ScriptableObject getOwnPropertyDescriptor(Context cx, Object id) {
        /*
         * 1. Assert: IsPropertyKey(P) is true.
         * 2. Let handler be O.[[ProxyHandler]].
         * 3. If handler is null, throw a TypeError exception.
         * 4. Assert: Type(handler) is Object.
         * 5. Let target be O.[[ProxyTarget]].
         * 6. Let trap be ? GetMethod(handler, "getOwnPropertyDescriptor").
         * 7. If trap is undefined, then
         *    a. Return ? target.[[GetOwnProperty]](P).
         * 8. Let trapResultObj be ? Call(trap, handler, « target, P »).
         * 9. If Type(trapResultObj) is neither Object nor Undefined, throw a TypeError exception.
         * 10. Let targetDesc be ? target.[[GetOwnProperty]](P).
         * 11. If trapResultObj is undefined, then
         *     a. If targetDesc is undefined, return undefined.
         *     b. If targetDesc.[[Configurable]] is false, throw a TypeError exception.
         *     c. Let extensibleTarget be ? IsExtensible(target).
         *     d. If extensibleTarget is false, throw a TypeError exception.
         *     e. Return undefined.
         * 12. Let extensibleTarget be ? IsExtensible(target).
         * 13. Let resultDesc be ? ToPropertyDescriptor(trapResultObj).
         * 14. Call CompletePropertyDescriptor(resultDesc).
         * 15. Let valid be IsCompatiblePropertyDescriptor(extensibleTarget, resultDesc, targetDesc).
         * 16. If valid is false, throw a TypeError exception.
         * 17. If resultDesc.[[Configurable]] is false, then
         *     a. If targetDesc is undefined or targetDesc.[[Configurable]] is true, then
         *         i. Throw a TypeError exception.
         *     b. If resultDesc has a [[Writable]] field and resultDesc.[[Writable]] is false, then
         *         i. If targetDesc.[[Writable]] is true, throw a TypeError exception.
         * 18. Return resultDesc.
         */
        ScriptableObject target = getTargetThrowIfRevoked();

        Callable trap = getTrap(TRAP_GET_OWN_PROPERTY_DESCRIPTOR);
        if (trap != null) {
            Object trapResultObj = callTrap(trap, new Object[] {target, id});
            if (!Undefined.isUndefined(trapResultObj)
                    && !(trapResultObj instanceof Scriptable
                            && !ScriptRuntime.isSymbol(trapResultObj))) {
                throw ScriptRuntime.typeError(
                        "getOwnPropertyDescriptor trap has to return undefined or an object");
            }

            ScriptableObject targetDesc;
            if (ScriptRuntime.isSymbol(id)) {
                targetDesc = target.getOwnPropertyDescriptor(cx, id);
            } else {
                targetDesc = target.getOwnPropertyDescriptor(cx, ScriptRuntime.toString(id));
            }

            if (Undefined.isUndefined(trapResultObj)) {
                if (Undefined.isUndefined(targetDesc)) {
                    return null;
                }

                if (Boolean.FALSE.equals(targetDesc.get("configurable"))
                        || !target.isExtensible()) {
                    throw ScriptRuntime.typeError(
                            "proxy can't report an existing own property '"
                                    + id
                                    + "' as non-existent on a non-extensible object");
                }
                return null;
            }

            Scriptable trapResult = (Scriptable) trapResultObj;
            if (trapResultObj != null) {
                Object value = ScriptableObject.getProperty(trapResult, "value");
                int attributes =
                        applyDescriptorToAttributeBitset(
                                DONTENUM | READONLY | PERMANENT,
                                getProperty(trapResult, "enumerable"),
                                getProperty(trapResult, "writable"),
                                getProperty(trapResult, "configurable"));

                ScriptableObject desc =
                        ScriptableObject.buildDataDescriptor(target, value, attributes);
                return desc;
            }
            return null;
        }

        if (ScriptRuntime.isSymbol(id)) {
            return target.getOwnPropertyDescriptor(cx, id);
        }

        return target.getOwnPropertyDescriptor(cx, ScriptRuntime.toString(id));
    }

    /**
     * see
     * https://262.ecma-international.org/12.0/#sec-proxy-object-internal-methods-and-internal-slots-defineownproperty-p-desc
     */
    @Override
    public boolean defineOwnProperty(Context cx, Object id, ScriptableObject desc) {
        /*
         * 1. Assert: IsPropertyKey(P) is true.
         * 2. Let handler be O.[[ProxyHandler]].
         * 3. If handler is null, throw a TypeError exception.
         * 4. Assert: Type(handler) is Object.
         * 5. Let target be O.[[ProxyTarget]].
         * 6. Let trap be ? GetMethod(handler, "defineProperty").
         * 7. If trap is undefined, then
         *    a. Return ? target.[[DefineOwnProperty]](P, Desc).
         * 8. Let descObj be FromPropertyDescriptor(Desc).
         * 9. Let booleanTrapResult be ! ToBoolean(? Call(trap, handler, « target, P, descObj »)).
         * 10. If booleanTrapResult is false, return false.
         * 11. Let targetDesc be ? target.[[GetOwnProperty]](P).
         * 12. Let extensibleTarget be ? IsExtensible(target).
         * 13. If Desc has a [[Configurable]] field and if Desc.[[Configurable]] is false, then
         *     a. Let settingConfigFalse be true.
         * 14. Else, let settingConfigFalse be false.
         * 15. If targetDesc is undefined, then
         *     a. If extensibleTarget is false, throw a TypeError exception.
         *     b. If settingConfigFalse is true, throw a TypeError exception.
         * 16. Else,
         *     a. If IsCompatiblePropertyDescriptor(extensibleTarget, Desc, targetDesc) is false, throw a TypeError exception.
         *     b. If settingConfigFalse is true and targetDesc.[[Configurable]] is true, throw a TypeError exception.
         *     c. If IsDataDescriptor(targetDesc) is true, targetDesc.[[Configurable]] is false, and targetDesc.[[Writable]] is true, then
         *         i. If Desc has a [[Writable]] field and Desc.[[Writable]] is false, throw a TypeError exception.
         * 17. Return true.
         */
        ScriptableObject target = getTargetThrowIfRevoked();

        Callable trap = getTrap(TRAP_DEFINE_PROPERTY);
        if (trap != null) {
            boolean booleanTrapResult =
                    ScriptRuntime.toBoolean(callTrap(trap, new Object[] {target, id, desc}));
            if (!booleanTrapResult) {
                return false;
            }

            ScriptableObject targetDesc = target.getOwnPropertyDescriptor(Context.getContext(), id);
            boolean extensibleTarget = target.isExtensible();

            boolean settingConfigFalse =
                    Boolean.TRUE.equals(ScriptableObject.hasProperty(desc, "configurable"))
                            && Boolean.FALSE.equals(desc.get("configurable"));

            if (targetDesc == null) {
                if (!extensibleTarget || settingConfigFalse) {
                    throw ScriptRuntime.typeError(
                            "proxy can't define an incompatible property descriptor");
                }
            } else {
                if (!AbstractEcmaObjectOperations.isCompatiblePropertyDescriptor(
                        cx, extensibleTarget, desc, targetDesc)) {
                    throw ScriptRuntime.typeError(
                            "proxy can't define an incompatible property descriptor");
                }

                if (settingConfigFalse && Boolean.TRUE.equals(targetDesc.get("configurable"))) {
                    throw ScriptRuntime.typeError(
                            "proxy can't define an incompatible property descriptor");
                }

                if (ScriptableObject.isDataDescriptor(targetDesc)
                        && Boolean.FALSE.equals(targetDesc.get("configurable"))
                        && Boolean.TRUE.equals(targetDesc.get("writable"))) {
                    if (Boolean.TRUE.equals(ScriptableObject.hasProperty(desc, "writable"))
                            && Boolean.FALSE.equals(desc.get("writable"))) {
                        throw ScriptRuntime.typeError(
                                "proxy can't define an incompatible property descriptor");
                    }
                }
            }
            return true;
        }

        return target.defineOwnProperty(cx, id, desc);
    }

    /**
     * see
     * https://262.ecma-international.org/12.0/#sec-proxy-object-internal-methods-and-internal-slots-isextensible
     */
    @Override
    public boolean isExtensible() {
        /*
         * 1. Let handler be O.[[ProxyHandler]].
         * 2. If handler is null, throw a TypeError exception.
         * 3. Assert: Type(handler) is Object.
         * 4. Let target be O.[[ProxyTarget]].
         * 5. Let trap be ? GetMethod(handler, "isExtensible").
         * 6. If trap is undefined, then
         *     a. a. Return ? IsExtensible(target).
         * 7. Let booleanTrapResult be ! ToBoolean(? Call(trap, handler, « target »)).
         * 8. Let targetResult be ? IsExtensible(target).
         * 9. If SameValue(booleanTrapResult, targetResult) is false, throw a TypeError exception.
         * 10. Return booleanTrapResult.
         */
        ScriptableObject target = getTargetThrowIfRevoked();

        Callable trap = getTrap(TRAP_IS_EXTENSIBLE);
        if (trap == null) {
            return target.isExtensible();
        }

        boolean booleanTrapResult = ScriptRuntime.toBoolean(callTrap(trap, new Object[] {target}));
        if (booleanTrapResult != target.isExtensible()) {
            throw ScriptRuntime.typeError(
                    "IsExtensible trap has to return the same value as the target");
        }
        return booleanTrapResult;
    }

    /**
     * see
     * https://262.ecma-international.org/12.0/#sec-proxy-object-internal-methods-and-internal-slots-preventextensions
     */
    @Override
    public boolean preventExtensions() {
        /*
         * 1. Let handler be O.[[ProxyHandler]].
         * 2. If handler is null, throw a TypeError exception.
         * 3. Assert: Type(handler) is Object.
         * 4. Let target be O.[[ProxyTarget]].
         * 5. Let trap be ? GetMethod(handler, "preventExtensions").
         * 6. If trap is undefined, then
         *     a. Return ? target.[[PreventExtensions]]().
         * 7. Let booleanTrapResult be ! ToBoolean(? Call(trap, handler, « target »)).
         * 8. If booleanTrapResult is true, then
         *     a. Let extensibleTarget be ? IsExtensible(target).
         *     b. If extensibleTarget is true, throw a TypeError exception.
         * 9. Return booleanTrapResult.
         */
        ScriptableObject target = getTargetThrowIfRevoked();

        Callable trap = getTrap(TRAP_PREVENT_EXTENSIONS);
        if (trap == null) {
            return target.preventExtensions();
        }
        boolean booleanTrapResult = ScriptRuntime.toBoolean(callTrap(trap, new Object[] {target}));
        if (booleanTrapResult && target.isExtensible()) {
            throw ScriptRuntime.typeError("target is extensible but trap returned true");
        }

        return booleanTrapResult;
    }

    @Override
    public String getTypeOf() {
        return typeOf;
    }

    /**
     * see
     * https://262.ecma-international.org/12.0/#sec-proxy-object-internal-methods-and-internal-slots-getprototypeof
     */
    @Override
    public Scriptable getPrototype() {
        /*
         * 1. Let handler be O.[[ProxyHandler]].
         * 2. If handler is null, throw a TypeError exception.
         * 3. Assert: Type(handler) is Object.
         * 4. Let target be O.[[ProxyTarget]].
         * 5. Let trap be ? GetMethod(handler, "getPrototypeOf").
         * 6. If trap is undefined, then
         *     a. Return ? target.[[GetPrototypeOf]]().
         * 7. Let handlerProto be ? Call(trap, handler, « target »).
         * 8. If Type(handlerProto) is neither Object nor Null, throw a TypeError exception.
         * 9. Let extensibleTarget be ? IsExtensible(target).
         * 10. If extensibleTarget is true, return handlerProto.
         * 11. Let targetProto be ? target.[[GetPrototypeOf]]().
         * 12. If SameValue(handlerProto, targetProto) is false, throw a TypeError exception.
         * 13. Return handlerProto.
         */
        ScriptableObject target = getTargetThrowIfRevoked();

        Callable trap = getTrap(TRAP_GET_PROTOTYPE_OF);
        if (trap != null) {
            Object handlerProto = callTrap(trap, new Object[] {target});

            Scriptable handlerProtoScriptable = Undefined.SCRIPTABLE_UNDEFINED;
            if (handlerProtoScriptable == null
                    || Undefined.isUndefined(handlerProto)
                    || ScriptRuntime.isSymbol(handlerProto)) {
                throw ScriptRuntime.typeErrorById(
                        "msg.arg.not.object", ScriptRuntime.typeof(handlerProto));
            }

            handlerProtoScriptable = ensureScriptable(handlerProto);

            if (target.isExtensible()) {
                return handlerProtoScriptable;
            }
            if (handlerProto != target.getPrototype()) {
                throw ScriptRuntime.typeError(
                        "getPrototypeOf trap has to return the original prototype");
            }
            return handlerProtoScriptable;
        }

        return target.getPrototype();
    }

    private void setPrototypeDirect(Scriptable prototype) {
        super.setPrototype(prototype);
    }

    /**
     * see
     * https://262.ecma-international.org/12.0/#sec-proxy-object-internal-methods-and-internal-slots-setprototypeof-v
     */
    @Override
    public void setPrototype(Scriptable prototype) {
        /*
         * 1. Assert: Either Type(V) is Object or Type(V) is Null.
         * 2. Let handler be O.[[ProxyHandler]].
         * 3. If handler is null, throw a TypeError exception.
         * 4. Assert: Type(handler) is Object.
         * 5. Let target be O.[[ProxyTarget]].
         * 6. Let trap be ? GetMethod(handler, "setPrototypeOf").
         * 7. If trap is undefined, then
         *     a. Return ? target.[[SetPrototypeOf]](V).
         * 8. Let booleanTrapResult be ! ToBoolean(? Call(trap, handler, « target, V »)).
         * 9. If booleanTrapResult is false, return false.
         * 10. Let extensibleTarget be ? IsExtensible(target).
         * 11. If extensibleTarget is true, return true.
         * 12. Let targetProto be ? target.[[SetPrototypeOf]]().
         * 13. If SameValue(V, targetProto) is false, throw a TypeError exception.
         * 14. Return true.
         */
        ScriptableObject target = getTargetThrowIfRevoked();

        Callable trap = getTrap(TRAP_SET_PROTOTYPE_OF);
        if (trap != null) {
            boolean booleanTrapResult =
                    ScriptRuntime.toBoolean(callTrap(trap, new Object[] {target, prototype}));
            if (!booleanTrapResult) {
                return; // false
            }
            if (target.isExtensible()) {
                return; // true
            }

            return;
        }

        target.setPrototype(prototype);
    }

    /**
     * see
     * https://262.ecma-international.org/12.0/#sec-proxy-object-internal-methods-and-internal-slots-call-thisargument-argumentslist
     */
    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        /*
         * 1. Let handler be O.[[ProxyHandler]].
         * 2. If handler is null, throw a TypeError exception.
         * 3. Assert: Type(handler) is Object.
         * 4. Let target be O.[[ProxyTarget]].
         * 5. Let trap be ? GetMethod(handler, "apply").
         * 6. If trap is undefined, then
         *     a. Return ? Call(target, thisArgument, argumentsList).
         * 7. Let argArray be ! CreateArrayFromList(argumentsList).
         * 8. Return ? Call(trap, handler, « target, thisArgument, argArray »).
         */
        ScriptableObject target = getTargetThrowIfRevoked();

        Scriptable argumentsList = cx.newArray(scope, args);

        Callable trap = getTrap(TRAP_APPLY);
        if (trap != null) {
            return callTrap(trap, new Object[] {target, thisObj, argumentsList});
        }

        return ScriptRuntime.applyOrCall(
                true, cx, scope, target, new Object[] {thisObj, argumentsList});
    }

    private static NativeProxy constructor(Context cx, Scriptable scope, Object[] args) {
        if (args.length < 2) {
            throw ScriptRuntime.typeErrorById(
                    "msg.method.missing.parameter",
                    "Proxy.ctor",
                    "2",
                    Integer.toString(args.length));
        }
        ScriptableObject target = ensureScriptableObjectButNotSymbol(args[0]);
        ScriptableObject handler = ensureScriptableObjectButNotSymbol(args[1]);

        NativeProxy proxy = new NativeProxy(target, handler);
        proxy.setPrototypeDirect(ScriptableObject.getClassPrototype(scope, PROXY_TAG));
        proxy.setParentScope(scope);
        return proxy;
    }

    // Proxy.revocable
    private static Object revocable(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (!ScriptRuntime.isObject(thisObj)) {
            throw ScriptRuntime.typeErrorById("msg.arg.not.object", ScriptRuntime.typeof(thisObj));
        }
        NativeProxy proxy = constructor(cx, scope, args);

        NativeObject revocable = (NativeObject) cx.newObject(scope);

        revocable.put("proxy", revocable, proxy);
        revocable.put("revoke", revocable, new LambdaFunction(scope, "", 0, new Revoker(proxy)));
        return revocable;
    }

    private Callable getTrap(String trapName) {
        Object handlerProp = ScriptableObject.getProperty(handlerObj, trapName);
        if (Scriptable.NOT_FOUND == handlerProp) {
            return null;
        }
        if (handlerProp == null || Undefined.isUndefined(handlerProp)) {
            return null;
        }
        if (!(handlerProp instanceof Callable)) {
            throw ScriptRuntime.notFunctionError(handlerProp, trapName);
        }

        return (Callable) handlerProp;
    }

    private Object callTrap(Callable trap, Object[] args) {
        return trap.call(Context.getContext(), handlerObj, handlerObj, args);
    }

    ScriptableObject getTargetThrowIfRevoked() {
        if (targetObj == null) {
            throw ScriptRuntime.typeError("Illegal operation attempted on a revoked proxy");
        }
        return targetObj;
    }
}
