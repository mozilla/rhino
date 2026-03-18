/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// API class

package org.mozilla.javascript;

/**
 * Interface the common route of JS objects and environment records, i.e. a thing that has
 * properties and some form of ancestor. Although environment records do not hold user accessible
 * keys which are symbols or numbers, it is useful to include these as it makes the concrete map
 * implementations simpler and they may be useful for internally maintained vbalues.
 */
public interface PropHolder<T extends PropHolder<T>> {

    public Object get(Symbol key, T start);

    public Object get(String name, T start);

    public Object get(int index, T start);

    public boolean has(Symbol name, T start);

    public boolean has(String name, T start);

    public boolean has(int index, T start);

    public void put(Symbol name, T start, Object value);

    public void put(String name, T start, Object value);

    public void put(int index, T start, Object value);

    public void delete(Symbol name);

    public void delete(String name);

    public void delete(int index);

    /**
     * Get the parent scope of the object.
     *
     * @return the parent scope
     */
    public T getAncestor();
}
