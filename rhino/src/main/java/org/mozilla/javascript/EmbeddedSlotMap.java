/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/*
 * This class implements the SlotMap interface using an embedded hash table. This hash table
 * has the minimum overhead needed to get the job done. In particular, it embeds the Slot
 * directly into the hash table rather than creating an intermediate object, which seems
 * to have a measurable performance benefit.
 */

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

public class EmbeddedSlotMap implements SlotMap {

    private Slot[] slots;

    // gateways into the definition-order linked list of slots
    private Slot firstAdded;
    private Slot lastAdded;

    private int count;
    private boolean hasIndex = false;

    // initial slot array size, must be a power of 2
    private static final int INITIAL_SLOT_SIZE = 4;

    private static final class Iter implements Iterator<Slot> {
        private Slot next;

        Iter(Slot slot) {
            next = slot;
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public Slot next() {
            Slot ret = next;
            if (ret == null) {
                throw new NoSuchElementException();
            }
            next = next.orderedNext;
            return ret;
        }
    }

    public EmbeddedSlotMap() {}

    public EmbeddedSlotMap(int capacity) {
        int n = -1 >>> Integer.numberOfLeadingZeros(capacity - 1);
        n = (n < 0) ? 1 : n + 1;
        slots = new Slot[n];
    }

    @Override
    public int size() {
        return count;
    }

    @Override
    public boolean isEmpty() {
        return count == 0;
    }

    @Override
    public Iterator<Slot> iterator() {
        return new Iter(firstAdded);
    }

    /** Locate the slot with the given name or index. */
    @Override
    public Slot query(Object key, int index) {
        if (slots == null || (key == null && !hasIndex)) {
            return null;
        }

        int indexOrHash = (key != null ? key.hashCode() : index);
        int slotIndex = getSlotIndex(slots.length, indexOrHash);
        for (Slot slot = slots[slotIndex]; slot != null; slot = slot.next) {
            if (indexOrHash == slot.indexOrHash && Objects.equals(slot.name, key)) {
                return slot;
            }
        }
        return null;
    }

    /**
     * Locate the slot with given name or index, and create a new one if necessary.
     *
     * @param key either a String or a Symbol object that identifies the property
     * @param index index or 0 if slot holds property name.
     */
    @Override
    public Slot modify(SlotMapOwner owner, Object key, int index, int attributes) {
        final int indexOrHash = (key != null ? key.hashCode() : index);
        Slot slot;

        if (slots != null) {
            final int slotIndex = getSlotIndex(slots.length, indexOrHash);
            for (slot = slots[slotIndex]; slot != null; slot = slot.next) {
                if (indexOrHash == slot.indexOrHash && Objects.equals(slot.name, key)) {
                    break;
                }
            }
            if (slot != null) {
                return slot;
            }
        }

        Slot newSlot = new Slot(key, index, attributes);
        createNewSlot(owner, newSlot);
        return newSlot;
    }

    private void createNewSlot(SlotMapOwner owner, Slot newSlot) {
        if (count == 0 && slots == null) {
            // Always throw away old slots if any on empty insert.
            slots = new Slot[INITIAL_SLOT_SIZE];
        }

        // Check if the table is not too full before inserting.
        if (4 * (count + 1) > 3 * slots.length) {
            // table size must be a power of 2 -- always grow by x2!
            if (count > SlotMapOwner.LARGE_HASH_SIZE) {
                promoteMap(owner, newSlot);
                return;
            }
            Slot[] newSlots = new Slot[slots.length * 2];
            copyTable(slots, newSlots);
            slots = newSlots;
        }

        insertNewSlot(newSlot);
    }

    protected void promoteMap(SlotMapOwner owner, Slot newSlot) {
        var newMap = new HashSlotMap(this, newSlot);
        owner.setMap(newMap);
    }

    @Override
    public <S extends Slot> S compute(
            SlotMapOwner owner, Object key, int index, SlotComputer<S> c) {
        final int indexOrHash = (key != null ? key.hashCode() : index);

        if (slots != null) {
            Slot slot;
            final int slotIndex = getSlotIndex(slots.length, indexOrHash);
            Slot prev = slots[slotIndex];
            for (slot = prev; slot != null; slot = slot.next) {
                if (indexOrHash == slot.indexOrHash && Objects.equals(slot.name, key)) {
                    break;
                }
                prev = slot;
            }
            if (slot != null) {
                return computeExisting(key, index, c, slot, prev, slotIndex);
            }
        }
        return computeNew(owner, key, index, c);
    }

    private <S extends Slot> S computeNew(
            SlotMapOwner owner, Object key, int index, SlotComputer<S> c) {
        S newSlot = c.compute(key, index, null);
        if (newSlot != null) {
            createNewSlot(owner, newSlot);
        }
        return newSlot;
    }

    private <S extends Slot> S computeExisting(
            Object key, int index, SlotComputer<S> c, Slot slot, Slot prev, int slotIndex) {
        // Modify or remove existing slot
        S newSlot = c.compute(key, index, slot);
        if (newSlot == null) {
            // Need to delete this slot actually
            removeSlot(slot, prev, slotIndex, key);
        } else if (!Objects.equals(slot, newSlot)) {
            // Replace slot in hash table
            if (prev == slot) {
                slots[slotIndex] = newSlot;
            } else {
                prev.next = newSlot;
            }
            newSlot.next = slot.next;
            // Replace new slot in linked list, keeping same order
            if (slot == firstAdded) {
                firstAdded = newSlot;
            } else {
                Slot ps = firstAdded;
                while ((ps != null) && (ps.orderedNext != slot)) {
                    ps = ps.orderedNext;
                }
                if (ps != null) {
                    ps.orderedNext = newSlot;
                }
            }
            newSlot.orderedNext = slot.orderedNext;
            if (slot == lastAdded) {
                lastAdded = newSlot;
            }
        }
        return newSlot;
    }

    @Override
    public void add(SlotMapOwner owner, Slot newSlot) {
        if (slots == null) {
            slots = new Slot[INITIAL_SLOT_SIZE];
        }
        createNewSlot(owner, newSlot);
    }

    private void insertNewSlot(Slot newSlot) {
        ++count;
        // add new slot to linked list
        if (lastAdded != null) {
            lastAdded.orderedNext = newSlot;
        }
        if (firstAdded == null) {
            firstAdded = newSlot;
        }
        lastAdded = newSlot;
        if (newSlot.name == null) hasIndex = true;
        addKnownAbsentSlot(slots, newSlot);
    }

    private void removeSlot(Slot slot, Slot prev, int ix, Object key) {
        count--;
        // remove slot from hash table
        if (prev == slot) {
            slots[ix] = slot.next;
        } else {
            prev.next = slot.next;
        }

        // remove from ordered list. Previously this was done lazily in
        // getIds() but delete is an infrequent operation so O(n)
        // should be ok

        // ordered list always uses the actual slot
        if (slot == firstAdded) {
            prev = null;
            firstAdded = slot.orderedNext;
        } else {
            prev = firstAdded;
            while (prev.orderedNext != slot) {
                prev = prev.orderedNext;
            }
            prev.orderedNext = slot.orderedNext;
        }
        if (slot == lastAdded) {
            lastAdded = prev;
        }
    }

    private static void copyTable(Slot[] oldSlots, Slot[] newSlots) {
        for (Slot slot : oldSlots) {
            while (slot != null) {
                Slot nextSlot = slot.next;
                addKnownAbsentSlot(newSlots, slot);
                slot = nextSlot;
            }
        }
    }

    /**
     * Add slot with keys that are known to absent from the table. This is an optimization to use
     * when inserting into empty table, after table growth or during deserialization.
     */
    private static void addKnownAbsentSlot(Slot[] addSlots, Slot slot) {
        final int insertPos = getSlotIndex(addSlots.length, slot.indexOrHash);
        slot.next = addSlots[insertPos];
        addSlots[insertPos] = slot;
    }

    private static int getSlotIndex(int tableSize, int indexOrHash) {
        // This is a Java trick to efficiently "mod" the hash code by the table size.
        // It only works if the table size is a power of 2.
        // The performance improvement is measurable.
        return indexOrHash & (tableSize - 1);
    }
}
