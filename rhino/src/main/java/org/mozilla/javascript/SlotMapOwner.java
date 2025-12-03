package org.mozilla.javascript;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

public abstract class SlotMapOwner<T extends PropHolder<T>> implements PropHolder<T> {
    private static final long serialVersionUID = 1L;

    /**
     * Maximum size of an {@link EmbeddedSlotMap} before it is promoted to a {@link HashSlotMap}.
     * The value must be 3/4 of a power of two, because the embedded slot map's slot size must be a
     * power of two, and it is resized when it is 3/4 full.
     */
    static final int LARGE_HASH_SIZE = (1 << 10) + (1 << 9);

    static final SlotMap<?> EMPTY_SLOT_MAP = new EmptySlotMap<>();

    static final SlotMap<?> THREAD_SAFE_EMPTY_SLOT_MAP = new ThreadSafeEmptySlotMap<>();

    @SuppressWarnings("AndroidJdkLibsChecker")
    // https://developer.android.com/reference/java/lang/invoke/VarHandle added in API level 33
    // Note: Due presence of this class, dexing of rhino will not be possible for APIs < 26
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

        static <T extends PropHolder<T>> SlotMap<T> checkAndReplaceMap(
                SlotMapOwner<T> owner, SlotMap<T> oldMap, SlotMap<T> newMap) {
            return (SlotMap<T>) SLOT_MAP.compareAndExchange(owner, oldMap, newMap);
        }
    }

    private static class EmptySlotMap<T extends PropHolder<T>> implements SlotMap<T> {

        @Override
        public Iterator<Slot<T>> iterator() {
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
        public Slot<T> modify(SlotMapOwner<T> owner, Object key, int index, int attributes) {
            var newSlot = new Slot<T>(key, index, attributes);
            var map = new SingleEntrySlotMap<T>(newSlot);
            owner.setMap(map);
            return newSlot;
        }

        @Override
        public Slot<T> query(Object key, int index) {
            return null;
        }

        @Override
        public void add(SlotMapOwner<T> owner, Slot<T> newSlot) {
            if (newSlot != null) {
                var map = new SingleEntrySlotMap<T>(newSlot);
                owner.setMap(map);
            }
        }

        @Override
        public <S extends Slot<T>> S compute(
                SlotMapOwner<T> owner,
                CompoundOperationMap<T> compoundOp,
                Object key,
                int index,
                SlotComputer<S, T> c) {
            var newSlot = c.compute(key, index, null, compoundOp, owner);
            if (newSlot != null) {
                if (!compoundOp.isTouched()) {
                    var map = new SingleEntrySlotMap<T>(newSlot);
                    owner.setMap(map);
                } else {
                    // The map has been touched so delegate the add (can't do
                    // a compute because that might be recursive).
                    compoundOp.add(owner, newSlot);
                }
            }
            return newSlot;
        }
    }

    private static final class ThreadSafeEmptySlotMap<T extends PropHolder<T>>
            extends EmptySlotMap<T> {

        @Override
        public Slot<T> modify(SlotMapOwner<T> owner, Object key, int index, int attributes) {
            var newSlot = new Slot<T>(key, index, attributes);
            var currentMap = replaceMapAndAddSlot(owner, newSlot);
            if (currentMap != this) {
                return currentMap.modify(owner, key, index, attributes);
            }
            return newSlot;
        }

        @Override
        public void add(SlotMapOwner<T> owner, Slot<T> newSlot) {
            if (newSlot != null) {
                var currentMap = replaceMapAndAddSlot(owner, newSlot);
                if (currentMap != this) {
                    currentMap.add(owner, newSlot);
                }
            }
        }

        @Override
        public <S extends Slot<T>> S compute(
                SlotMapOwner<T> owner,
                CompoundOperationMap<T> compoundOp,
                Object key,
                int index,
                SlotComputer<S, T> c) {
            var newSlot = c.compute(key, index, null, compoundOp, owner);
            if (newSlot != null) {
                var currentMap = replaceMapAndAddSlot(owner, newSlot);
                if (currentMap != this) {
                    return currentMap.compute(owner, key, index, c);
                }
            }
            return newSlot;
        }

        private SlotMap<T> replaceMapAndAddSlot(SlotMapOwner<T> owner, Slot<T> newSlot) {
            var map = new ThreadSafeSingleEntrySlotMap<T>(newSlot);
            return ThreadedAccess.checkAndReplaceMap(owner, this, map);
        }

        @Override
        public CompoundOperationMap<T> startCompoundOp(SlotMapOwner<T> owner, boolean forWriting) {
            var map = new ThreadSafeEmbeddedSlotMap<T>();
            return ThreadedAccess.checkAndReplaceMap(owner, this, map)
                    .startCompoundOp(owner, forWriting);
        }
    }

    private static final class Iter<T extends PropHolder<T>> implements Iterator<Slot<T>> {
        private Slot<T> next;

        Iter(Slot<T> slot) {
            next = slot;
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public Slot<T> next() {
            Slot<T> ret = next;
            if (ret == null) {
                throw new NoSuchElementException();
            }
            next = next.orderedNext;
            return ret;
        }
    }

    static class SingleEntrySlotMap<T extends PropHolder<T>> implements SlotMap<T> {

        SingleEntrySlotMap(Slot<T> slot) {
            assert (slot != null);
            this.slot = slot;
        }

        protected final Slot<T> slot;

        @Override
        public Iterator<Slot<T>> iterator() {
            return new Iter<T>(slot);
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
        public Slot<T> modify(SlotMapOwner<T> owner, Object key, int index, int attributes) {
            final int indexOrHash = (key != null ? key.hashCode() : index);

            if (indexOrHash == slot.indexOrHash && Objects.equals(slot.name, key)) {
                return slot;
            }
            Slot<T> newSlot = new Slot<T>(key, index, attributes);
            add(owner, newSlot);
            return newSlot;
        }

        @Override
        public Slot<T> query(Object key, int index) {
            final int indexOrHash = (key != null ? key.hashCode() : index);

            if (indexOrHash == slot.indexOrHash && Objects.equals(slot.name, key)) {
                return slot;
            }
            return null;
        }

        @Override
        public void add(SlotMapOwner<T> owner, Slot<T> newSlot) {
            if (owner == null) {
                throw new IllegalStateException();
            } else {
                var newMap = new EmbeddedSlotMap<T>();
                owner.setMap(newMap);
                newMap.add(owner, slot);
                newMap.add(owner, newSlot);
            }
        }

        @Override
        public <S extends Slot<T>> S compute(
                SlotMapOwner<T> owner,
                CompoundOperationMap<T> compoundOp,
                Object key,
                int index,
                SlotComputer<S, T> c) {
            var newMap = new EmbeddedSlotMap<T>();
            owner.setMap(newMap);
            newMap.add(owner, slot);
            return newMap.compute(owner, compoundOp, key, index, c);
        }
    }

    static final class ThreadSafeSingleEntrySlotMap<T extends PropHolder<T>>
            extends SingleEntrySlotMap<T> {

        ThreadSafeSingleEntrySlotMap(Slot<T> slot) {
            super(slot);
        }

        @Override
        public void add(SlotMapOwner<T> owner, Slot<T> newSlot) {
            if (owner == null) {
                throw new IllegalStateException();
            } else {
                var newMap = new ThreadSafeEmbeddedSlotMap<T>(2);
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
        public <S extends Slot<T>> S compute(
                SlotMapOwner<T> owner,
                CompoundOperationMap<T> compoundOp,
                Object key,
                int index,
                SlotComputer<S, T> c) {
            var currentMap = checkAndReplaceMap(owner);
            return currentMap.compute(owner, compoundOp, key, index, c);
        }

        @Override
        public CompoundOperationMap<T> startCompoundOp(SlotMapOwner<T> owner, boolean forWriting) {
            return checkAndReplaceMap(owner).startCompoundOp(owner, forWriting);
        }

        private SlotMap<T> checkAndReplaceMap(SlotMapOwner<T> owner) {
            var newMap = new ThreadSafeEmbeddedSlotMap<T>(2);
            newMap.add(null, slot);
            var currentMap = ThreadedAccess.checkAndReplaceMap(owner, this, newMap);
            return currentMap;
        }
    }

    /**
     * This holds all the slots. It may or may not be thread-safe, and may expand itself to a
     * different data structure depending on the size of the object.
     */
    private SlotMap<T> slotMap;

    protected SlotMapOwner() {
        slotMap = createSlotMap(0);
    }

    protected SlotMapOwner(int capacity) {
        slotMap = createSlotMap(capacity);
    }

    protected SlotMapOwner(SlotMap<T> map) {
        slotMap = map;
    }

    protected static <T extends PropHolder<T>> SlotMap<T> createSlotMap(int initialSize) {
        Context cx = Context.getCurrentContext();
        if ((cx != null) && cx.hasFeature(Context.FEATURE_THREAD_SAFE_OBJECTS)) {
            if (initialSize == 0) {
                @SuppressWarnings("unchecked")
                var res = (SlotMap<T>) THREAD_SAFE_EMPTY_SLOT_MAP;
                return res;
            } else if (initialSize > LARGE_HASH_SIZE) {
                return new ThreadSafeHashSlotMap<>(initialSize);
            } else {
                return new ThreadSafeEmbeddedSlotMap<>();
            }
        } else if (initialSize == 0) {
            @SuppressWarnings("unchecked")
            var res = (SlotMap<T>) EMPTY_SLOT_MAP;
            return res;
        } else if (initialSize > LARGE_HASH_SIZE) {
            return new HashSlotMap<>();
        } else {
            return new EmbeddedSlotMap<>();
        }
    }

    SlotMap<T> getMap() {
        return slotMap;
    }

    final void setMap(SlotMap<T> newMap) {
        slotMap = newMap;
    }

    /**
     * Returns an {@link AutoCloseable} map which can be used for compound operations. If the
     * underlying map is thread safe then this will perform any locking required to ensure that no
     * other operation will mutate the map during this. It is vital to either close the returned
     * {@link CompoundOperationMap} manually or to claim this in a try-with-resources block which
     * will do that for you. For example:
     *
     * <blockquote>
     *
     * <pre>
     *     try (var op = obj.startCompoundOp(true)) {
     *         var slot = op.compute(obj, "myKey", 0, this::complexOperation);
     *     }
     * </pre>
     *
     * </blockquote>
     *
     * <p>While the compound operation is in progress other threads will not be able to manipulate
     * the map and will block until the compound operation has been closed. It is important to note
     * that this compound operation only covers the map being manipulated, great care must be taken
     * if any other maps are being accessed as it is extremely easy to end up in a deadlock
     * situation.
     *
     * @param forWriting Indicates whether this compound operation should retrieve a read lock or a
     *     write lock. This needs to be claimed at the start of the operation because there is no
     *     good way for one thread to upgrade a lock from read to write without the possibility of
     *     deadlocks or other semantic issues.
     * @return the {@link CompoundOperationMap} which can be used for the operation.
     */
    final CompoundOperationMap<T> startCompoundOp(boolean forWriting) {
        return slotMap.startCompoundOp(this, forWriting);
    }
}
