/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * A SlotMap is an interface to the main data structure that contains all the "Slots" that back a
 * ScriptableObject. It is the primary property map in Rhino. It is Iterable but does not implement
 * java.util.Map because that comes with a bunch of overhead that we do not need.
 *
 * <p>This class generally has a bit of a strange interface, and its interactions with
 * ScriptableObject are complex. Many attempts to make this interface more elegant have resulted in
 * substantial performance regressions so we are doing the best that we can.
 */
public interface SlotMap extends Iterable<Slot> {

    /** Return the size of the map. */
    int size();

    /** Return whether the map is empty. */
    boolean isEmpty();

    /**
     * Return the Slot that matches EITHER "key" or "index". (It will use "key" if it is not null,
     * and otherwise "index".) If no slot exists, then create a default slot class.
     *
     * @param key The key for the slot, which should be a String or a Symbol.
     * @param index if key is zero, then this will be used as the key instead.
     * @param attributes the attributes to be set on the slot if a new slot is created. Existing
     *     slots will not be modified.
     * @return a Slot, which will be created anew if no such slot exists.
     */
    Slot modify(Object key, int index, int attributes);

    /**
     * Retrieve the slot at EITHER key or index, or return null if the slot cannot be found.
     *
     * @param key The key for the slot, which should be a String or a Symbol.
     * @param index if key is zero, then this will be used as the key instead.
     * @return either the Slot that matched the key and index, or null
     */
    Slot query(Object key, int index);

    /** Replace "slot" with a new slot. This is used to change slot types. */
    void replace(Slot oldSlot, Slot newSlot);

    /**
     * Insert a new slot to the map. Both "name" and "indexOrHash" must be populated. Note that
     * ScriptableObject generally adds slots via the "modify" method.
     */
    void add(Slot newSlot);

    /**
     * Remove the slot at either "key" or "index".
     *
     * @param key The key for the slot, which should be a String or a Symbol.
     * @param index if key is zero, then this will be used as the key instead.
     */
    void remove(Object key, int index);
}
