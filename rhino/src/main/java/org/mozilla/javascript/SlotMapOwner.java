package org.mozilla.javascript;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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

    /**
     * Returns true if the named property is defined.
     *
     * @param name the name of the property
     * @param start the object in which the lookup began
     * @return true if and only if the property was found in the object
     */
    @Override
    public boolean has(String name, T start) {
        return null != getMap().query(name, 0);
    }

    /**
     * Returns true if the property index is defined.
     *
     * @param index the numeric index for the property
     * @param start the object in which the lookup began
     * @return true if and only if the property was found in the object
     */
    @Override
    public boolean has(int index, T start) {
        return null != getMap().query(null, index);
    }

    /** A version of "has" that supports symbols. */
    @Override
    public boolean has(Symbol key, T start) {
        return null != getMap().query(key, 0);
    }

    /**
     * Returns the value of the named property or NOT_FOUND.
     *
     * <p>If the property was created using defineProperty, the appropriate getter method is called.
     *
     * @param name the name of the property
     * @param start the object in which the lookup began
     * @return the value of the property (may be null), or NOT_FOUND
     */
    @Override
    public Object get(String name, T start) {
        var slot = getMap().query(name, 0);
        if (slot == null) {
            return Scriptable.NOT_FOUND;
        }
        return slot.getValue(start);
    }

    /**
     * Returns the value of the indexed property or NOT_FOUND.
     *
     * @param index the numeric index for the property
     * @param start the object in which the lookup began
     * @return the value of the property (may be null), or NOT_FOUND
     */
    @Override
    public Object get(int index, T start) {
        var slot = getMap().query(null, index);
        if (slot == null) {
            return Scriptable.NOT_FOUND;
        }
        return slot.getValue(start);
    }

    /** Another version of Get that supports Symbol keyed properties. */
    @Override
    public Object get(Symbol key, T start) {
        var slot = getMap().query(key, 0);
        if (slot == null) {
            return Scriptable.NOT_FOUND;
        }
        return slot.getValue(start);
    }

    @Override
    public void put(String name, T start, Object value) {
        Slot<T> slot;
        if (this != start) {
            slot = getMap().query(name, 0);
            if (slot == null) {
                return;
            }
        } else {

            slot = getMap().modify(this, name, 0, 0);
        }
        slot.setValue(value, getThis(), start, false);
    }

    @Override
    public void put(int index, T start, Object value) {
        Slot<T> slot;
        if (this != start) {
            slot = getMap().query(null, index);
            if (slot == null) {
                return;
            }
        } else {
            slot = getMap().modify(this, null, index, 0);
        }
        slot.setValue(value, getThis(), start, false);
    }

    @Override
    public void put(Symbol name, T start, Object value) {
        Slot<T> slot;
        if (this != start) {
            slot = getMap().query(name, 0);
            if (slot == null) {
                return;
            }
        } else {
            slot = getMap().modify(this, name, 0, 0);
        }
        slot.setValue(value, getThis(), start, false);
    }

    @Override
    public void delete(String name) {
        getMap().compute(this, name, 0, ScriptableObject::checkSlotRemoval);
    }

    @Override
    public void delete(int index) {
        getMap().compute(this, null, index, ScriptableObject::checkSlotRemoval);
    }

    @Override
    public void delete(Symbol key) {
        getMap().compute(this, key, 0, ScriptableObject::checkSlotRemoval);
    }

    /**
     * Get arbitrary application-specific value associated with this object.
     *
     * @param key key object to select particular value.
     * @see #associateValue(Object key, Object value)
     */
    public final Object getAssociatedValue(Object key) {
        Map<Object, Object> h = associatedValues;
        if (h == null) return null;
        return h.get(key);
    }

    protected void copyAssociatedValue(SlotMapOwner<T> other) {
        for (var e : other.associatedValues.entrySet()) {
            associateValue(e.getKey(), e.getValue());
        }
    }

    /**
     * Associate arbitrary application-specific value with this object. Value can only be associated
     * with the given object and key only once. The method ignores any subsequent attempts to change
     * the already associated value.
     *
     * <p>The associated values are not serialized.
     *
     * @param key key object to select particular value.
     * @param value the value to associate
     * @return the passed value if the method is called first time for the given key or old value
     *     for any subsequent calls.
     * @see #getAssociatedValue(Object key)
     */
    public final synchronized Object associateValue(Object key, Object value) {
        if (value == null) throw new IllegalArgumentException();
        Map<Object, Object> h = associatedValues;
        if (h == null) {
            h = new HashMap<>();
            associatedValues = h;
        }
        return Kit.initHash(h, key, value);
    }

    void addLazilyInitializedValue(String name, int index, LazilyLoadedCtor init, int attributes) {
        if (name != null && index != 0) throw new IllegalArgumentException(name);
        checkNotSealed(name, index);
        var lslot = getMap().compute(this, name, index, ScriptableObject::ensureLazySlot);
        lslot.setAttributes(attributes);
        lslot.value = init;
    }

    void addLazilyInitializedValue(Symbol key, int index, LazilyLoadedCtor init, int attributes) {
        if (key != null && index != 0) throw new IllegalArgumentException(key.toString());
        checkNotSealed(key, index);
        LazyLoadSlot lslot = getMap().compute(this, key, index, ScriptableObject::ensureLazySlot);
        lslot.setAttributes(attributes);
        lslot.value = init;
    }

    /**
     * Return true if this object is sealed.
     *
     * @return true if sealed, false otherwise.
     * @since 1.4R3
     * @see #sealObject()
     */
    public final boolean isSealed() {
        return isSealed;
    }

    public void sealObject() {
        isSealed = true;
    }

    protected void checkNotSealed(Object key, int index) {
        if (!isSealed()) return;

        String str = (key != null) ? key.toString() : Integer.toString(index);
        throw Context.reportRuntimeErrorById("msg.modify.sealed", str);
    }

    /**
     * @deprecated Use {@link #getAttributes(String name)}. The engine always ignored the start
     *     argument.
     */
    @Deprecated
    public final int getAttributes(String name, Scriptable start) {
        return getAttributes(name);
    }

    /**
     * @deprecated Use {@link #getAttributes(int index)}. The engine always ignored the start
     *     argument.
     */
    @Deprecated
    public final int getAttributes(int index, Scriptable start) {
        return getAttributes(index);
    }

    /**
     * @deprecated Use {@link #setAttributes(String name, int attributes)}. The engine always
     *     ignored the start argument.
     */
    @Deprecated
    public final void setAttributes(String name, Scriptable start, int attributes) {
        setAttributes(name, attributes);
    }

    /**
     * @deprecated Use {@link #setAttributes(int index, int attributes)}. The engine always ignored
     *     the start argument.
     */
    @Deprecated
    public void setAttributes(int index, Scriptable start, int attributes) {
        setAttributes(index, attributes);
    }

    /**
     * Get the attributes of a named property.
     *
     * <p>The property is specified by {@code name} as defined for {@code has}.
     *
     * @param name the identifier for the property
     * @return the bitset of attributes
     * @exception EvaluatorException if the named property is not found
     * @see org.mozilla.javascript.ScriptableObject#has(String, Scriptable)
     * @see org.mozilla.javascript.ScriptableObject#READONLY
     * @see org.mozilla.javascript.ScriptableObject#DONTENUM
     * @see org.mozilla.javascript.ScriptableObject#PERMANENT
     * @see org.mozilla.javascript.ScriptableObject#EMPTY
     */
    public int getAttributes(String name) {
        return getAttributeSlot(name, 0).getAttributes();
    }

    /**
     * Get the attributes of an indexed property.
     *
     * @param index the numeric index for the property
     * @exception EvaluatorException if the named property is not found is not found
     * @return the bitset of attributes
     * @see org.mozilla.javascript.ScriptableObject#has(String, Scriptable)
     * @see org.mozilla.javascript.ScriptableObject#READONLY
     * @see org.mozilla.javascript.ScriptableObject#DONTENUM
     * @see org.mozilla.javascript.ScriptableObject#PERMANENT
     * @see org.mozilla.javascript.ScriptableObject#EMPTY
     */
    public int getAttributes(int index) {
        return getAttributeSlot(null, index).getAttributes();
    }

    public int getAttributes(Symbol sym) {
        return getAttributeSlot(sym).getAttributes();
    }

    /**
     * Set the attributes of a named property.
     *
     * <p>The property is specified by {@code name} as defined for {@code has}.
     *
     * <p>The possible attributes are READONLY, DONTENUM, and PERMANENT. Combinations of attributes
     * are expressed by the bitwise OR of attributes. EMPTY is the state of no attributes set. Any
     * unused bits are reserved for future use.
     *
     * @param name the name of the property
     * @param attributes the bitset of attributes
     * @exception EvaluatorException if the named property is not found
     * @see org.mozilla.javascript.Scriptable#has(String, Scriptable)
     * @see org.mozilla.javascript.ScriptableObject#READONLY
     * @see org.mozilla.javascript.ScriptableObject#DONTENUM
     * @see org.mozilla.javascript.ScriptableObject#PERMANENT
     * @see org.mozilla.javascript.ScriptableObject#EMPTY
     */
    public void setAttributes(String name, int attributes) {
        checkNotSealed(name, 0);
        Slot attrSlot = getMap().modify(this, name, 0, 0);
        attrSlot.setAttributes(attributes);
    }

    /**
     * Set the attributes of an indexed property.
     *
     * @param index the numeric index for the property
     * @param attributes the bitset of attributes
     * @exception EvaluatorException if the named property is not found
     * @see org.mozilla.javascript.Scriptable#has(String, Scriptable)
     * @see org.mozilla.javascript.ScriptableObject#READONLY
     * @see org.mozilla.javascript.ScriptableObject#DONTENUM
     * @see org.mozilla.javascript.ScriptableObject#PERMANENT
     * @see org.mozilla.javascript.ScriptableObject#EMPTY
     */
    public void setAttributes(int index, int attributes) {
        checkNotSealed(null, index);
        Slot attrSlot = getMap().modify(this, null, index, 0);
        attrSlot.setAttributes(attributes);
    }

    /** Set attributes of a Symbol-keyed property. */
    public void setAttributes(Symbol key, int attributes) {
        checkNotSealed(key, 0);
        Slot attrSlot = getMap().modify(this, key, 0, 0);
        attrSlot.setAttributes(attributes);
    }

    private Slot getAttributeSlot(String name, int index) {
        Slot slot = getMap().query(name, index);
        if (slot == null) {
            String str = (name != null ? name : Integer.toString(index));
            throw Context.reportRuntimeErrorById("msg.prop.not.found", str);
        }
        return slot;
    }

    private Slot getAttributeSlot(Symbol key) {
        Slot slot = getMap().query(key, 0);
        if (slot == null) {
            throw Context.reportRuntimeErrorById("msg.prop.not.found", key);
        }
        return slot;
    }

    protected void writeMaps(ObjectOutputStream out) throws IOException {
        out.writeObject(associatedValues);
        try (var map = startCompoundOp(false)) {
            int objectsCount = map.dirtySize();
            if (objectsCount == 0) {
                out.writeInt(0);
            } else {
                out.writeInt(objectsCount);
                for (Slot slot : getMap()) {
                    out.writeObject(slot);
                }
            }
        }
    }

    protected void readMaps(ObjectInputStream in) throws IOException, ClassNotFoundException {
        @SuppressWarnings("unchecked")
        var map = (Map<Object, Object>) in.readObject();
        associatedValues = map;
        int tableSize = in.readInt();
        setMap(createSlotMap(tableSize));
        for (int i = 0; i < tableSize; i++) {
            @SuppressWarnings("unchecked")
            var slot = (Slot<T>) in.readObject();
            getMap().add(this, slot);
        }
    }

    /**
     * Define a JavaScript property.
     *
     * <p>Creates the property with an initial value and sets its attributes.
     *
     * @param propertyName the name of the property to define.
     * @param value the initial value of the property
     * @param attributes the attributes of the JavaScript property
     * @see org.mozilla.javascript.Scriptable#put(String, Scriptable, Object)
     */
    public void defineProperty(String propertyName, Object value, int attributes) {
        checkNotSealed(propertyName, 0);
        put(propertyName, getThis(), value);
        setAttributes(propertyName, attributes);
    }

    /**
     * A version of defineProperty that uses a Symbol key.
     *
     * @param key symbol of the property to define.
     * @param value the initial value of the property
     * @param attributes the attributes of the JavaScript property
     */
    public void defineProperty(Symbol key, Object value, int attributes) {
        checkNotSealed(key, 0);
        put(key, getThis(), value);
        setAttributes(key, attributes);
    }

    public abstract T getThis();

    private volatile Map<Object, Object> associatedValues;
    protected boolean isSealed = false;
}
