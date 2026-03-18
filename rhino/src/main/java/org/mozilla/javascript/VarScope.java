/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// API class

package org.mozilla.javascript;

/**
 * Interface that represents an the bindings of an ECMAScript environment record. Unlike {@link
 * Scriptable} scopes do not have prototypes.
 */
public interface VarScope extends Scriptable, ConstProperties<Scriptable> {

    @Override
    default String getClassName() {
        Kit.codeBug("Attempt to get classname of scope.");
        return null;
    }

    @Override
    default Object getDefaultValue(Class<?> hint) {
        Kit.codeBug("Attempt to get default value of scope.");
        return null;
    }

    @Override
    default boolean hasInstance(Scriptable instance) {
        Kit.codeBug("Attempt to do hasInstance of scope.");
        return false;
    }

    @Override
    default void setParentScope(Scriptable parent) {
        Kit.codeBug("Attempt to change parent of scope.");
    }

    @Override
    default void setPrototype(Scriptable prototype) {
        Kit.codeBug("Attempt to set prototype of scope.");
    }

    @Override
    default Scriptable getPrototype() {
        return null;
    }
}
