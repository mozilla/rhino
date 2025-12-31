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
}
