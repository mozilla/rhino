/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.EnumMap;

/**
 * A top-level scope object that provides special means to cache and preserve the initial values of
 * the built-in constructor properties for better ECMAScript compliance.
 *
 * <p>ECMA 262 requires that most constructors used internally construct objects with the original
 * prototype object as value of their [[Prototype]] internal property. Since built-in global
 * constructors are defined as writable and deletable, this means they should be cached to protect
 * against redefinition at runtime.
 *
 * <p>In order to implement this efficiently, this class provides a mechanism to access the original
 * built-in global constructors and their prototypes via numeric class-ids. To make use of this, the
 * new {@link ScriptRuntime#newBuiltinObject ScriptRuntime.newBuiltinObject} and {@link
 * ScriptRuntime#setBuiltinProtoAndParent ScriptRuntime.setBuiltinProtoAndParent} methods should be
 * used to create and initialize objects of built-in classes instead of their generic counterparts.
 *
 * <p>Calling {@link org.mozilla.javascript.Context#initStandardObjects()} with an instance of this
 * class as argument will automatically cache built-in classes after initialization. For other
 * setups involving top-level scopes that inherit global properties from their prototypes (e.g. with
 * dynamic scopes) embeddings should explicitly call {@link #cacheBuiltins(boolean)} to initialize
 * the class cache for each top-level scope.
 */
public class TopLevel extends ScriptableObject {

    private static final long serialVersionUID = -4648046356662472260L;

    /** An enumeration of built-in ECMAScript objects. */
    public enum Builtins {
        /** The built-in Object type. */
        Object,
        /** The built-in Array type. */
        Array,
        /** The built-in Function type. */
        Function,
        /** The built-in String type. */
        String,
        /** The built-in Number type. */
        Number,
        /** The built-in Boolean type. */
        Boolean,
        /** The built-in RegExp type. */
        RegExp,
        /** The built-in Error type. */
        Error,
        /** The built-in Symbol type. */
        Symbol,
        /** The built-in GeneratorFunction type. */
        GeneratorFunction,
        /** The built-in BigInt type. */
        BigInt,
        /** The built-in Promise type. */
        Promise,
        Date,
        ArrayBuffer,
        Int8Array,
        Uint8Array,
        Uint8ClampedArray,
        Int16Array,
        Uint16Array,
        Int32Array,
        Uint32Array,
        BigInt64Array,
        BigUint64Array,
        Float32Array,
        Float64Array,
        DataView
    }

    /** An enumeration of built-in native errors. [ECMAScript 5 - 15.11.6] */
    enum NativeErrors {
        /** The AggregateError */
        AggregateError,
        /** Basic Error */
        Error,
        /** The native EvalError. */
        EvalError,
        /** The native RangeError. */
        RangeError,
        /** The native ReferenceError. */
        ReferenceError,
        /** The native SyntaxError. */
        SyntaxError,
        /** The native TypeError. */
        TypeError,
        /** The native URIError. */
        URIError,
        /** The native InternalError (non-standard). */
        InternalError,
        /** The native JavaException (non-standard). */
        JavaException
    }

    public static class GlobalThis extends ScriptableObject {

        @Override
        public String getClassName() {
            return "global";
        }
    }

    private EnumMap<Builtins, BaseFunction> ctors;
    private EnumMap<NativeErrors, BaseFunction> errors;
    private transient ScriptableObject globalThis;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(globalThis);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        globalThis = (ScriptableObject) in.readObject();
    }

    public TopLevel() {
        this(new GlobalThis());
    }

    public TopLevel(ScriptableObject customGlobal) {
        globalThis = customGlobal;
    }

    public TopLevel createIsolate() {
        var newGlobal = new NativeObject();
        newGlobal.setPrototype(getGlobalThis());
        newGlobal.setParentScope(null);
        var isolate = new TopLevel(newGlobal);
        isolate.copyAssociatedValue(this);
        isolate.copyBuiltins(this, false);
        return isolate;
    }

    public TopLevel createIsolate(ScriptableObject customGlobal) {
        customGlobal.setParentScope(null);
        customGlobal.setPrototype(getGlobalThis());
        var isolate = new TopLevel(customGlobal);
        isolate.copyAssociatedValue(this);
        isolate.copyBuiltins(this, false);
        return isolate;
    }

    /**
     * Only use this function if you have already set the customGlobal's prototype chain to point to
     * this top level's global object. This should only be done if you know for certain that no
     * other use will be made of this prototype chain.
     */
    public TopLevel createIsolateCustomPrototypeChain(ScriptableObject customGlobal) {
        customGlobal.setParentScope(null);
        var isolate = new TopLevel(customGlobal);
        isolate.copyAssociatedValue(this);
        isolate.copyBuiltins(this, false);
        return isolate;
    }

    @Override
    public String getClassName() {
        return "topLevel";
    }

    /**
     * Cache the built-in ECMAScript objects to protect them against modifications by the script.
     * This method is called automatically by {@link ScriptRuntime#initStandardObjects
     * ScriptRuntime.initStandardObjects} if the scope argument is an instance of this class. It
     * only has to be called by the embedding if a top-level scope is not initialized through {@code
     * initStandardObjects()}.
     *
     * <p>This method is deprecated and kept for compatibility. Please use either {@link
     * cacheBuiltins(boolean)} or {@link copyBuiltins(TopLevel, boolean)} instead.
     */
    @Deprecated
    public void cacheBuiltins(TopLevel scope, boolean sealed) {
        cacheBuiltins(sealed);
    }

    public void cacheBuiltins(boolean sealed) {
        ctors = new EnumMap<>(Builtins.class);
        for (Builtins builtin : Builtins.values()) {
            Object value = ScriptableObject.getProperty(this, builtin.name());
            if (value instanceof BaseFunction) {
                ctors.put(builtin, (BaseFunction) value);
            } else if (builtin == Builtins.GeneratorFunction) {
                // Handle weird situation of "GeneratorFunction" being a real constructor
                // which is never registered in the top-level scope
                ctors.put(
                        builtin, (BaseFunction) BaseFunction.initAsGeneratorFunction(this, sealed));
            }
        }
        errors = new EnumMap<>(NativeErrors.class);
        for (NativeErrors error : NativeErrors.values()) {
            Object value = ScriptableObject.getProperty(this, error.name());
            if (value instanceof BaseFunction) {
                errors.put(error, (BaseFunction) value);
            }
        }
    }

    public void copyBuiltins(TopLevel other, boolean sealed) {
        ctors = other.ctors;
        errors = other.errors;
    }

    /** Clears the cache; this is necessary, when standard objects are reinitialized. */
    void clearCache() {
        ctors = null;
        errors = null;
    }

    /**
     * Static helper method to get a built-in object constructor with the given {@code type} from
     * the given {@code scope}. If the scope is not an instance of this class or does have a cache
     * of built-ins, the constructor is looked up via normal property lookup.
     *
     * @param cx the current Context
     * @param scope the top-level scope
     * @param type the built-in type
     * @return the built-in constructor
     */
    public static Function getBuiltinCtor(Context cx, Scriptable scope, Builtins type) {
        // must be called with top level scope
        assert scope.getParentScope() == null;
        if (scope instanceof TopLevel) {
            Function result = ((TopLevel) scope).getBuiltinCtor(type);
            if (result != null) {
                return result;
            }
        }
        // fall back to normal constructor lookup
        String typeName;
        if (type == Builtins.GeneratorFunction) {
            // GeneratorFunction isn't stored in scope with that name, but in case
            // we end up falling back to this value then we have to
            // look this up using a hidden name.
            typeName = BaseFunction.GENERATOR_FUNCTION_CLASS;
        } else {
            typeName = type.name();
        }
        return ScriptRuntime.getExistingCtor(cx, scope, typeName);
    }

    /**
     * Static helper method to get a native error constructor with the given {@code type} from the
     * given {@code scope}. If the scope is not an instance of this class or does have a cache of
     * native errors, the constructor is looked up via normal property lookup.
     *
     * @param cx the current Context
     * @param scope the top-level scope
     * @param type the native error type
     * @return the native error constructor
     */
    static Function getNativeErrorCtor(Context cx, Scriptable scope, NativeErrors type) {
        // must be called with top level scope
        assert scope.getParentScope() == null;
        if (scope instanceof TopLevel) {
            Function result = ((TopLevel) scope).getNativeErrorCtor(type);
            if (result != null) {
                return result;
            }
        }
        // fall back to normal constructor lookup
        return ScriptRuntime.getExistingCtor(cx, scope, type.name());
    }

    /**
     * Static helper method to get a built-in object prototype with the given {@code type} from the
     * given {@code scope}. If the scope is not an instance of this class or does have a cache of
     * built-ins, the prototype is looked up via normal property lookup.
     *
     * @param scope the top-level scope
     * @param type the built-in type
     * @return the built-in prototype
     */
    public static Scriptable getBuiltinPrototype(TopLevel scope, Builtins type) {
        // must be called with top level scope
        Scriptable result = ((TopLevel) scope).getBuiltinPrototype(type);
        if (result != null) {
            return result;
        }

        // fall back to normal prototype lookup
        String typeName;
        if (type == Builtins.GeneratorFunction) {
            // GeneratorFunction isn't stored in scope with that name, but in case
            // we end up falling back to this value then we have to
            // look this up using a hidden name.
            typeName = BaseFunction.GENERATOR_FUNCTION_CLASS;
        } else {
            typeName = type.name();
        }
        return ScriptableObject.getClassPrototype(scope, typeName);
    }

    /**
     * Get the cached built-in object constructor from this scope with the given {@code type}.
     * Returns null if {@link #cacheBuiltins(boolean)} has not been called on this object.
     *
     * @param type the built-in type
     * @return the built-in constructor
     */
    public BaseFunction getBuiltinCtor(Builtins type) {
        return ctors != null ? ctors.get(type) : null;
    }

    /**
     * Get the cached native error constructor from this scope with the given {@code type}. Returns
     * null if {@link #cacheBuiltins(boolean)} has not been called on this object.
     *
     * @param type the native error type
     * @return the native error constructor
     */
    BaseFunction getNativeErrorCtor(NativeErrors type) {
        return errors != null ? errors.get(type) : null;
    }

    /**
     * Get the cached built-in object prototype from this scope with the given {@code type}. Returns
     * null if {@link #cacheBuiltins(boolean)} has not been called on this object.
     *
     * @param type the built-in type
     * @return the built-in prototype
     */
    public Scriptable getBuiltinPrototype(Builtins type) {
        BaseFunction func = getBuiltinCtor(type);
        Object proto = func != null ? func.getPrototypeProperty() : null;
        return proto instanceof Scriptable ? (Scriptable) proto : null;
    }

    public ScriptableObject getGlobalThis() {
        return globalThis;
    }

    @Override
    public Object get(String name, Scriptable start) {
        var res = super.get(name, start);
        if (res != NOT_FOUND) {
            return res;
        }
        return ScriptableObject.getProperty(globalThis, name);
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        ScriptableObject.putProperty(globalThis, name, value);
    }

    @Override
    public boolean has(String name, Scriptable start) {
        return super.has(name, start) || ScriptableObject.hasProperty(globalThis, name);
    }

    @Override
    public void delete(String name) {
        globalThis.delete(name);
    }

    @Override
    public void sealObject() {
        globalThis.sealObject();
        super.sealObject();
    }

    @Override
    public void defineProperty(String propertyName, Object value, int attributes) {
        globalThis.defineProperty(propertyName, value, attributes);
    }

    @Override
    void addLazilyInitializedValue(String name, int index, LazilyLoadedCtor init, int attributes) {
        globalThis.addLazilyInitializedValue(name, index, init, attributes);
    }

    @Override
    public void setAttributes(String name, int attributes) {
        if (super.get(name, this) != NOT_FOUND) {
            super.setAttributes(name, attributes);
        } else {
            globalThis.setAttributes(name, attributes);
        }
    }

    @Override
    public int getAttributes(String name) {
        if (super.get(name, this) != NOT_FOUND) {
            return super.getAttributes(name);
        } else {
            return globalThis.getAttributes(name);
        }
    }

    // Technically this is wrong, but there are currently tests that
    // depend const variable being defined on globalThis.
    //
    // In a compliant implementation const declarations should bind
    // the values on the global scope but not on the global object.

    @Override
    public boolean isConst(String name) {
        if (super.get(name, this) != NOT_FOUND) {
            return super.isConst(name);
        } else {
            return globalThis.isConst(name);
        }
    }

    @Override
    public void putConst(String name, Scriptable start, Object value) {
        globalThis.putConst(name, globalThis, value);
    }

    @Override
    public void defineConst(String name, Scriptable start) {
        globalThis.defineConst(name, globalThis);
    }
}
