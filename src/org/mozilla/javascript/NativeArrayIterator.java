/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

public final class NativeArrayIterator extends ES6Iterator {
    private static final long serialVersionUID = 1L;
    private static final String ITERATOR_TAG = "ArrayIterator";

    static void init(ScriptableObject scope, boolean sealed) {
        ES6Iterator.init(scope, sealed, new NativeArrayIterator(), ITERATOR_TAG);
    }

    /**
     * Only for constructing the prototype object.
     */
    private NativeArrayIterator() {
        super();
    }

    NativeArrayIterator(Scriptable scope, Scriptable arrayLike) {
        super(scope);
        this.index = 0;
        this.arrayLike = arrayLike;
    }

    @Override
    public String getClassName() {
        return "Array Iterator";
    }

    @Override
    Object next(Context cx, Scriptable scope) {
        Object value = Undefined.instance;
        boolean done = index >= NativeArray.getLengthProperty(cx, arrayLike);
        if (!done) {
            value = arrayLike.get(index++, arrayLike);
            if (value == ScriptableObject.NOT_FOUND) {
                value = Undefined.instance;
            }
        }
        return makeIteratorResult(cx, scope, done, value);
    }

    @Override
    String getTag() {
        return ITERATOR_TAG;
    }

    private Scriptable arrayLike;
    private int index;
}

