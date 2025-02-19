package org.mozilla.javascript;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

public class OrderedSlotMap implements SlotMap {
    // The hash table
    private Slot[] slots;
    // The slots, in insertion order
    private Slot[] orderedSlots;
    // Number of slots in the ordered slots array -- this is not the same as
    // "count" because it includes deleted items
    private int orderedCount;
    // Number of times that an item was deleted
    private int deleteCount;
    // Number of items in the map
    private int count;

    // initial slot array size, must be a power of 2
    private static final int INITIAL_SLOT_SIZE = 4;
    // when we reach this number of deleted objects, we will
    // promote ourselves to a different type of slot map
    private static final int MAX_DELETED_SLOTS = 10;

    // This represents a deleted slot
    private static final Slot DELETED_SENTINEL = new Slot(null, 0, 0);

    private final class Iter implements Iterator<Slot> {
        private int pos;

        @Override
        public boolean hasNext() {
            skipDeleted();
            return pos < orderedCount;
        }

        @Override
        public Slot next() {
            skipDeleted();
            if (pos >= orderedCount) {
                throw new NoSuchElementException();
            }
            return orderedSlots[pos++];
        }

        private void skipDeleted() {
            while ((pos < orderedCount) && (orderedSlots[pos] == DELETED_SENTINEL)) {
                pos++;
            }
        }
    }

    public OrderedSlotMap() {}

    public OrderedSlotMap(int capacity) {
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
        return new OrderedSlotMap.Iter();
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
                // Switch to new map and insert new slot
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
        SlotMap newMap;
        if (newSlot == null) {
            newMap = new HashSlotMap(this);
        } else {
            newMap = new HashSlotMap(this, newSlot);
        }
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
                return computeExisting(owner, key, index, c, slot, prev, slotIndex);
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
            SlotMapOwner owner,
            Object key,
            int index,
            SlotComputer<S> c,
            Slot slot,
            Slot prev,
            int slotIndex) {
        // Modify or remove existing slot
        S newSlot = c.compute(key, index, slot);
        if (newSlot == null) {
            // Need to delete this slot actually
            removeSlot(owner, slot, prev, slotIndex, key);
        } else if (!Objects.equals(slot, newSlot)) {
            // Replace slot in hash table
            if (prev == slot) {
                slots[slotIndex] = newSlot;
            } else {
                prev.next = newSlot;
            }
            newSlot.next = slot.next;
            // Replace slot in ordered list
            orderedSlots[slot.orderedPos] = newSlot;
            newSlot.orderedPos = slot.orderedPos;
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
        if (orderedSlots == null) {
            orderedSlots = new Slot[INITIAL_SLOT_SIZE];
        }
        if (orderedCount == orderedSlots.length) {
            Slot[] newOrderedSlots = new Slot[orderedSlots.length * 2];
            System.arraycopy(orderedSlots, 0, newOrderedSlots, 0, orderedCount);
            orderedSlots = newOrderedSlots;
        }
        assert orderedCount < Short.MAX_VALUE;
        newSlot.orderedPos = (short)orderedCount;
        orderedSlots[orderedCount++] = newSlot;
        addKnownAbsentSlot(slots, newSlot);
    }

    private void removeSlot(SlotMapOwner owner, Slot slot, Slot prev, int ix, Object key) {
        count--;
        // remove slot from hash table
        if (prev == slot) {
            slots[ix] = slot.next;
        } else {
            prev.next = slot.next;
        }
        // Replace with sentinel so that we don't iterate forever
        orderedSlots[slot.orderedPos] = DELETED_SENTINEL;
        if (++deleteCount > MAX_DELETED_SLOTS) {
            // Replace this map with one that handles deletes better
            promoteMap(owner, null);
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
