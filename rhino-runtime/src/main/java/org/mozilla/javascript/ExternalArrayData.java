/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * Implement this interface in order to allow external data to be attached to a ScriptableObject.
 */
public interface ExternalArrayData {
    /**
     * Return the element at the specified index. The result must be a type that is valid in
     * JavaScript: Number, String, or Scriptable. This method will not be called unless "index" is
     * in range.
     */
    Object getArrayElement(int index);

    /**
     * Set the element at the specified index. This method will not be called unless "index" is in
     * range. The method must check that "value" is a valid type, and convert it if necessary.
     */
    void setArrayElement(int index, Object value);

    /** Return the length of the array. */
    int getArrayLength();
}
