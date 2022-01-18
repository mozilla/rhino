/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// API class

package org.mozilla.javascript;

/**
 * Interface to represent arbitrary action that requires to have Context object associated with the
 * current thread for its execution.
 *
 * @param T the type of the return value of action execution
 */
public interface ContextAction<T> {
    /**
     * Execute action using the supplied Context instance. When Rhino runtime calls the method,
     * <code>cx</code> will be associated with the current thread as active context.
     *
     * @see ContextFactory#call(ContextAction)
     */
    public T run(Context cx);
}
