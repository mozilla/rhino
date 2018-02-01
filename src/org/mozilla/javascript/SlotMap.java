/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * A SlotMap is an interface to the main data structure that contains all the "Slots"
 * that back a ScriptableObject. It is the primary property map in Rhino. It is
 * Iterable but does not implement java.util.Map because that comes with a bunch
 * of overhead that we do not need.
 *
 * This class generally has a bit of a strange interface, and its interactions with
 * ScriptableObject are complex. Many attempts to make this interface more elegant have
 * resulted in substantial performance regressions so we are doing the best that we can.
 */

public interface SlotMap
    extends Iterable<ScriptableObject.Slot> {

    /**
     * Return the size of the map.
     */
    int size();

    /**
     * Return whether the map is empty.
     */
    boolean isEmpty();

    /**
     * Return the Slot that matches EITHER "key" or "index". (It will use "key"
     * if it is not null, and otherwise "index". "accessType" is one of the
     * constants defined in ScriptableObject.
     */
    ScriptableObject.Slot get(Object key, int index, ScriptableObject.SlotAccess accessType);

    /**
     * This is an optimization that is the same as get with an accessType of SLOT_QUERY.
     * It should be used instead of SLOT_QUERY because it is more efficient.
     */
    ScriptableObject.Slot query(Object key, int index);

    /**
     * Insert a new slot to the map. Both "name" and "indexOrHash" must be populated.
     * Note that ScriptableObject generally adds slots via the "get" method.
     */
    void addSlot(ScriptableObject.Slot newSlot);

    /**
     * Remove the slot at either "key" or "index".
     */
    void remove(Object key, int index);
}

