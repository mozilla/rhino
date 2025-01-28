package org.mozilla.javascript;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

public abstract class SlotMapOwner {
    private static final long serialVersionUID = 1L;

    static final int LARGE_HASH_SIZE = 2000;

    static final SlotMap EMPTY_SLOT_MAP = new EmptySlotMap();

    static final SlotMap THREAD_SAFE_EMPTY_SLOT_MAP = new ThreadSafeEmptySlotMap();

    @SuppressWarnings("AndroidJdkLibsChecker")
    static final class ThreadedAccess {

        private static final VarHandle SLOT_MAP = getSlotMapHandle();

        private static VarHandle getSlotMapHandle() {
            try {
                return MethodHandles.lookup()
                        .findVarHandle(SlotMapOwner.class, "slotMap", SlotMap.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new Error(e);
            }
        }

        static SlotMap checkAndReplaceMap(SlotMapOwner owner, SlotMap oldMap, SlotMap newMap) {
            return (SlotMap) SLOT_MAP.compareAndExchange(owner, oldMap, newMap);
        }
    }

    private static class EmptySlotMap implements SlotMap {

        @Override
        public Iterator<Slot> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public Slot modify(SlotMapOwner owner, Object key, int index, int attributes) {
            var newSlot = new Slot(key, index, attributes);
            var map = new SingleEntrySlotMap(newSlot);
            owner.setMap(map);
            return newSlot;
        }

        @Override
        public Slot query(Object key, int index) {
            return null;
        }

        @Override
        public void add(SlotMapOwner owner, Slot newSlot) {
            if (newSlot != null) {
                var map = new SingleEntrySlotMap(newSlot);
                owner.setMap(map);
            }
        }

        @Override
        public <S extends Slot> S compute(
                SlotMapOwner owner, Object key, int index, SlotComputer<S> c) {
            var newSlot = c.compute(key, index, null);
            if (newSlot != null) {
                var map = new SingleEntrySlotMap(newSlot);
                owner.setMap(map);
            }
            return newSlot;
        }
    }

    private static final class ThreadSafeEmptySlotMap extends EmptySlotMap {

        @Override
        public Slot modify(SlotMapOwner owner, Object key, int index, int attributes) {
            var newSlot = new Slot(key, index, attributes);
            var currentMap = replaceMapAndAddSlot(owner, newSlot);
            if (currentMap != this) {
                return currentMap.modify(owner, key, index, attributes);
            }
            return newSlot;
        }

        @Override
        public void add(SlotMapOwner owner, Slot newSlot) {
            if (newSlot != null) {
                var currentMap = replaceMapAndAddSlot(owner, newSlot);
                if (currentMap != this) {
                    currentMap.add(owner, newSlot);
                }
                return;
            }
        }

        @Override
        public <S extends Slot> S compute(
                SlotMapOwner owner, Object key, int index, SlotComputer<S> c) {
            var newSlot = c.compute(key, index, null);
            if (newSlot != null) {
                var currentMap = replaceMapAndAddSlot(owner, newSlot);
                if (currentMap != this) {
                    return currentMap.compute(owner, key, index, c);
                }
            }
            return newSlot;
        }

        private SlotMap replaceMapAndAddSlot(SlotMapOwner owner, Slot newSlot) {
            var map = new ThreadSafeSingleEntrySlotMap(newSlot);
            return ThreadedAccess.checkAndReplaceMap(owner, this, map);
        }
    }

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

    static class SingleEntrySlotMap implements SlotMap {

        SingleEntrySlotMap(Slot slot) {
            assert (slot != null);
            this.slot = slot;
        }

        protected final Slot slot;

        @Override
        public Iterator<Slot> iterator() {
            return new Iter(slot);
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Slot modify(SlotMapOwner owner, Object key, int index, int attributes) {
            final int indexOrHash = (key != null ? key.hashCode() : index);

            if (indexOrHash == slot.indexOrHash && Objects.equals(slot.name, key)) {
                return slot;
            }
            Slot newSlot = new Slot(key, index, attributes);
            add(owner, newSlot);
            return newSlot;
        }

        @Override
        public Slot query(Object key, int index) {
            final int indexOrHash = (key != null ? key.hashCode() : index);

            if (indexOrHash == slot.indexOrHash && Objects.equals(slot.name, key)) {
                return slot;
            }
            return null;
        }

        @Override
        public void add(SlotMapOwner owner, Slot newSlot) {
            if (owner == null) {
                throw new IllegalStateException();
            } else {
                var newMap = new EmbeddedSlotMap();
                owner.setMap(newMap);
                newMap.add(owner, slot);
                newMap.add(owner, newSlot);
            }
        }

        @Override
        public <S extends Slot> S compute(
                SlotMapOwner owner, Object key, int index, SlotComputer<S> c) {
            var newMap = new EmbeddedSlotMap();
            owner.setMap(newMap);
            newMap.add(owner, slot);
            return newMap.compute(owner, key, index, c);
        }
    }

    static final class ThreadSafeSingleEntrySlotMap extends SingleEntrySlotMap {

        ThreadSafeSingleEntrySlotMap(Slot slot) {
            super(slot);
        }

        @Override
        public void add(SlotMapOwner owner, Slot newSlot) {
            if (owner == null) {
                throw new IllegalStateException();
            } else {
                var newMap = new ThreadSafeEmbeddedSlotMap(2);
                newMap.add(null, slot);
                var currentMap = ThreadedAccess.checkAndReplaceMap(owner, this, newMap);
                if (currentMap == this) {
                    newMap.add(owner, newSlot);
                } else {
                    currentMap.add(owner, newSlot);
                }
            }
        }

        @Override
        public <S extends Slot> S compute(
                SlotMapOwner owner, Object key, int index, SlotComputer<S> c) {
            var newMap = new ThreadSafeEmbeddedSlotMap(2);
            newMap.add(null, slot);
            var currentMap = ThreadedAccess.checkAndReplaceMap(owner, this, newMap);
            if (currentMap == this) {
                return newMap.compute(owner, key, index, c);
            } else {
                return currentMap.compute(owner, key, index, c);
            }
        }
    }

    /**
     * This holds all the slots. It may or may not be thread-safe, and may expand itself to a
     * different data structure depending on the size of the object.
     */
    private SlotMap slotMap;

    protected SlotMapOwner() {
        slotMap = createSlotMap(0);
    }

    protected SlotMapOwner(int capacity) {
        slotMap = createSlotMap(capacity);
    }

    protected SlotMapOwner(SlotMap map) {
        slotMap = map;
    }

    protected static SlotMap createSlotMap(int initialSize) {
        Context cx = Context.getCurrentContext();
        if ((cx != null) && cx.hasFeature(Context.FEATURE_THREAD_SAFE_OBJECTS)) {
            if (initialSize == 0) {
                return THREAD_SAFE_EMPTY_SLOT_MAP;
            } else if (initialSize > LARGE_HASH_SIZE) {
                return new ThreadSafeHashSlotMap(initialSize);
            } else {
                return new ThreadSafeEmbeddedSlotMap();
            }
        } else if (initialSize == 0) {
            return EMPTY_SLOT_MAP;
        } else if (initialSize > LARGE_HASH_SIZE) {
            return new HashSlotMap();
        } else {
            return new EmbeddedSlotMap();
        }
    }

    final SlotMap getMap() {
        return slotMap;
    }

    final void setMap(SlotMap newMap) {
        slotMap = newMap;
    }
}
