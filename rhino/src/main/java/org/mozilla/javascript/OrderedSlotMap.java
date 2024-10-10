package org.mozilla.javascript;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

public class OrderedSlotMap implements SlotMap {
    private Slot[] slots;
    private int slotCount;
    private Entry[] map;
    private int count;

    // initial slot array size, must be a power of 2
    private static final int INITIAL_SLOT_SIZE = 4;
    // Sentinel for deletions
    private static final Slot DELETED_SLOT = new Slot(null, 0, 0);

    @Override
    public int size() {
        return count;
    }

    @Override
    public boolean isEmpty() {
        return count == 0;
    }

    @Override
    public Slot query(Object key, int index) {
        if (map == null) {
            return null;
        }

        int indexOrHash = (key != null ? key.hashCode() : index);
        int slotIndex = getSlotIndex(map.length, indexOrHash);
        for (Entry e = map[slotIndex]; e != null; e = e.next) {
            if (indexOrHash == e.indexOrHash && Objects.equals(e.key, key)) {
                return e.slot;
            }
        }
        return null;
    }

    @Override
    public Slot modify(Object key, int index, int attributes) {
        final int indexOrHash = (key != null ? key.hashCode() : index);
        Entry e;

        if (map != null) {
            int slotIndex = getSlotIndex(map.length, indexOrHash);
            for (e = map[slotIndex]; e != null; e = e.next) {
                if (indexOrHash == e.indexOrHash && Objects.equals(e.key, key)) {
                    break;
                }
            }
            if (e != null) {
                return e.slot;
            }
        }

        // A new slot has to be inserted.
        Slot newSlot = new Slot(key, index, attributes);
        createNewSlot(newSlot);
        return newSlot;
    }

    @Override
    public <S extends Slot> S compute(Object key, int index, SlotComputer<S> c) {
        final int indexOrHash = (key != null ? key.hashCode() : index);

        if (map != null) {
            Entry e;
            int slotIndex = getSlotIndex(map.length, indexOrHash);
            Entry prev = map[slotIndex];
            Slot slot = null;
            for (e = prev; e != null; e = e.next) {
                if (indexOrHash == e.indexOrHash && Objects.equals(e.key, key)) {
                    slot = e.slot;
                    break;
                }
                prev = e;
            }
            if (e != null) {
                assert(slot != null);
                // Modify or remove existing slot
                S newSlot = c.compute(key, index, slot);
                if (newSlot == null) {
                    // Need to delete this slot actually
                    removeSlot(e, prev, slotIndex, key);
                } else if (!Objects.equals(slot, newSlot)) {
                    // Replace slot in the list
                    e.slot = newSlot;
                    slots[e.slotIx] = newSlot;
                }
                return newSlot;
            }
        }

        // If we get here, we know we are potentially adding a new slot
        S newSlot = c.compute(key, index, null);
        if (newSlot != null) {
            createNewSlot(newSlot);
        }
        return newSlot;
    }

    @Override
    public void add(Slot newSlot) {
        if (map == null) {
            map = new Entry[INITIAL_SLOT_SIZE];
        }
        insertNewSlot(newSlot);
    }

    @Override
    public Iterator<Slot> iterator() {
        return new Itr();
    }

    private void createNewSlot(Slot newSlot) {
        if (count == 0) {
            // Always throw away old slots if any on empty insert.
            map = new Entry[INITIAL_SLOT_SIZE];
        }

        // Check if the table is not too full before inserting.
        if (4 * (count + 1) > 3 * map.length) {
            // table size must be a power of 2 -- always grow by x2!
            Entry[] newMap = new Entry[map.length * 2];
            copyTable(map, newMap);
            map = newMap;
        }

        insertNewSlot(newSlot);
    }

    private void insertNewSlot(Slot newSlot) {
        ++count;
        if (slots == null) {
            slots = new Slot[INITIAL_SLOT_SIZE];
        } else if (slotCount == slots.length) {
            Slot[] newSlots = new Slot[slots.length * 2];
            System.arraycopy(slots, 0, newSlots, 0, slots.length);
            slots = newSlots;
        }
        slots[slotCount] = newSlot;
        Entry e = new Entry();
        e.slotIx = slotCount;
        e.slot = newSlot;
        e.indexOrHash = newSlot.indexOrHash;
        e.key = newSlot.name;
        slotCount++;
        addKnownAbsentSlot(map, e);
    }

    private void removeSlot(Entry e, Entry prev, int ix, Object key) {
        count--;
        // remove slot from hash table
        if (prev == e) {
            map[ix] = e.next;
        } else {
            prev.next = e.next;
        }

        // Mark missing in slot list -- will add GC later
        slots[e.slotIx] = DELETED_SLOT;
    }

    private static void copyTable(Entry[] oldMap, Entry[] newMap) {
        for (Entry e : oldMap) {
            while (e != null) {
                Entry nextEntry = e.next;
                addKnownAbsentSlot(newMap, e);
                e = nextEntry;
            }
        }
    }

    /**
     * Add slot with keys that are known to absent from the table. This is an optimization to use
     * when inserting into empty table, after table growth or during deserialization.
     */
    private static void addKnownAbsentSlot(Entry[] map, Entry e) {
        int insertPos = getSlotIndex(map.length, e.indexOrHash);
        e.next = map[insertPos];
        map[insertPos] = e;
    }

    private static int getSlotIndex(int tableSize, int indexOrHash) {
        // This is a Java trick to efficiently "mod" the hash code by the table size.
        // It only works if the table size is a power of 2.
        // The performance improvement is measurable.
        return indexOrHash & (tableSize - 1);
    }

    private final class Itr implements Iterator<Slot> {
        private int pos;

        @Override
        public boolean hasNext() {
            while (pos < slotCount && slots[pos] == DELETED_SLOT) {
                pos++;
            }
            return pos < slotCount;
        }

        @Override
        public Slot next() {
            while (pos < slotCount && slots[pos] == DELETED_SLOT) {
                pos++;
            }
            if (pos < slotCount) {
                return slots[pos];
            } else {
                throw new NoSuchElementException();
            }
        }
    }

    private static final class Entry {
        int indexOrHash;
        int slotIx;
        Object key;
        Entry next;
        Slot slot;
    }
}
