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
import static org.mozilla.javascript.ScriptableObject.SlotAccess.*;

public class EmbeddedSlotMap
    implements SlotMap {

    private ScriptableObject.Slot[] slots;

    // gateways into the definition-order linked list of slots
    private ScriptableObject.Slot firstAdded;
    private ScriptableObject.Slot lastAdded;

    private int count;

    // initial slot array size, must be a power of 2
    private static final int INITIAL_SLOT_SIZE = 4;

    private static final class Iter
        implements Iterator<ScriptableObject.Slot>
    {
        private ScriptableObject.Slot next;

        Iter(ScriptableObject.Slot slot) {
            next = slot;
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public ScriptableObject.Slot next() {
            ScriptableObject.Slot ret = next;
            next = next.orderedNext;
            return ret;
        }
    }

    public EmbeddedSlotMap()
    {
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
    public Iterator<ScriptableObject.Slot> iterator() {
        return new Iter(firstAdded);
    }

    /**
     * Locate the slot with the given name or index.
     */
    @Override
    public ScriptableObject.Slot query(Object key, int index)
    {
        if (slots == null) {
            return null;
        }

        final int indexOrHash = (key != null ? key.hashCode() : index);
        final int slotIndex = getSlotIndex(slots.length, indexOrHash);
        for (ScriptableObject.Slot slot = slots[slotIndex];
            slot != null;
            slot = slot.next) {
            Object skey = slot.name;
            if (indexOrHash == slot.indexOrHash &&
                (skey == key ||
                    (key != null && key.equals(skey)))) {
                return slot;
            }
        }
        return null;
    }

    /**
     * Locate the slot with given name or index. Depending on the accessType
     * parameter and the current slot status, a new slot may be allocated.
     *
     * @param key either a String or a Symbol object that identifies the property
     * @param index index or 0 if slot holds property name.
     */
    @Override
    public ScriptableObject.Slot get(Object key, int index, ScriptableObject.SlotAccess accessType)
    {
        if (slots == null && accessType == QUERY) {
            return null;
        }

        final int indexOrHash = (key != null ? key.hashCode() : index);
        ScriptableObject.Slot slot = null;

        if (slots != null) {
            final int slotIndex = getSlotIndex(slots.length, indexOrHash);
            for (slot = slots[slotIndex];
                 slot != null;
                 slot = slot.next) {
                Object skey = slot.name;
                if (indexOrHash == slot.indexOrHash &&
                        (skey == key ||
                                (key != null && key.equals(skey)))) {
                    break;
                }
            }
            switch (accessType) {
                case QUERY:
                    return slot;
                case MODIFY:
                case MODIFY_CONST:
                    if (slot != null) {
                        return slot;
                    }
                    break;
                case MODIFY_GETTER_SETTER:
                    if (slot instanceof ScriptableObject.GetterSlot) {
                        return slot;
                    }
                    break;
                case CONVERT_ACCESSOR_TO_DATA:
                    if ( !(slot instanceof ScriptableObject.GetterSlot) ) {
                        return slot;
                    }
                    break;
            }
        }

        // A new slot has to be inserted or the old has to be replaced
        // by GetterSlot. Time to synchronize.
        return createSlot(key, indexOrHash, accessType, slot);
    }

    private ScriptableObject.Slot createSlot(Object key, int indexOrHash,
        ScriptableObject.SlotAccess accessType, ScriptableObject.Slot existingSlot) {
        if (count == 0) {
            // Always throw away old slots if any on empty insert.
            slots = new ScriptableObject.Slot[INITIAL_SLOT_SIZE];
        } else if (existingSlot != null) {
            // Re-search the slot list because it is a singly-linked list to find
            // where to replace it with a new object if necessary
            final int insertPos = getSlotIndex(slots.length, indexOrHash);
            ScriptableObject.Slot prev = slots[insertPos];
            ScriptableObject.Slot slot = prev;
            while (slot != null) {
                if (slot.indexOrHash == indexOrHash &&
                    (slot.name == key ||
                        (key != null && key.equals(slot.name)))) {
                    break;
                }
                prev = slot;
                slot = slot.next;
            }

            if (slot != null) {
                // A slot with same name/index already exists. This means that
                // a slot is being redefined from a value to a getter slot or
                // vice versa, or it could be a race in application code.
                // Check if we need to replace the slot depending on the
                // accessType flag and return the appropriate slot instance.
                ScriptableObject.Slot newSlot;

                if (accessType == MODIFY_GETTER_SETTER
                    && !(slot instanceof ScriptableObject.GetterSlot)) {
                    newSlot = new ScriptableObject.GetterSlot(key, indexOrHash,
                        slot.getAttributes());
                } else if (accessType == CONVERT_ACCESSOR_TO_DATA
                    && (slot instanceof ScriptableObject.GetterSlot)) {
                    newSlot = new ScriptableObject.Slot(key, indexOrHash, slot.getAttributes());
                } else if (accessType == MODIFY_CONST) {
                    return null;
                } else {
                    return slot;
                }

                newSlot.value = slot.value;
                newSlot.next = slot.next;
                // add new slot to linked list
                if (lastAdded != null) {
                    lastAdded.orderedNext = newSlot;
                }
                if (firstAdded == null) {
                    firstAdded = newSlot;
                }
                lastAdded = newSlot;
                // add new slot to hash table
                if (prev == slot) {
                    slots[insertPos] = newSlot;
                } else {
                    prev.next = newSlot;
                }
                return newSlot;
            }
        }

        // If we get here, then we are going to insert a new slot
        // Check if the table is not too full before inserting.
        if (4 * (count + 1) > 3 * slots.length) {
            // table size must be a power of 2 -- always grow by x2!
            ScriptableObject.Slot[] newSlots = new ScriptableObject.Slot[slots.length * 2];
            copyTable(slots, newSlots);
            slots = newSlots;
        }

        ScriptableObject.Slot newSlot = (accessType == MODIFY_GETTER_SETTER
                ? new ScriptableObject.GetterSlot(key, indexOrHash, 0)
                : new ScriptableObject.Slot(key, indexOrHash, 0));
        if (accessType == MODIFY_CONST) {
            newSlot.setAttributes(ScriptableObject.CONST);
        }
        insertNewSlot(newSlot);
        return newSlot;
    }

    @Override
    public void addSlot(ScriptableObject.Slot newSlot) {
        if (slots == null) {
            slots = new ScriptableObject.Slot[INITIAL_SLOT_SIZE];
        }
        insertNewSlot(newSlot);
    }

    private void insertNewSlot(ScriptableObject.Slot newSlot) {
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
            ScriptableObject.Slot prev = slots[slotIndex];
            ScriptableObject.Slot slot = prev;
            while (slot != null) {
                if (slot.indexOrHash == indexOrHash &&
                        (slot.name == key ||
                                (key != null && key.equals(slot.name))))
                {
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
                        throw ScriptRuntime.typeError1("msg.delete.property.with.configurable.false", key);
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

    private void copyTable(ScriptableObject.Slot[] oldSlots, ScriptableObject.Slot[] newSlots)
    {
        for (ScriptableObject.Slot slot : oldSlots) {
            while (slot != null) {
                ScriptableObject.Slot nextSlot = slot.next;
                slot.next = null;
                addKnownAbsentSlot(newSlots, slot);
                slot = nextSlot;
            }
        }
    }

    /**
     * Add slot with keys that are known to absent from the table.
     * This is an optimization to use when inserting into empty table,
     * after table growth or during deserialization.
     */
    private void addKnownAbsentSlot(ScriptableObject.Slot[] addSlots, ScriptableObject.Slot slot)
    {
        final int insertPos = getSlotIndex(addSlots.length, slot.indexOrHash);
        ScriptableObject.Slot old = addSlots[insertPos];
        addSlots[insertPos] = slot;
        slot.next = old;
    }

    private static int getSlotIndex(int tableSize, int indexOrHash)
    {
        // This is a Java trick to efficiently "mod" the hash code by the table size.
        // It only works if the table size is a power of 2.
        // The performance improvement is measurable.
        return indexOrHash & (tableSize - 1);
    }
}
