/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.io.Serializable;

/**
 * This class implements the Undefined value in JavaScript.
 *
 * <p>We represent "undefined" internally using two static objects -- "Undefined.instance" and
 * SCRIPTABLE_UNDEFINED. Java code that needs to make something undefined should generally use the
 * first, and use the second if a Scriptable object is absolutely required.
 *
 * <p>Java code that needs to test whether something is undefined <b>must</b> use the "isUndefined"
 * method because of the multiple internal representations.
 */
public class Undefined implements Serializable {
    private static final long serialVersionUID = 9195680630202616767L;

    /**
     * This is the standard value for "undefined" in Rhino. Java code that needs to represent
     * "undefined" should use this object (rather than a new instance of this class).
     */
    public static final Object instance = new Undefined();

    private static final int instanceHash = System.identityHashCode(instance);

    private Undefined() {}

    public Object readResolve() {
        return instance;
    }

    @Override
    public boolean equals(Object obj) {
        return isUndefined(obj) || obj == this;
    }

    @Override
    public int hashCode() {
        // All instances of Undefined are equivalent!
        return instanceHash;
    }

    /**
     * An alternate representation of undefined, to be used only when we need to pass it to a method
     * that takes as Scriptable as a parameter. This is used when we need to pass undefined as the
     * "this" parameter of a Callable instance, because we cannot change that interface without
     * breaking backward compatibility.
     */
    public static final Scriptable SCRIPTABLE_UNDEFINED =
            new Scriptable() {
                @Override
                public String getClassName() {
                    return "undefined";
                }

                @Override
                public Object get(String name, Scriptable start) {
                    return NOT_FOUND;
                }

                @Override
                public Object get(int index, Scriptable start) {
                    return NOT_FOUND;
                }

                @Override
                public boolean has(String name, Scriptable start) {
                    return false;
                }

                @Override
                public boolean has(int index, Scriptable start) {
                    return false;
                }

                @Override
                public void put(String name, Scriptable start, Object value) {}

                @Override
                public void put(int index, Scriptable start, Object value) {}

                @Override
                public void delete(String name) {}

                @Override
                public void delete(int index) {}

                @Override
                public Scriptable getPrototype() {
                    return null;
                }

                @Override
                public void setPrototype(Scriptable prototype) {}

                @Override
                public Scriptable getParentScope() {
                    return null;
                }

                @Override
                public void setParentScope(Scriptable parent) {}

                @Override
                public Object[] getIds() {
                    return ScriptRuntime.emptyArgs;
                }

                @Override
                public Object getDefaultValue(Class<?> hint) {
                    if (hint == null || hint == ScriptRuntime.StringClass) {
                        return toString();
                    }
                    return null;
                }

                @Override
                public boolean hasInstance(Scriptable instance) {
                    return false;
                }

                @Override
                public String toString() {
                    return "undefined";
                }

                @Override
                public boolean equals(Object obj) {
                    return isUndefined(obj) || (obj == this);
                }

                @Override
                public int hashCode() {
                    return instanceHash;
                }
            };

    /**
     * Safely test whether "obj" is undefined. Java code must use this function rather than testing
     * the value directly since we have two representations of undefined in Rhino.
     */
    public static boolean isUndefined(Object obj) {
        return Undefined.instance == obj || Undefined.SCRIPTABLE_UNDEFINED == obj;
    }
}
