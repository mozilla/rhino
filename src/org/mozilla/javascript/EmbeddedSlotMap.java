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

    public EmbeddedSlotMap(int initialSize)
    {
        slots = new ScriptableObject.Slot[initialSize];
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
        final ScriptableObject.Slot[] localSlots = slots;
        if (localSlots == null) {
            return null;
        }

        final int indexOrHash = (key != null ? key.hashCode() : index);
        final int slotIndex = getSlotIndex(localSlots.length, indexOrHash);
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
        // Check the hashtable without using synchronization.
        final ScriptableObject.Slot[] localSlots = slots;
        if (localSlots == null && accessType == QUERY) {
            return null;
        }

        final int indexOrHash = (key != null ? key.hashCode() : index);
        if (localSlots != null) {
            ScriptableObject.Slot slot;
            final int slotIndex = getSlotIndex(localSlots.length, indexOrHash);
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
                    if (slot != null)
                        return slot;
                    break;
                case MODIFY_GETTER_SETTER:
                    slot = ScriptableObject.unwrapSlot(slot);
                    if (slot instanceof ScriptableObject.GetterSlot)
                        return slot;
                    break;
                case CONVERT_ACCESSOR_TO_DATA:
                    slot = ScriptableObject.unwrapSlot(slot);
                    if ( !(slot instanceof ScriptableObject.GetterSlot) )
                        return slot;
                    break;
            }
        }

        // A new slot has to be inserted or the old has to be replaced
        // by GetterSlot. Time to synchronize.
        return createSlot(key, indexOrHash, accessType);
    }

    private synchronized ScriptableObject.Slot createSlot(Object key, int indexOrHash,
        ScriptableObject.SlotAccess accessType) {
        if (count == 0) {
            // Always throw away old slots if any on empty insert.
            slots = new ScriptableObject.Slot[INITIAL_SLOT_SIZE];
        } else {
            final int tableSize = slots.length;
            final int insertPos = getSlotIndex(tableSize, indexOrHash);
            ScriptableObject.Slot prev = slots[insertPos];
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
                // A slot with same name/index already exists. This means that
                // a slot is being redefined from a value to a getter slot or
                // vice versa, or it could be a race in application code.
                // Check if we need to replace the slot depending on the
                // accessType flag and return the appropriate slot instance.

                ScriptableObject.Slot inner = ScriptableObject.unwrapSlot(slot);
                ScriptableObject.Slot newSlot;

                if (accessType == MODIFY_GETTER_SETTER
                        && !(inner instanceof ScriptableObject.GetterSlot)) {
                    newSlot = new ScriptableObject.GetterSlot(key, indexOrHash, inner.getAttributes());
                } else if (accessType == CONVERT_ACCESSOR_TO_DATA
                        && (inner instanceof ScriptableObject.GetterSlot)) {
                    newSlot = new ScriptableObject.Slot(key, indexOrHash, inner.getAttributes());
                } else if (accessType == MODIFY_CONST) {
                    return null;
                } else {
                    return inner;
                }

                newSlot.value = inner.value;
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
                // other housekeeping
                slot.markDeleted();
                return newSlot;
            } else {
                // Check if the table is not too full before inserting.
                if (4 * (count + 1) > 3 * slots.length) {
                    // table size must be a power of 2, always grow by x2
                    ScriptableObject.Slot[] newSlots = new ScriptableObject.Slot[slots.length * 2];
                    copyTable(slots, newSlots, count);
                    slots = newSlots;
                }
            }
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
    public synchronized void addSlot(ScriptableObject.Slot newSlot) {
        insertNewSlot(newSlot);
    }

    private void insertNewSlot(ScriptableObject.Slot newSlot) {
        ++count;
        // add new slot to linked list
        if (lastAdded != null)
            lastAdded.orderedNext = newSlot;
        if (firstAdded == null)
            firstAdded = newSlot;
        lastAdded = newSlot;
        // add new slot to hash table, return it
        addKnownAbsentSlot(slots, newSlot);
    }

    @Override
    public synchronized void remove(Object key, int index) {
        int indexOrHash = (key != null ? key.hashCode() : index);

        ScriptableObject.Slot[] slotsLocalRef = slots;
        if (count != 0) {
            final int tableSize = slotsLocalRef.length;
            final int slotIndex = getSlotIndex(tableSize, indexOrHash);
            ScriptableObject.Slot prev = slotsLocalRef[slotIndex];
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
                    slotsLocalRef[slotIndex] = slot.next;
                } else {
                    prev.next = slot.next;
                }

                // remove from ordered list. Previously this was done lazily in
                // getIds() but delete is an infrequent operation so O(n)
                // should be ok

                // ordered list always uses the actual slot
                ScriptableObject.Slot deleted = ScriptableObject.unwrapSlot(slot);
                if (deleted == firstAdded) {
                    prev = null;
                    firstAdded = deleted.orderedNext;
                } else {
                    prev = firstAdded;
                    while (prev.orderedNext != deleted) {
                        prev = prev.orderedNext;
                    }
                    prev.orderedNext = deleted.orderedNext;
                }
                if (deleted == lastAdded) {
                    lastAdded = prev;
                }

                // Mark the slot as removed.
                slot.markDeleted();
            }
        }
    }

    // Must be inside synchronized (this)
    private void copyTable(ScriptableObject.Slot[] oldSlots, ScriptableObject.Slot[] newSlots, int count)
    {
        for (ScriptableObject.Slot slot : oldSlots) {
            while (slot != null) {
                // If slot has next chain in old table use a new
                // RelinkedSlot wrapper to keep old table valid.
                // This is necessary because we use unlocked access in multi-threaded cases.
                ScriptableObject.Slot insSlot = slot.next == null ? slot : new ScriptableObject.RelinkedSlot(slot);
                ScriptableObject.Slot nextSlot = slot.next;
                addKnownAbsentSlot(newSlots, insSlot);
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
