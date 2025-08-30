/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.lang.ref.WeakReference;

/**
 * Implementation of the ES2021 WeakRef constructor and prototype.
 *
 * <p>WeakRef allows holding a weak reference to an object without preventing its garbage
 * collection. This is useful for caches, mappings, or any scenario where you want to reference an
 * object without keeping it alive.
 *
 * <p>The WeakRef object provides a single method, deref(), which returns the referenced object if
 * it's still alive, or undefined if it has been collected.
 *
 * @see <a href="https://tc39.es/ecma262/#sec-weak-ref-objects">ECMAScript WeakRef Objects</a>
 */
public class NativeWeakRef extends ScriptableObject {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = "WeakRef";
    private static final String DEREF_METHOD = "deref";

    // Constants
    private static final String MSG_NO_TARGET = "msg.weakref.no.target";
    private static final String MSG_TARGET_NOT_OBJECT = "msg.weakref.target.not.object";
    private static final int CONSTRUCTOR_ARITY = 1;
    private static final int DEREF_ARITY = 0;

    private WeakReference<Scriptable> weakReference;

    /**
     * Initializes the WeakRef constructor and prototype in the given scope.
     *
     * @param cx the JavaScript context
     * @param scope the scope to initialize in
     * @param sealed whether to seal the constructor and prototype
     * @return the WeakRef constructor
     */
    public static Object init(Context cx, Scriptable scope, boolean sealed) {
        LambdaConstructor constructor = createConstructor(scope);
        configurePrototype(constructor, scope);

        if (sealed) {
            sealConstructor(constructor);
        }
        return constructor;
    }

    private static LambdaConstructor createConstructor(Scriptable scope) {
        LambdaConstructor constructor =
                new LambdaConstructor(
                        scope,
                        CLASS_NAME,
                        CONSTRUCTOR_ARITY,
                        LambdaConstructor.CONSTRUCTOR_NEW,
                        NativeWeakRef::constructor);
        constructor.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);
        return constructor;
    }

    private static void configurePrototype(LambdaConstructor constructor, Scriptable scope) {
        // Define prototype methods
        constructor.definePrototypeMethod(
                scope,
                DEREF_METHOD,
                DEREF_ARITY,
                NativeWeakRef::deref,
                DONTENUM,
                DONTENUM | READONLY);

        // Define Symbol.toStringTag
        constructor.definePrototypeProperty(
                SymbolKey.TO_STRING_TAG, CLASS_NAME, DONTENUM | READONLY);
    }

    private static void sealConstructor(LambdaConstructor constructor) {
        constructor.sealObject();
        ScriptableObject prototype = (ScriptableObject) constructor.getPrototypeProperty();
        if (prototype != null) {
            prototype.sealObject();
        }
    }

    /**
     * WeakRef constructor implementation. Creates a new WeakRef instance holding a weak reference
     * to the target object.
     *
     * @param cx the current context
     * @param scope the scope
     * @param args constructor arguments, expects exactly one object argument
     * @return the new WeakRef instance
     * @throws TypeError if no argument provided or argument is not an object
     */
    private static Scriptable constructor(Context cx, Scriptable scope, Object[] args) {
        validateConstructorArgs(args);
        return createWeakRef((Scriptable) args[0]);
    }

    private static NativeWeakRef createWeakRef(Scriptable target) {
        NativeWeakRef ref = new NativeWeakRef();
        ref.weakReference = new WeakReference<>(target);
        return ref;
    }

    /**
     * Validates constructor arguments according to ES2021 spec.
     *
     * @param args the constructor arguments
     * @throws TypeError if validation fails
     */
    private static void validateConstructorArgs(Object[] args) {
        if (args.length < 1) {
            throw ScriptRuntime.typeErrorById(MSG_NO_TARGET);
        }

        Object target = args[0];
        if (!isValidTarget(target)) {
            throw ScriptRuntime.typeErrorById(MSG_TARGET_NOT_OBJECT);
        }
    }

    /**
     * Checks if a value is a valid WeakRef target. According to the spec, only objects (excluding
     * null, undefined, and symbols) can be weakly referenced.
     *
     * @param target the value to check
     * @return true if the target is a valid object reference
     */
    private static boolean isValidTarget(Object target) {
        return target instanceof Scriptable
                && target != Undefined.instance
                && target != null
                && !(target instanceof Symbol);
    }

    /**
     * WeakRef.prototype.deref() implementation. Returns the WeakRef's target object if it's still
     * alive, or undefined if it has been collected.
     *
     * @param cx the current context
     * @param scope the scope
     * @param thisObj the 'this' object
     * @param args method arguments (none expected)
     * @return the target object or undefined
     */
    private static Object deref(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeWeakRef self = ensureWeakRef(thisObj);
        return self.dereference();
    }

    private static NativeWeakRef ensureWeakRef(Scriptable thisObj) {
        return LambdaConstructor.convertThisObject(thisObj, NativeWeakRef.class);
    }

    /**
     * Dereferences the weak reference.
     *
     * @return the target object if still alive, or Undefined.instance if collected
     */
    private Object dereference() {
        if (weakReference == null) {
            return Undefined.instance;
        }

        Scriptable target = weakReference.get();
        return (target == null) ? Undefined.instance : target;
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }
}
