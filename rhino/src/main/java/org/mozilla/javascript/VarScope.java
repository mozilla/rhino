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
public interface VarScope extends PropHolder<VarScope>, ConstProperties<VarScope> {

    public static final Object UNINITIALIZED = UniqueTag.UNINITIALIZED;

    /**
     * Get the parent scope of this scope.
     *
     * @return the parent scope
     */
    public VarScope getParentScope();

    @Override
    default VarScope getAncestor() {
        return getParentScope();
    }

    default boolean isNestedScope() {
        return false;
    }

    default VarScope copyScope() {
        throw new UnsupportedOperationException(this.getClass().getName());
    }

    /**
     * Create a mutable binding which must return {@link UniqueTag.UNINITIALIZED} until an initial
     * value has been set.
     */
    public boolean createMutableBinding(String name, boolean deletable);

    public boolean createImmutableBinding(String name, boolean strict);

    public boolean initializeBinding(String name, Object value);

    /**
     * Returns true if this scope holds a binding for the name which has not been initialized yet.
     * This must not invoke any getter.
     */
    default boolean isUninitializedBinding(String name) {
        return false;
    }
}
