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
public interface JSScope {

    public Object get(Symbol key, JSScope start);

    public Object get(String name, JSScope start);

    public Object get(int index, JSScope start);

    public boolean has(Symbol name, JSScope start);

    public boolean has(String name, JSScope start);

    public boolean has(int index, JSScope start);

    public void put(Symbol name, JSScope start, Object value);

    public void put(String name, JSScope start, Object value);

    public void put(int index, JSScope start, Object value);

    public void delete(Symbol name);

    public void delete(String name);

    public void delete(int index);

    /**
     * Get the parent scope of the object.
     *
     * @return the parent scope
     */
    public JSScope getParentScope();

    /**
     * Set the parent scope of the object.
     *
     * @param parent the parent scope to set
     */
    public void setParentScope(JSScope parent);
}
