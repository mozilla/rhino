/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Avoid loading classes unless they are used.
 *
 * <p>This improves startup time and average memory usage.
 */
public final class LazilyLoadedCtor implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final int STATE_BEFORE_INIT = 0;
    private static final int STATE_INITIALIZING = 1;
    private static final int STATE_WITH_VALUE = 2;

    private final Scriptable scope;
    private final Initializable initializer;
    private final String propertyName;
    private final boolean sealed;
    private final boolean privileged;
    private Object initializedValue;
    private int state;

    /**
     * Create a constructor using a lambda function. The lambda should initialize the new value
     * however it needs and then return the value. The lambda may also return null, which indicates
     * that it stored the necessary property (and possibly other properties) in the scope itself.
     * This is legacy behavior used by some initializers that register many objects in a single
     * initialization function.
     */
    public LazilyLoadedCtor(
            ScriptableObject scope,
            String propertyName,
            boolean sealed,
            boolean privileged,
            Initializable initializer) {
        this.scope = scope;
        this.propertyName = propertyName;
        this.sealed = sealed;
        this.privileged = privileged;
        this.state = STATE_BEFORE_INIT;
        this.initializer = initializer;

        scope.addLazilyInitializedValue(propertyName, 0, this, ScriptableObject.DONTENUM);
    }

    /**
     * Create a constructor using a lambda function. The lambda should initialize the new value
     * however it needs and then return the value. The lambda may also return null, which indicates
     * that it stored the necessary property (and possibly other properties) in the scope itself.
     * This is legacy behavior used by some initializers that register many objects in a single
     * initialization function.
     */
    public LazilyLoadedCtor(
            ScriptableObject scope,
            String propertyName,
            boolean sealed,
            Initializable initializer,
            boolean privileged) {
        this(scope, propertyName, sealed, privileged, initializer);
    }

    /**
     * Create a constructor that loads via reflection, looking for an "init" method on the class.
     * This is a legacy mechanism.
     */
    public LazilyLoadedCtor(
            ScriptableObject scope, String propertyName, String className, boolean sealed) {
        this(scope, propertyName, className, sealed, false);
    }

    /**
     * Create a constructor that loads via reflection, looking for an "init" method on the class.
     * This is a legacy mechanism.
     */
    public LazilyLoadedCtor(
            ScriptableObject scope,
            String propertyName,
            String className,
            boolean sealed,
            boolean privileged) {
        this(
                scope,
                propertyName,
                sealed,
                privileged,
                (Context lcx, Scriptable lscope, boolean lsealed) ->
                        buildUsingReflection(lscope, className, propertyName, lsealed));
    }

    void init() {
        synchronized (this) {
            if (state == STATE_INITIALIZING)
                throw new IllegalStateException("Recursive initialization for " + propertyName);
            if (state == STATE_BEFORE_INIT) {
                state = STATE_INITIALIZING;
                // Set value now to have something to set in finally block if
                // buildValue throws.
                Object value = Scriptable.NOT_FOUND;
                try {
                    value = buildValue();
                } finally {
                    initializedValue = value;
                    state = STATE_WITH_VALUE;
                }
            }
        }
    }

    Object getValue() {
        if (state != STATE_WITH_VALUE) throw new IllegalStateException(propertyName);
        return initializedValue;
    }

    private Object buildValue() {

        if (privileged) {
            return AccessController.doPrivileged(
                    (PrivilegedAction<Object>) () -> buildValueInternal());
        }
        return buildValueInternal();
    }

    private Object buildValueInternal() {
        Context cx = Context.getCurrentContext();
        Object value = initializer.initialize(cx, scope, sealed);
        if (value != null) {
            return value;
        }
        return scope.get(propertyName, scope);
    }

    private static Object buildUsingReflection(
            Scriptable scope, String className, String propertyName, boolean sealed) {
        Class<? extends Scriptable> cl = cast(Kit.classOrNull(className));
        if (cl != null) {
            try {
                Object value = ScriptableObject.buildClassCtor(scope, cl, sealed, false);
                if (value != null) {
                    return value;
                }
                // cl has own static initializer which is expected
                // to set the property on its own.
                value = scope.get(propertyName, scope);
                if (value != Scriptable.NOT_FOUND) return value;
            } catch (InvocationTargetException ex) {
                Throwable target = ex.getTargetException();
                if (target instanceof RuntimeException) {
                    throw (RuntimeException) target;
                }
            } catch (RhinoException
                    | InstantiationException
                    | IllegalAccessException
                    | SecurityException ex) {
                // Ignore, which is the legacy behavior
            }
        }
        return Scriptable.NOT_FOUND;
    }

    @SuppressWarnings({"unchecked"})
    private static Class<? extends Scriptable> cast(Class<?> cl) {
        return (Class<? extends Scriptable>) cl;
    }
}
