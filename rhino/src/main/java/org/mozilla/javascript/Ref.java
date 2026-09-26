/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Generic notion of reference object that know how to query/modify the target objects based on some
 * property/index.
 */
public abstract class Ref implements Serializable {

    private static final long serialVersionUID = 4044540354730911424L;

    public boolean has(Context cx) {
        return true;
    }

    public abstract Object get(Context cx);

    /**
     * @deprecated Use {@link #set(Context, Scriptable, Object)} instead
     */
    @Deprecated
    public abstract Object set(Context cx, Object value);

    public Object set(Context cx, Scriptable scope, Object value) {
        return set(cx, value);
    }

    public boolean delete(Context cx) {
        return false;
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        Context.checkDeserializationEnabled();
        stream.defaultReadObject();
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        Context.checkSerializationEnabled();
        stream.defaultWriteObject();
    }

    private void readObjectNoData() throws InvalidObjectException {
        throw new InvalidObjectException("Missing serialized Rhino object data");
    }
}
