/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.io.Serial;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;

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
    @Serial private static final long serialVersionUID = 9195680630202616767L;

    /**
     * This is the standard value for "undefined" in Rhino. Java code that needs to represent
     * "undefined" should use this object (rather than a new instance of this class).
     */
    public static final Undefined instance = new Undefined();

    private static final int instanceHash = System.identityHashCode(instance);

    private Undefined() {}

    public Object readResolve() {
        return instance;
    }

    /** Bootstrap method used by {@code UndefinedDescriber} to resolve a dynamic constant. */
    public static Undefined undefinedConstant(
            MethodHandles.Lookup lookup, String name, Class<?> type) {
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
     * Safely test whether "obj" is undefined. Java code must use this function rather than testing
     * the value directly since we have two representations of undefined in Rhino.
     */
    public static boolean isUndefined(Object obj) {
        return Undefined.instance == obj;
    }
}
