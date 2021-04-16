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
        if (slots == null) {
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
    public Slot modify(Object key, int index, int attributes) {
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

        // A new slot has to be inserted.
        return createSlot(key, indexOrHash, attributes);
    }

    private Slot createSlot(Object key, int indexOrHash, int attributes) {
        if (count == 0) {
            // Always throw away old slots if any on empty insert.
            slots = new Slot[INITIAL_SLOT_SIZE];
        }

        // Check if the table is not too full before inserting.
        if (4 * (count + 1) > 3 * slots.length) {
            // table size must be a power of 2 -- always grow by x2!
            Slot[] newSlots = new Slot[slots.length * 2];
            copyTable(slots, newSlots);
            slots = newSlots;
        }

        Slot newSlot = new Slot(key, indexOrHash, attributes);
        insertNewSlot(newSlot);
        return newSlot;
    }

    @Override
    public void replace(Slot oldSlot, Slot newSlot) {
        final int insertPos = getSlotIndex(slots.length, oldSlot.indexOrHash);
        Slot prev = slots[insertPos];
        Slot tmpSlot = prev;
        // Find original slot and previous one in hash table
        while (tmpSlot != null) {
            if (tmpSlot == oldSlot) {
                break;
            }
            prev = tmpSlot;
            tmpSlot = tmpSlot.next;
        }

        // It's an error to call this when the slot isn't already there
        assert (tmpSlot == oldSlot);

        // add new slot to hash table
        if (prev == oldSlot) {
            slots[insertPos] = newSlot;
        } else {
            prev.next = newSlot;
        }
        newSlot.next = oldSlot.next;

        // Replace new slot in linked list, keeping same order
        if (oldSlot == firstAdded) {
            firstAdded = newSlot;
        } else {
            Slot ps = firstAdded;
            while ((ps != null) && (ps.orderedNext != oldSlot)) {
                ps = ps.orderedNext;
            }
            if (ps != null) {
                ps.orderedNext = newSlot;
            }
        }
        newSlot.orderedNext = oldSlot.orderedNext;
        if (oldSlot == lastAdded) {
            lastAdded = newSlot;
        }
    }

    @Override
    public void add(Slot newSlot) {
        if (slots == null) {
            slots = new Slot[INITIAL_SLOT_SIZE];
        }
        insertNewSlot(newSlot);
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
        // add new slot to hash table, return it
        addKnownAbsentSlot(slots, newSlot);
    }

    @Override
    public void remove(Object key, int index) {
        int indexOrHash = (key != null ? key.hashCode() : index);

        if (count != 0) {
            final int slotIndex = getSlotIndex(slots.length, indexOrHash);
            Slot prev = slots[slotIndex];
            Slot slot = prev;
            while (slot != null) {
                if (slot.indexOrHash == indexOrHash && Objects.equals(slot.name, key)) {
                    break;
                }
                prev = slot;
                slot = slot.next;
            }
            if (slot != null) {
                // non-configurable
                if ((slot.getAttributes() & ScriptableObject.PERMANENT) != 0) {
                    Context cx = Context.getContext();
                    if (cx.isStrictMode()) {
                        throw ScriptRuntime.typeErrorById(
                                "msg.delete.property.with.configurable.false", key);
                    }
                    return;
                }
                count--;
                // remove slot from hash table
                if (prev == slot) {
                    slots[slotIndex] = slot.next;
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
        }
    }

    private static void copyTable(Slot[] oldSlots, Slot[] newSlots) {
        for (Slot slot : oldSlots) {
            while (slot != null) {
                Slot nextSlot = slot.next;
                slot.next = null;
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
        Slot old = addSlots[insertPos];
        addSlots[insertPos] = slot;
        slot.next = old;
    }

    private static int getSlotIndex(int tableSize, int indexOrHash) {
        // This is a Java trick to efficiently "mod" the hash code by the table size.
        // It only works if the table size is a power of 2.
        // The performance improvement is measurable.
        return indexOrHash & (tableSize - 1);
    }
}
