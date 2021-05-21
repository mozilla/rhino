/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.Iterator;

/**
 * This class holds the various SlotMaps of various types, and knows how to atomically switch
 * between them when we need to so that we use the right data structure at the right time.
 */
class SlotMapContainer implements SlotMap {

    /**
     * Once the object has this many properties in it, we will replace the EmbeddedSlotMap with
     * HashSlotMap. We can adjust this parameter to balance performance for typical objects versus
     * performance for huge objects with many collisions.
     */
    private static final int LARGE_HASH_SIZE = 2000;

    private static final int DEFAULT_SIZE = 10;

    protected SlotMap map;

    SlotMapContainer() {
        this(DEFAULT_SIZE);
    }

    SlotMapContainer(int initialSize) {
        if (initialSize > LARGE_HASH_SIZE) {
            map = new HashSlotMap();
        } else {
            map = new EmbeddedSlotMap();
        }
    }

    @Override
    public int size() {
        return map.size();
    }

    public int dirtySize() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public Slot modify(Object key, int index, int attributes) {
        checkMapSize();
        return map.modify(key, index, attributes);
    }

    @Override
    public void replace(Slot oldSlot, Slot newSlot) {
        map.replace(oldSlot, newSlot);
    }

    @Override
    public Slot query(Object key, int index) {
        return map.query(key, index);
    }

    @Override
    public void add(Slot newSlot) {
        checkMapSize();
        map.add(newSlot);
    }

    @Override
    public void remove(Object key, int index) {
        map.remove(key, index);
    }

    @Override
    public Iterator<Slot> iterator() {
        return map.iterator();
    }

    public long readLock() {
        // No locking in the default implementation
        return 0L;
    }

    public void unlockRead(long stamp) {
        // No locking in the default implementation
    }

    /**
     * Before inserting a new item in the map, check and see if we need to expand from the embedded
     * map to a HashMap that is more robust against large numbers of hash collisions.
     */
    protected void checkMapSize() {
        if ((map instanceof EmbeddedSlotMap) && map.size() >= LARGE_HASH_SIZE) {
            SlotMap newMap = new HashSlotMap();
            for (Slot s : map) {
                newMap.add(s);
            }
            map = newMap;
        }
    }
}
