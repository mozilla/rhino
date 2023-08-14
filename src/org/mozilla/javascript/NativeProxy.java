/* -*- Mode: java; tab-width: 4; indent-tabs-mode: 1; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * This class implements the Proxy object.
 *
 * @author Ronald Brill
 */
final class NativeProxy extends IdScriptableObject implements Callable {
    private static final long serialVersionUID = 6676871870513494844L;

    private static final Object PROXY_TAG = "Proxy";

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

    private final boolean isCtor;
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

    static void init(Scriptable scope, boolean sealed) {
        NativeProxy constructor = new NativeProxy(null, null, true);
        IdFunctionObject ctor = constructor.exportAsJSClass(MAX_PROTOTYPE_ID, scope, false);
        ctor.setPrototypeProperty(null);
    }

    private NativeProxy(ScriptableObject target, Scriptable handler, boolean isCtor) {
        this.target = target;
        this.handler = handler;
        this.isCtor = isCtor;

        if (target == null || !(target instanceof Callable)) {
            typeOf = super.getTypeOf();
        }
        else {
            typeOf = target.getTypeOf();
        }
    }

    @Override
    public String getClassName() {
        if (isCtor) {
            return "Proxy";
        }

        assertNotRevoked();
        return target.getClassName();
    }

    @Override
    protected void initPrototypeId(int id) {
        if (id <= MAX_PROTOTYPE_ID) {
            String name;
            int arity;
            switch (id) {
                case Id_constructor:
                    arity = 2;
                    name = "constructor";
                    break;

                default:
                    throw new IllegalStateException(String.valueOf(id));
            }
            initPrototypeMethod(PROXY_TAG, id, name, arity);
        }
    }

    @Override
    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (!f.hasTag(PROXY_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }

        int methodId = f.methodId();
        switch (methodId) {
            case Id_constructor:
                if (thisObj != null && cx.getLanguageVersion() >= Context.VERSION_ES6) {
                    throw ScriptRuntime.typeErrorById("msg.only.from.new", getClassName());
                }
                return js_constructor(cx, scope, args);

            case ConstructorId_revocable:
                return js_revocable(cx, scope, args);

            default:
                throw new IllegalStateException(String.valueOf(methodId));
        }
    }

    @Override
    public boolean has(String name, Scriptable start) {
        assertNotRevoked();

        Callable trap = getTrap(TRAP_HAS);
        if (trap != null) {
            return (boolean) callTrap(trap, new Object[] {target, name});
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

    @Override
    public Object[] getIds() {
        if (isCtor) {
            return super.getIds();
        }

        assertNotRevoked();

        Callable trap = getTrap(TRAP_OWN_KEYS);
        if (trap != null) {
            Object res = callTrap(trap, new Object[] {target});
            if (res instanceof NativeArray) {
                return ((NativeArray) res).toArray();
            }
            return (Object[]) res;
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

    @Override
    public boolean isExtensible() {
        assertNotRevoked();

        Callable trap = getTrap(TRAP_IS_EXTENSIBLE);
        if (trap != null) {
            callTrap(trap, new Object[] {target});
        }

        return target.isExtensible();
    }

    @Override
    public void preventExtensions() {
        assertNotRevoked();

        Callable trap = getTrap(TRAP_PREVENT_EXTENSIONS);
        if (trap != null) {
            callTrap(trap, new Object[] {target});
        }

        target.preventExtensions();
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

    private NativeProxy js_constructor(Context cx, Scriptable scope, Object[] args) {
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

        NativeProxy proxy = new NativeProxy(trgt, hndlr, false);
        proxy.setPrototype(ScriptableObject.getClassPrototype(scope, proxy.getClassName()));
        proxy.setParentScope(scope);
        return proxy;
    }

    private NativeObject js_revocable(Context cx, Scriptable scope, Object[] args) {
        NativeProxy proxy = js_constructor(cx, scope, args);

        NativeObject revocable = (NativeObject) cx.newObject(scope);

        revocable.put("proxy", revocable, proxy);
        revocable.put("revoke", revocable, new Revoker(proxy));
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
        if (!isCtor && target == null) {
            throw ScriptRuntime.typeError("Illegal operation attempted on a revoked proxy");
        }
    }

    @Override
    protected int findPrototypeId(String s) {
        int id;
        switch (s) {
            case "constructor":
                id = Id_constructor;
                break;

            default:
                id = 0;
                break;
        }
        return id;
    }

    @Override
    protected void fillConstructorProperties(IdFunctionObject ctor) {
        addIdFunctionProperty(ctor, PROXY_TAG, ConstructorId_revocable, "revocable", 2);
        super.fillConstructorProperties(ctor);
    }

    private static final int ConstructorId_revocable = -1,
            Id_constructor = 1,
            MAX_PROTOTYPE_ID = Id_constructor;
}
