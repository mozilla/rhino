/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// API class

package org.mozilla.javascript;

/**
 * This is interface that all functions in JavaScript must implement. The interface provides for
 * calling functions and constructors.
 *
 * @see org.mozilla.javascript.Scriptable
 * @author Norris Boyd
 */
public interface Function extends Scriptable, Callable, Constructable {
    /**
     * Call the function.
     *
     * <p>Note that the array of arguments is not guaranteed to have length greater than 0.
     *
     * @param cx the current Context for this thread
     * @param scope the scope to execute the function relative to. This is set to the value returned
     *     by getParentScope() except when the function is called from a closure.
     * @param thisObj the JavaScript {@code this} object
     * @param args the array of arguments
     * @return the result of the call
     */
    @Override
    Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args);

    /**
     * Call the function as a constructor.
     *
     * <p>This method is invoked by the runtime in order to satisfy a use of the JavaScript {@code
     * new} operator. This method is expected to create a new object and return it.
     *
     * @param cx the current Context for this thread
     * @param scope an enclosing scope of the caller except when the function is called from a
     *     closure.
     * @param args the array of arguments
     * @return the allocated object
     */
    @Override
    Scriptable construct(Context cx, Scriptable scope, Object[] args);

    /**
     * Return the scope in which this function was declared or closed over. This is the
     * "[[Environment]]" defined in https://tc39.es/ecma262/#sec-ecmascript-function-objects.
     * Although this is currently precisely equal to the parent scope (or "__parent__") of an object
     * it is useful to distinguish the two concepts as parent scopes are no longer supported by the
     * spec in general and present a significant barrier to future optimisations.
     */
    default Scriptable getDeclarationScope() {
        return this.getParentScope();
    }
}
