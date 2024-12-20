package org.mozilla.javascript;

import java.util.concurrent.locks.StampedLock;

@SuppressWarnings("AndroidJdkLibsChecker")
class ThreadSafeHashSlotMap extends HashSlotMap implements LockAwareSlotMap {

    private final StampedLock lock;

    public ThreadSafeHashSlotMap() {
        super();
        lock = new StampedLock();
    }

    public ThreadSafeHashSlotMap(int capacity) {
        super(capacity);
        lock = new StampedLock();
    }

    public ThreadSafeHashSlotMap(StampedLock lock, SlotMap oldMap) {
        super(oldMap.dirtySize());
        this.lock = lock;
        for (Slot n : oldMap) {
            addWithLock(null, n.copySlot());
        }
    }

    public ThreadSafeHashSlotMap(StampedLock lock, SlotMap oldMap, Slot newSlot) {
        super(oldMap.size() + 1);
        this.lock = lock;
        for (Slot n : oldMap) {
            addWithLock(null, n.copySlot());
        }
        addWithLock(null, newSlot);
    }

    @Override
    public int size() {
        long stamp = lock.tryOptimisticRead();
        int s = super.size();
        if (lock.validate(stamp)) {
            return s;
        }

        stamp = lock.readLock();
        try {
            return super.size();
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public int dirtySize() {
        assert lock.isReadLocked();
        return super.size();
    }

    @Override
    public boolean isEmpty() {
        long stamp = lock.tryOptimisticRead();
        boolean e = super.isEmpty();
        if (lock.validate(stamp)) {
            return e;
        }

        stamp = lock.readLock();
        try {
            return super.isEmpty();
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public Slot modify(SlotMapOwner owner, Object key, int index, int attributes) {
        final long stamp = lock.writeLock();
        try {
            return super.modify(owner, key, index, attributes);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public <S extends Slot> S compute(
            SlotMapOwner owner, Object key, int index, SlotComputer<S> c) {
        final long stamp = lock.writeLock();
        try {
            return super.compute(owner, key, index, c);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public Slot query(Object key, int index) {
        long stamp = lock.tryOptimisticRead();
        Slot s = super.query(key, index);
        if (lock.validate(stamp)) {
            return s;
        }

        stamp = lock.readLock();
        try {
            return super.query(key, index);
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public void add(SlotMapOwner owner, Slot newSlot) {
        final long stamp = lock.writeLock();
        try {
            super.add(owner, newSlot);
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
}
