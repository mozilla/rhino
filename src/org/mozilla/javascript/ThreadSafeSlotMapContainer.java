/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.Iterator;
import java.util.concurrent.locks.StampedLock;

/**
 * This class extends the SlotMapContainer so that we have thread-safe access to all the properties
 * of an object.
 */
class ThreadSafeSlotMapContainer extends SlotMapContainer {

    private final StampedLock lock = new StampedLock();

    ThreadSafeSlotMapContainer() {}

    ThreadSafeSlotMapContainer(int initialSize) {
        super(initialSize);
    }

    @Override
    public int size() {
        long stamp = lock.tryOptimisticRead();
        int s = map.size();
        if (lock.validate(stamp)) {
            return s;
        }

        stamp = lock.readLock();
        try {
            return map.size();
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public int dirtySize() {
        assert (lock.isReadLocked());
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        long stamp = lock.tryOptimisticRead();
        boolean e = map.isEmpty();
        if (lock.validate(stamp)) {
            return e;
        }

        stamp = lock.readLock();
        try {
            return map.isEmpty();
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public Slot modify(Object key, int index, int attributes) {
        final long stamp = lock.writeLock();
        try {
            checkMapSize();
            return map.modify(key, index, attributes);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public void replace(Slot oldSlot, Slot newSlot) {
        final long stamp = lock.writeLock();
        try {
            map.replace(oldSlot, newSlot);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public Slot query(Object key, int index) {
        long stamp = lock.tryOptimisticRead();
        Slot s = map.query(key, index);
        if (lock.validate(stamp)) {
            return s;
        }

        stamp = lock.readLock();
        try {
            return map.query(key, index);
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public void add(Slot newSlot) {
        final long stamp = lock.writeLock();
        try {
            checkMapSize();
            map.add(newSlot);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public void remove(Object key, int index) {
        final long stamp = lock.writeLock();
        try {
            map.remove(key, index);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Take out a read lock on the slot map, if locking is implemented. The caller MUST call this
     * method before using the iterator, and MUST NOT call this method otherwise.
     */
    @Override
    public long readLock() {
        return lock.readLock();
    }

    /**
     * Unlock the lock taken out by readLock.
     *
     * @param stamp the value returned by readLock.
     */
    @Override
    public void unlockRead(long stamp) {
        lock.unlockRead(stamp);
    }

    @Override
    public Iterator<Slot> iterator() {
        assert (lock.isReadLocked());
        return map.iterator();
    }

    /**
     * Before inserting a new item in the map, check and see if we need to expand from the embedded
     * map to a HashMap that is more robust against large numbers of hash collisions.
     */
    @Override
    protected void checkMapSize() {
        assert (lock.isWriteLocked());
        super.checkMapSize();
    }
}
