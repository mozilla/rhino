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
import java.io.Serial;
import java.io.Serializable;

/**
 * Generic notion of reference object that know how to query/modify the target objects based on some
 * property/index.
 */
public abstract class Ref implements Serializable {

    @Serial private static final long serialVersionUID = 4044540354730911424L;

    public boolean has(Context cx) {
        return true;
    }

    public abstract Object get(Context cx);

    /**
     * @deprecated Use {@link #set(Context, VarScope, Object)} instead
     */
    @Deprecated
    public abstract Object set(Context cx, Object value);

    public Object set(Context cx, VarScope scope, Object value) {
        return set(cx, value);
    }

    public boolean delete(Context cx) {
        return false;
    }

    @Serial
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        Context.checkDeserializationEnabled();
        stream.defaultReadObject();
    }

    @Serial
    private void writeObject(ObjectOutputStream stream) throws IOException {
        Context.checkSerializationEnabled();
        stream.defaultWriteObject();
    }

    @Serial
    private void readObjectNoData() throws InvalidObjectException {
        throw new InvalidObjectException("Missing serialized Rhino object data");
    }
}
