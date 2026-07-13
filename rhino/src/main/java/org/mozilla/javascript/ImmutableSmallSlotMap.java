package org.mozilla.javascript;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class ImmutableSmallSlotMap<T extends PropHolder<T>> implements SlotMap<T> {

    protected final ASlot<T> slot0;
    protected final ASlot<T> slot1;
    protected final ASlot<T> slot2;
    protected final ASlot<T> slot3;

    public ImmutableSmallSlotMap(ASlot<T> slot0, ASlot<T> slot1, ASlot<T> slot2, ASlot<T> slot3) {
        this.slot0 = slot0;
        if (slot0 != null) slot0.orderedNext = slot1;
        this.slot1 = slot1;
        if (slot1 != null) slot1.orderedNext = slot2;
        this.slot2 = slot2;
        if (slot2 != null) slot2.orderedNext = slot3;
        this.slot3 = slot3;
    }

    protected SlotMap<T> newMap(SlotMapOwner<T> owner, int expansion) {
        var newMap = new EmbeddedSlotMap<T>(size() + expansion);
        owner.setMap(newMap);
        if (slot0 != null) newMap.add(owner, slot0);
        if (slot1 != null) newMap.add(owner, slot1);
        if (slot2 != null) newMap.add(owner, slot2);
        if (slot3 != null) newMap.add(owner, slot3);
        return newMap;
    }

    @Override
    public void add(SlotMapOwner<T> owner, ASlot<T> newSlot) {
        if (owner == null) {
            throw new IllegalStateException();
        } else {
            newMap(owner, 1).add(owner, newSlot);
        }
    }

    @Override
    public <S extends ASlot<T>> S compute(
            SlotMapOwner<T> owner,
            CompoundOperationMap<T> mutableMap,
            Object key,
            int index,
            SlotComputer<S, T> compute) {
        return newMap(owner, 0).compute(owner, mutableMap, key, index, compute);
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public ASlot<T> modify(SlotMapOwner<T> owner, Object key, int index, int attributes) {
        var slot = query(key, index);
        if (slot != null) return slot;
        var newSlot = new StandardSlot<T>(key, index, attributes);
        add(owner, newSlot);
        return newSlot;
    }

    @Override
    public ASlot<T> query(Object key, int index) {
        final int indexOrHash = (key != null ? key.hashCode() : index);

        if (slot0 != null && slot0.keyMatches(key, indexOrHash)) {
            return slot0;
        } else if (slot1 != null && slot1.keyMatches(key, indexOrHash)) {
            return slot1;
        } else if (slot2 != null && slot2.keyMatches(key, indexOrHash)) {
            return slot2;
        } else if (slot3 != null && slot3.keyMatches(key, indexOrHash)) {
            return slot3;
        } else {
            return null;
        }
    }

    @Override
    public int size() {
        if (slot3 != null) return 4;
        if (slot2 != null) return 3;
        if (slot1 != null) return 2;
        if (slot0 != null) return 1;
        return 0;
    }

    @Override
    public Iterator<ASlot<T>> iterator() {
        return new Iter<>(slot0);
    }

    private static final class Iter<T extends PropHolder<T>> implements Iterator<ASlot<T>> {
        private ASlot<T> next;

        Iter(ASlot<T> slot) {
            next = slot;
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public ASlot<T> next() {
            var ret = next;
            if (ret == null) {
                throw new NoSuchElementException();
            }
            next = next.orderedNext;
            return ret;
        }
    }
}
