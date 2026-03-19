package org.mozilla.javascript;

import java.util.Iterator;

/**
 * This class represents a compound operation performed on a non-thread safe map. As such it does
 * the minimum work possible and does not enforce locking, instead it simply records whether the map
 * has been mutated as part of the operation to allow the underlying map to delegate operations as
 * required.
 */
public class CompoundOperationMap<T extends PropHolder<T>> implements SlotMap<T>, AutoCloseable {
    protected final SlotMapOwner<T> owner;
    protected SlotMap<T> map;
    boolean touched = false;

    public CompoundOperationMap(SlotMapOwner<T> owner) {
        this.owner = owner;
        this.map = owner.getMap();
    }

    protected void updateMap(boolean resetTouched) {
        if (touched) {
            map = owner.getMap();
            touched = resetTouched ? false : touched;
        }
    }

    public boolean isTouched() {
        return touched;
    }

    @Override
    public void add(SlotMapOwner<T> owner, Slot<T> newSlot) {
        map.add(owner, newSlot);
        touched = true;
    }

    @Override
    public <S extends Slot<T>> S compute(
            SlotMapOwner<T> owner, Object key, int index, SlotComputer<S, T> compute) {
        updateMap(true);
        var res = map.compute(owner, this, key, index, compute);
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
        var res = map.compute(owner, this, key, index, compute);
        touched = true;
        return res;
    }

    @Override
    public int dirtySize() {
        updateMap(false);
        return map.dirtySize();
    }

    @Override
    public boolean isEmpty() {
        updateMap(false);
        return map.isEmpty();
    }

    @Override
    public Slot<T> modify(SlotMapOwner<T> owner, Object key, int index, int attributes) {
        updateMap(true);
        var res = map.modify(owner, key, index, attributes);
        touched = true;
        return res;
    }

    @Override
    public Slot<T> query(Object key, int index) {
        updateMap(false);
        return map.query(key, index);
    }

    @Override
    public int size() {
        updateMap(false);
        return map.size();
    }

    @Override
    public Iterator<Slot<T>> iterator() {
        updateMap(false);
        return map.iterator();
    }

    @Override
    public void close() {
        // This version doesn't need to do anything on clean up.
    }
}
