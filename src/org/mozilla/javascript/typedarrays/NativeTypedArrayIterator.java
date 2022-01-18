/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.typedarrays;

import java.util.ListIterator;
import java.util.NoSuchElementException;

public class NativeTypedArrayIterator<T> implements ListIterator<T> {
    private final NativeTypedArrayView<T> view;

    /** Position represents the position of the NEXT element */
    private int position;

    private int lastPosition = -1;

    NativeTypedArrayIterator(NativeTypedArrayView<T> view, int start) {
        this.view = view;
        this.position = start;
    }

    @Override
    public boolean hasNext() {
        return (position < view.length);
    }

    @Override
    public boolean hasPrevious() {
        return (position > 0);
    }

    @Override
    public int nextIndex() {
        return position;
    }

    @Override
    public int previousIndex() {
        return position - 1;
    }

    @Override
    public T next() {
        if (hasNext()) {
            T ret = view.get(position);
            lastPosition = position;
            position++;
            return ret;
        }
        throw new NoSuchElementException();
    }

    @Override
    public T previous() {
        if (hasPrevious()) {
            position--;
            lastPosition = position;
            return view.get(position);
        }
        throw new NoSuchElementException();
    }

    @Override
    public void set(T t) {
        if (lastPosition < 0) {
            throw new IllegalStateException();
        }
        view.js_set(lastPosition, t);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(T t) {
        throw new UnsupportedOperationException();
    }
}
