package org.mozilla.javascript;

import java.util.concurrent.locks.StampedLock;

@SuppressWarnings("AndroidJdkLibsChecker")
class ThreadSafeEmbeddedSlotMap extends EmbeddedSlotMap implements LockAwareSlotMap {

    private final StampedLock lock = new StampedLock();
    private volatile LockAwareSlotMap current = this;

    public ThreadSafeEmbeddedSlotMap() {
        super();
    }

    public ThreadSafeEmbeddedSlotMap(int capacity) {
        super(capacity);
    }

    @Override
    public int size() {
        long stamp = lock.tryOptimisticRead();
        int s = current.sizeWithLock();
        if (lock.validate(stamp)) {
            return s;
        }

        stamp = lock.readLock();
        try {
            return current.sizeWithLock();
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public int dirtySize() {
        assert lock.isReadLocked();
        return current.sizeWithLock();
    }

    @Override
    public boolean isEmpty() {
        long stamp = lock.tryOptimisticRead();
        boolean e = current.isEmptyWithLock();
        if (lock.validate(stamp)) {
            return e;
        }

        stamp = lock.readLock();
        try {
            return current.isEmptyWithLock();
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public Slot modify(SlotMapOwner owner, Object key, int index, int attributes) {
        final long stamp = lock.writeLock();
        try {
            return current.modifyWithLock(owner, key, index, attributes);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public <S extends Slot> S compute(
            SlotMapOwner owner, Object key, int index, SlotComputer<S> c) {
        final long stamp = lock.writeLock();
        try {
            return current.computeWithLock(owner, key, index, c);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public Slot query(Object key, int index) {
        long stamp = lock.tryOptimisticRead();
        Slot s = current.queryWithLock(key, index);
        if (lock.validate(stamp)) {
            return s;
        }

        stamp = lock.readLock();
        try {
            return current.queryWithLock(key, index);
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public void add(SlotMapOwner owner, Slot newSlot) {
        final long stamp = lock.writeLock();
        try {
            current.addWithLock(owner, newSlot);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public void addWithLock(SlotMapOwner owner, Slot newSlot) {
        super.add(owner, newSlot);
    }

    @Override
    public <S extends Slot> S computeWithLock(
            SlotMapOwner owner, Object key, int index, SlotComputer<S> compute) {
        return super.compute(owner, key, index, compute);
    }

    @Override
    public boolean isEmptyWithLock() {
        return super.isEmpty();
    }

    @Override
    public Slot modifyWithLock(SlotMapOwner owner, Object key, int index, int attributes) {
        return super.modify(owner, key, index, attributes);
    }

    @Override
    public Slot queryWithLock(Object key, int index) {
        return super.query(key, index);
    }

    @Override
    public int sizeWithLock() {
        return super.size();
    }

    @Override
    public long readLock() {
        return lock.readLock();
    }

    @Override
    public void unlockRead(long stamp) {
        lock.unlock(stamp);
    }

    @Override
    protected void promoteMap(SlotMapOwner owner, Slot newSlot) {
        // We can use `setMap` here as this promotion can only be done
        // by the lock holder and the operation will be being done on
        // the "current" map.
        var newMap = new ThreadSafeHashSlotMap(lock, this, newSlot);
        owner.setMap(newMap);
        current = newMap;
    }
}
