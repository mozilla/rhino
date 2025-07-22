package org.mozilla.javascript;

/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/**
 * The ContextHolder encapsulates a ThreadLocal, that holds the current context. Note: former
 * methods have used an Object[] for performance reasons. This seems to be outdated. See
 * ContextHolderBenchmark
 *
 * @author Roland Praml
 */
class ContextHolder {
    private static final ThreadLocal<Context> contextLocal = new ThreadLocal<>();

    private ContextHolder() {}

    static void setContext(Context context) {
        contextLocal.set(context);
    }

    static Context getContext() {
        return contextLocal.get();
    }

    static void clearContext() {
        // do not use contextLocal.remove() here, as this might be much slower, when the same thread
        // creates a new context.
        // See ContextHolderBenchmark
        contextLocal.set(null);
    }
}
