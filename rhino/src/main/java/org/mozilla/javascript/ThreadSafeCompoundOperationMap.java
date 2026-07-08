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
    public void add(SlotMapOwner<T> owner, ASlot<T> newSlot) {
        ((LockAwareSlotMap<T>) map).addWithLock(owner, newSlot);
        touched = true;
    }

    @Override
    public <S extends ASlot<T>> S compute(
            SlotMapOwner<T> owner, Object key, int index, SlotComputer<S, T> compute) {
        updateMap(true);
        S res = ((LockAwareSlotMap<T>) map).computeWithLock(owner, this, key, index, compute);
        touched = true;
        return res;
    }

    @Override
    public <S extends ASlot<T>> S compute(
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
    public ASlot<T> modify(SlotMapOwner<T> owner, Object key, int index, int attributes) {
        updateMap(true);
        ASlot<T> res = ((LockAwareSlotMap<T>) map).modifyWithLock(owner, key, index, attributes);
        touched = true;
        return res;
    }

    @Override
    public ASlot<T> query(Object key, int index) {
        updateMap(false);
        return ((LockAwareSlotMap<T>) map).queryWithLock(key, index);
    }

    @Override
    public int size() {
        updateMap(false);
        return ((LockAwareSlotMap<T>) map).sizeWithLock();
    }

    @Override
    public Iterator<ASlot<T>> iterator() {
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

    private static class Iter<T extends PropHolder<T>> implements Iterator<ASlot<T>> {
        private final Iterator<ASlot<T>> mapIterator;

        private Iter(Iterator<ASlot<T>> mapIterator) {
            this.mapIterator = mapIterator;
        }

        @Override
        public boolean hasNext() {
            return mapIterator.hasNext();
        }

        @Override
        public ASlot<T> next() {
            return mapIterator.next();
        }
    }
}
