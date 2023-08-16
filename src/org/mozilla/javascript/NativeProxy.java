/* -*- Mode: java; tab-width: 4; indent-tabs-mode: 1; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    private ScriptableObject target;
    private Scriptable handler;
    private final String typeOf;

    private static final class Revoker implements Callable {
        private NativeProxy revocableProxy = null;

        public Revoker(NativeProxy proxy){
            revocableProxy = proxy;
        }

        @Override
        public Object call(Context cx, Scriptable scope, Scriptable thisObj,
                Object[] args) {
            if (revocableProxy != null) {
                revocableProxy.handler = null;
                revocableProxy.target = null;
                revocableProxy = null;
            }
            return Undefined.instance;
        }
    }


    public static void init(Context cx, Scriptable scope, boolean sealed) {
        LambdaConstructor constructor =
                new LambdaConstructor(
                        scope,
                        PROXY_TAG,
                        2,
                        LambdaConstructor.CONSTRUCTOR_NEW,
                        NativeProxy::constructor) {

            @Override
            public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
                NativeProxy obj = (NativeProxy) getTargetConstructor().construct(cx, scope, args);
                // avoid getting trapped
                obj.setPrototypeDirect(getClassPrototype());
                obj.setParentScope(scope);
                return obj;
            }
        };
        constructor.setPrototypeProperty(null);

        constructor.defineConstructorMethod(
                scope, "revocable", 2, NativeProxy::revocable, DONTENUM, DONTENUM | READONLY);

        ScriptableObject.defineProperty(scope, PROXY_TAG, constructor, DONTENUM);
        if (sealed) {
            constructor.sealObject();
        }
    }

    private NativeProxy(ScriptableObject target, Scriptable handler) {
        this.target = target;
        this.handler = handler;

        if (target == null || !(target instanceof Callable)) {
            typeOf = super.getTypeOf();
        }
        else {
            typeOf = target.getTypeOf();
        }
    }

    @Override
    public String getClassName() {
        assertNotRevoked();
        return target.getClassName();
    }

    @Override
    public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
        assertNotRevoked();

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
    @Override
    public boolean has(String name, Scriptable start) {
        assertNotRevoked();

        Callable trap = getTrap(TRAP_HAS);
        if (trap != null) {

            boolean booleanTrapResult = ScriptRuntime.toBoolean(callTrap(trap, new Object[] {target, name}));
            if (!booleanTrapResult) {
                ScriptableObject targetDesc = target.getOwnPropertyDescriptor(Context.getCurrentContext(), name);
                if (targetDesc != null) {
                    if (Boolean.FALSE.equals(targetDesc.get("configurable"))) {
                            throw ScriptRuntime.typeError("proxy can't report an existing own property '\"attr\"' as non-existent on a non-extensible object");
                    }
                    if (!target.isExtensible()) {
                        throw ScriptRuntime.typeError("proxy can't report an existing own property '\"attr\"' as non-existent on a non-extensible object");
                    }
                }
            }
            return booleanTrapResult;
        }

        return ScriptableObject.hasProperty(target, name);
    }

    @Override
    public boolean has(int index, Scriptable start) {
        assertNotRevoked();

        Callable trap = getTrap(TRAP_HAS);
        if (trap != null) {
            return (boolean) callTrap(trap, new Object[] {target, index});
        }

        return ScriptableObject.hasProperty(target, index);
    }

    @Override
    public boolean has(Symbol key, Scriptable start) {
        assertNotRevoked();

        Callable trap = getTrap(TRAP_HAS);
        if (trap != null) {
            return (boolean) callTrap(trap, new Object[] {target, key});
        }

        return ScriptableObject.hasProperty(target, key);
    }

    /**
     * see https://262.ecma-international.org/12.0/#sec-proxy-object-internal-methods-and-internal-slots-ownpropertykeys
     *
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
    @Override
    public Object[] getIds() {
        assertNotRevoked();

        Callable trap = getTrap(TRAP_OWN_KEYS);
        if (trap != null) {
            Object res = callTrap(trap, new Object[] {target});
            if (!(res instanceof Scriptable) || !ScriptRuntime.isArrayLike((Scriptable)res)) {
                throw ScriptRuntime.typeError("ToDo");
            }

            Context cx = Context.getCurrentContext();
            List<Object> trapResult = AbstractEcmaObjectOperations.createListFromArrayLike(cx, (Scriptable) res, new String[] {"String", "Symbol"});

            boolean extensibleTarget = target.isExtensible();
            Object[] targetKeys = target.getIds();

            ArrayList<Object> targetConfigurableKeys = new ArrayList<>();
            ArrayList<Object> targetNonconfigurableKeys = new ArrayList<>();
            for (Object targetKey : targetKeys) {
                ScriptableObject desc = target.getOwnPropertyDescriptor(cx, targetKey);
                if (desc != null&& Boolean.FALSE.equals(desc.get("configurable"))) {
                    targetNonconfigurableKeys.add(targetKey);
                }
                else {
                    targetConfigurableKeys.add(targetKey);
                }
            }

            if (extensibleTarget && targetNonconfigurableKeys.size() == 0) {
                return trapResult.toArray();
            }

            // ToDo Set?
            List<Object> uncheckedResultKeys = Arrays.asList(trapResult);
            for (Object key : targetNonconfigurableKeys) {
                if (!uncheckedResultKeys.contains(key)) {
                    throw ScriptRuntime.typeError("ToDo");
                }
                uncheckedResultKeys.remove(key);
            }
            if (extensibleTarget) {
                return trapResult.toArray();
            }

            for (Object key : targetConfigurableKeys) {
                if (!uncheckedResultKeys.contains(key)) {
                    throw ScriptRuntime.typeError("ToDo");
                }
                uncheckedResultKeys.remove(key);
            }

            if (uncheckedResultKeys.size() > 0) {
                throw ScriptRuntime.typeError("ToDo");
            }

            return trapResult.toArray();
        }

        return target.getIds();
    }

    @Override
    public Object[] getAllIds() {
        assertNotRevoked();

        return target.getAllIds();
    }

    @Override
    public Object get(String name, Scriptable start) {
        assertNotRevoked();

        Callable trap = getTrap(TRAP_GET);
        if (trap != null) {
            return callTrap(trap, new Object[] {target, name, this});
        }

        return ScriptRuntime.getObjectProp(target, name, Context.getContext());
    }

    @Override
    public Object get(int index, Scriptable start) {
        assertNotRevoked();

        Callable trap = getTrap(TRAP_GET);
        if (trap != null) {
            return callTrap(trap, new Object[] {target, index, this});
        }
        return ScriptRuntime.getObjectIndex(target, index, Context.getContext());
    }

    @Override
    public Object get(Symbol key, Scriptable start) {
        assertNotRevoked();

        Callable trap = getTrap(TRAP_GET);
        if (trap != null) {
            return callTrap(trap, new Object[] {target, key, this});
        }

        if (start == this) {
            start = target;
        }
        SymbolScriptable symbolScriptableTarget = ensureSymbolScriptable(target);
        return symbolScriptableTarget.get(key, start);
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        assertNotRevoked();

        Callable trap = getTrap(TRAP_SET);
        if (trap != null) {
            ScriptableObject desc = ScriptableObject.buildDataDescriptor(target, value, EMPTY);
            callTrap(trap, new Object[] {target, name, desc});
        }

        ScriptableObject.putProperty(target, name, value);
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
        assertNotRevoked();

        Callable trap = getTrap(TRAP_SET);
        if (trap != null) {
            ScriptableObject desc = ScriptableObject.buildDataDescriptor(target, value, EMPTY);
            callTrap(trap, new Object[] {target, index, desc});
        }

        ScriptableObject.putProperty(target, index, value);
    }

    @Override
    public void put(Symbol key, Scriptable start, Object value) {
        assertNotRevoked();

        Callable trap = getTrap(TRAP_SET);
        if (trap != null) {
            ScriptableObject desc = ScriptableObject.buildDataDescriptor(target, value, EMPTY);
            callTrap(trap, new Object[] {target, key, desc});
        }

        if (start == this) {
            start = target;
        }
        SymbolScriptable symbolScriptableTarget = ensureSymbolScriptable(target);
        symbolScriptableTarget.put(key, start, value);
    }

    @Override
    public void delete(String name) {
        assertNotRevoked();

        Callable trap = getTrap(TRAP_DELETE_PROPERTY);
        if (trap != null) {
            callTrap(trap, new Object[] {target, name});
        }

        target.delete(name);
    }

    @Override
    public void delete(int index) {
        assertNotRevoked();

        Callable trap = getTrap(TRAP_DELETE_PROPERTY);
        if (trap != null) {
            callTrap(trap, new Object[] {target, index});
        }

        target.delete(index);
    }

    @Override
    public void delete(Symbol key) {
        assertNotRevoked();

        Callable trap = getTrap(TRAP_DELETE_PROPERTY);
        if (trap != null) {
            callTrap(trap, new Object[] {target, key});
        }

        SymbolScriptable symbolScriptableTarget = ensureSymbolScriptable(target);
        symbolScriptableTarget.delete(key);
    }

    @Override
    protected ScriptableObject getOwnPropertyDescriptor(Context cx, Object id) {
        assertNotRevoked();

        Callable trap = getTrap(TRAP_GET_OWN_PROPERTY_DESCRIPTOR);
        if (trap != null) {
            ScriptableObject proxiedDescriptor = (ScriptableObject) callTrap(trap, new Object[] {target, id});
            if (proxiedDescriptor != null) {
                Object value = ScriptableObject.getProperty(proxiedDescriptor, "value");
                int attributes = applyDescriptorToAttributeBitset(DONTENUM | READONLY | PERMANENT, proxiedDescriptor);

                ScriptableObject desc = ScriptableObject.buildDataDescriptor(target, value, attributes);
                return desc;
            }
            // TODO
            return null;
        }

        if (ScriptRuntime.isSymbol(id)) {
            return target.getOwnPropertyDescriptor(cx, id);
        }

        return target.getOwnPropertyDescriptor(cx, ScriptRuntime.toString(id));
    }

    @Override
    public void defineOwnProperty(Context cx, Object id, ScriptableObject desc) {
        assertNotRevoked();

        Callable trap = getTrap(TRAP_DEFINE_PROPERTY);
        if (trap != null) {
            callTrap(trap, new Object[] {target, id, desc});
        }

        target.defineOwnProperty(cx, id, desc);
    }

    /**
     * see https://262.ecma-international.org/12.0/#sec-proxy-object-internal-methods-and-internal-slots-isextensible
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
    @Override
    public boolean isExtensible() {
        assertNotRevoked();

        Callable trap = getTrap(TRAP_IS_EXTENSIBLE);
        if (trap == null) {
            return target.isExtensible();
        }

        boolean booleanTrapResult = ScriptRuntime.toBoolean(callTrap(trap, new Object[] {target}));
        boolean targetResult = target.isExtensible();

        if (booleanTrapResult != targetResult) {
            throw ScriptRuntime.typeError("IsExtensible trap has to return the same value as the target");
        }
        return booleanTrapResult;
    }

    /**
     * see https://262.ecma-international.org/12.0/#sec-proxy-object-internal-methods-and-internal-slots-preventextensions
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
    @Override
    public void preventExtensions() {
        assertNotRevoked();

        Callable trap = getTrap(TRAP_PREVENT_EXTENSIONS);
        if (trap == null) {
            target.preventExtensions();
            return;
        }
        callTrap(trap, new Object[] {target});
        if (target.isExtensible()) {
            throw ScriptRuntime.typeError("target is not extensible");
        }
    }

    @Override
    public String getTypeOf() {
        return typeOf;
    }

    @Override
    public Scriptable getPrototype() {
        assertNotRevoked();

        Callable trap = getTrap(TRAP_GET_PROTOTYPE_OF);
        if (trap != null) {
            return (ScriptableObject) callTrap(trap, new Object[] {target});
        }

        return target.getPrototype();
    }

    private void setPrototypeDirect(Scriptable prototype) {
        super.setPrototype(prototype);
    }

    @Override
    public void setPrototype(Scriptable prototype) {
        assertNotRevoked();

        Callable trap = getTrap(TRAP_SET_PROTOTYPE_OF);
        if (trap != null) {
            callTrap(trap, new Object[] {target, prototype});
            return;
        }

        target.setPrototype(prototype);
    }

    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        assertNotRevoked();

        Scriptable argumentsList = cx.newArray(scope, args);

        Callable trap = getTrap(TRAP_APPLY);
        if (trap != null) {
            return callTrap(trap, new Object[] {target, thisObj, argumentsList});
        }

        return ScriptRuntime.applyOrCall(true, cx, scope, target, new Object[] {thisObj, argumentsList});
    }

    private static NativeProxy constructor(Context cx, Scriptable scope, Object[] args) {
        if (args.length < 2) {
            throw ScriptRuntime.typeErrorById(
                    "msg.method.missing.parameter",
                    "Proxy.ctor",
                    "2",
                    Integer.toString(args.length));
        }
        Scriptable s = ScriptRuntime.toObject(cx, scope, args[0]);
        ScriptableObject trgt = ensureScriptableObject(s);

        s = ScriptRuntime.toObject(cx, scope, args[1]);
        ScriptableObject hndlr = ensureScriptableObject(s);

        NativeProxy proxy = new NativeProxy(trgt, hndlr);
        proxy.setPrototypeDirect(ScriptableObject.getClassPrototype(scope, PROXY_TAG));
        proxy.setParentScope(scope);
        return proxy;
    }

    // Proxy.revocable
    private static Object revocable(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (!ScriptRuntime.isObject(thisObj)) {
            throw ScriptRuntime.typeErrorById("msg.arg.not.object", ScriptRuntime.typeof(thisObj));
        }
        NativeProxy proxy = constructor(cx, scope, args);

        NativeObject revocable = (NativeObject) cx.newObject(scope);

        revocable.put("proxy", revocable, proxy);
        revocable.put("revoke", revocable, new LambdaFunction(scope, "revoke", 0, new Revoker(proxy)));
        return revocable;
    }

    private Callable getTrap(String trapName) {
        Object handlerProp = ScriptableObject.getProperty(handler, trapName);
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
        return trap.call(Context.getCurrentContext(), handler, handler, args);
    }

    private void assertNotRevoked() {
        if (target == null) {
            throw ScriptRuntime.typeError("Illegal operation attempted on a revoked proxy");
        }
    }
}
