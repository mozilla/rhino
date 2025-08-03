package org.mozilla.javascript;

import java.util.Iterator;

/**
 * This class represents a compound operation performed on a thread safe slot map. As each compound
 * operation creates a new instance the class itself does not need to consider access by multiple
 * threads. This means that the instance fields do not need to be volatile as we are only
 * considering access from this thread.
 */
class ThreadSafeCompoundOperationMap extends CompoundOperationMap {
    private boolean closed = false;
    private long lockStamp = 0;

    public ThreadSafeCompoundOperationMap(
            SlotMapOwner owner, LockAwareSlotMap map, long lockStamp) {
        super(owner);
        this.map = map;
        this.lockStamp = lockStamp;
    }

    @Override
    public void add(SlotMapOwner owner, Slot newSlot) {
        ((LockAwareSlotMap) map).addWithLock(owner, newSlot);
        touched = true;
    }

    @Override
    public <S extends Slot> S compute(
            SlotMapOwner owner, Object key, int index, SlotComputer<S> compute) {
        updateMap(true);
        S res = ((LockAwareSlotMap) map).computeWithLock(owner, this, key, index, compute);
        touched = true;
        return res;
    }

    @Override
    public <S extends Slot> S compute(
            SlotMapOwner owner,
            CompoundOperationMap mutableMap,
            Object key,
            int index,
            SlotComputer<S> compute) {
        updateMap(true);
        S res = ((LockAwareSlotMap) map).computeWithLock(owner, this, key, index, compute);
        touched = true;
        return res;
    }

    @Override
    public boolean isEmpty() {
        updateMap(false);
        return ((LockAwareSlotMap) map).isEmptyWithLock();
    }

    @Override
    public Slot modify(SlotMapOwner owner, Object key, int index, int attributes) {
        updateMap(true);
        Slot res = ((LockAwareSlotMap) map).modifyWithLock(owner, key, index, attributes);
        touched = true;
        return res;
    }

    @Override
    public Slot query(Object key, int index) {
        updateMap(false);
        return ((LockAwareSlotMap) map).queryWithLock(key, index);
    }

    @Override
    public int size() {
        updateMap(false);
        return ((LockAwareSlotMap) map).sizeWithLock();
    }

    @Override
    public Iterator<Slot> iterator() {
        updateMap(false);
        return new Iter(map.iterator());
    }

    @Override
    public void close() {
        if (!closed) {
            ((LockAwareSlotMap) owner.getMap()).releaseLock(lockStamp);
            closed = true;
        }
    }

    private static class Iter implements Iterator<Slot> {
        private final Iterator<Slot> mapIterator;

        private Iter(Iterator<Slot> mapIterator) {
            this.mapIterator = mapIterator;
        }

        @Override
        public boolean hasNext() {
            return mapIterator.hasNext();
        }

        @Override
        public Slot next() {
            return mapIterator.next();
        }
    }
}
