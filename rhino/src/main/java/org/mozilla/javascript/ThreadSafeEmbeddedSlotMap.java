package org.mozilla.javascript;

import java.util.concurrent.locks.StampedLock;

@SuppressWarnings("AndroidJdkLibsChecker")
class ThreadSafeEmbeddedSlotMap<T extends PropHolder<T>> extends EmbeddedSlotMap<T>
        implements LockAwareSlotMap<T> {

    private final StampedLock lock = new StampedLock();
    private volatile LockAwareSlotMap<T> current = this;

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
        assert lock.isReadLocked() || lock.isWriteLocked();
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
    public Slot<T> modify(SlotMapOwner<T> owner, Object key, int index, int attributes) {
        final long stamp = lock.writeLock();
        try {
            return current.modifyWithLock(owner, key, index, attributes);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public <S extends Slot<T>> S compute(
            SlotMapOwner<T> owner,
            CompoundOperationMap<T> mutableMap,
            Object key,
            int index,
            SlotComputer<S, T> c) {
        return current.computeWithLock(owner, mutableMap, key, index, c);
    }

    @Override
    public Slot<T> query(Object key, int index) {
        long stamp = lock.tryOptimisticRead();
        Slot<T> s = current.queryWithLock(key, index);
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
    public void add(SlotMapOwner<T> owner, Slot<T> newSlot) {
        final long stamp = lock.writeLock();
        try {
            current.addWithLock(owner, newSlot);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public void addWithLock(SlotMapOwner<T> owner, Slot<T> newSlot) {
        super.add(owner, newSlot);
    }

    @Override
    public <S extends Slot<T>> S computeWithLock(
            SlotMapOwner<T> owner,
            CompoundOperationMap<T> mutableMap,
            Object key,
            int index,
            SlotComputer<S, T> compute) {
        return super.compute(owner, mutableMap, key, index, compute);
    }

    @Override
    public boolean isEmptyWithLock() {
        return super.isEmpty();
    }

    @Override
    public Slot<T> modifyWithLock(SlotMapOwner<T> owner, Object key, int index, int attributes) {
        return super.modify(owner, key, index, attributes);
    }

    @Override
    public Slot<T> queryWithLock(Object key, int index) {
        return super.query(key, index);
    }

    @Override
    public int sizeWithLock() {
        return super.size();
    }

    @Override
    public long getReadLock() {
        return lock.readLock();
    }

    @Override
    public long getWriteLock() {
        return lock.writeLock();
    }

    @Override
    public void releaseLock(long stamp) {
        lock.unlock(stamp);
    }

    @Override
    protected void promoteMap(SlotMapOwner<T> owner, Slot<T> newSlot) {
        // We can use `setMap` here as this promotion can only be done
        // by the lock holder and the operation will be being done on
        // the "current" map.
        var newMap = new ThreadSafeHashSlotMap<>(lock, this, newSlot);
        owner.setMap(newMap);
        current = newMap;
    }
}
