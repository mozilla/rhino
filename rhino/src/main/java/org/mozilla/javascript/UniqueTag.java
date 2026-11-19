/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.io.Serial;
import java.io.Serializable;

/**
 * Class instances represent serializable tags to mark special Object values.
 *
 * <p>Compatibility note: under jdk 1.1 use org.mozilla.javascript.serialize.ScriptableInputStream
 * to read serialized instances of UniqueTag as under this JDK version the default ObjectInputStream
 * would not restore them correctly as it lacks support for readResolve method
 */
public enum UniqueTag {
    /** Tag to mark non-existing values. */
    NOT_FOUND,

    /** Tag to distinguish between uninitialized and null values. */
    NULL_VALUE,

    /**
     * Tag to indicate that a object represents "double" with the real value stored somewhere else.
     */
    DOUBLE_MARK,

    /** Tag to represent an initiailized binding in a variable scope. */
    UNINITIALIZED;

    // Overridden for better debug printouts
    @Override
    public String toString() {
        return UniqueTag.class.getCanonicalName() + "@0:" + super.toString();
    }
}
