package org.mozilla.javascript;

import java.util.Iterator;

/**
 * This class represents a compound operation performed on a thread safe slot map. As each compound
 * operation creates a new instance the class itself does not need to consider access by multiple
 * threads. This means that the instance fields do not need to be volatile as we are only
 * considering access from this thread.
 */
class ThreadSafeCompoundOperationMap<T extends PropHolder<T>> extends CompoundOperationMap<T> {
    private boolean closed = false;
    private long lockStamp = 0;

    public ThreadSafeCompoundOperationMap(
            SlotMapOwner<T> owner, LockAwareSlotMap<T> map, long lockStamp) {
        super(owner);
        this.map = map;
        this.lockStamp = lockStamp;
    }

    @Override
    public void add(SlotMapOwner<T> owner, Slot<T> newSlot) {
        ((LockAwareSlotMap<T>) map).addWithLock(owner, newSlot);
        touched = true;
    }

    @Override
    public <S extends Slot<T>> S compute(
            SlotMapOwner<T> owner, Object key, int index, SlotComputer<S, T> compute) {
        updateMap(true);
        S res = ((LockAwareSlotMap<T>) map).computeWithLock(owner, this, key, index, compute);
        touched = true;
        return res;
    }

    @Override
    public <S extends Slot<T>> S compute(
            SlotMapOwner<T> owner,
            CompoundOperationMap<T> compoundOp,
            Object key,
            int index,
            SlotComputer<S, T> compute) {
        assert (compoundOp == this);
        updateMap(true);
        S res = ((LockAwareSlotMap<T>) map).computeWithLock(owner, this, key, index, compute);
        touched = true;
        return res;
    }

    @Override
    public boolean isEmpty() {
        updateMap(false);
        return ((LockAwareSlotMap<T>) map).isEmptyWithLock();
    }

    @Override
    public Slot<T> modify(SlotMapOwner<T> owner, Object key, int index, int attributes) {
        updateMap(true);
        Slot<T> res = ((LockAwareSlotMap<T>) map).modifyWithLock(owner, key, index, attributes);
        touched = true;
        return res;
    }

    @Override
    public Slot<T> query(Object key, int index) {
        updateMap(false);
        return ((LockAwareSlotMap<T>) map).queryWithLock(key, index);
    }

    @Override
    public int size() {
        updateMap(false);
        return ((LockAwareSlotMap<T>) map).sizeWithLock();
    }

    @Override
    public Iterator<Slot<T>> iterator() {
        updateMap(false);
        return new Iter<>(map.iterator());
    }

    @Override
    public void close() {
        if (!closed) {
            ((LockAwareSlotMap<T>) owner.getMap()).releaseLock(lockStamp);
            closed = true;
        }
    }

    private static class Iter<T extends PropHolder<T>> implements Iterator<Slot<T>> {
        private final Iterator<Slot<T>> mapIterator;

        private Iter(Iterator<Slot<T>> mapIterator) {
            this.mapIterator = mapIterator;
        }

        @Override
        public boolean hasNext() {
            return mapIterator.hasNext();
        }

        @Override
        public Slot<T> next() {
            return mapIterator.next();
        }
    }
}
