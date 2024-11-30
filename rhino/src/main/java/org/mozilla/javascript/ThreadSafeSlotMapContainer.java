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
@SuppressWarnings("AndroidJdkLibsChecker")
class ThreadSafeSlotMapContainer extends SlotMapContainer {

    private final StampedLock lock = new StampedLock();

    ThreadSafeSlotMapContainer() {}

    ThreadSafeSlotMapContainer(int initialSize) {
        super(initialSize);
    }

    @Override
    public int size() {
        long stamp = lock.tryOptimisticRead();
        int s = getMap().size();
        if (lock.validate(stamp)) {
            return s;
        }

        stamp = lock.readLock();
        try {
            return getMap().size();
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public int dirtySize() {
        assert lock.isReadLocked();
        return getMap().size();
    }

    @Override
    public boolean isEmpty() {
        long stamp = lock.tryOptimisticRead();
        boolean e = getMap().isEmpty();
        if (lock.validate(stamp)) {
            return e;
        }

        stamp = lock.readLock();
        try {
            return getMap().isEmpty();
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public Slot modify(SlotMapOwner owner, Object key, int index, int attributes) {
        final long stamp = lock.writeLock();
        try {
            return getMap().modify(this, key, index, attributes);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public <S extends Slot> S compute(
            SlotMapOwner owner, Object key, int index, SlotComputer<S> c) {
        final long stamp = lock.writeLock();
        try {
            return getMap().compute(this, key, index, c);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public Slot query(Object key, int index) {
        long stamp = lock.tryOptimisticRead();
        Slot s = getMap().query(key, index);
        if (lock.validate(stamp)) {
            return s;
        }

        stamp = lock.readLock();
        try {
            return getMap().query(key, index);
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public void add(SlotMapOwner owner, Slot newSlot) {
        final long stamp = lock.writeLock();
        try {
            getMap().add(this, newSlot);
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
        assert lock.isReadLocked();
        return getMap().iterator();
    }

    /**
     * Before inserting a new item in the map, check and see if we need to expand from the embedded
     * map to a HashMap that is more robust against large numbers of hash collisions.
     */
    @Override
    protected void checkMapSize() {
        assert lock.isWriteLocked();
        super.checkMapSize();
    }
}
