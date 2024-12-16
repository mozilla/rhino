package org.mozilla.javascript;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public abstract class SlotMapOwner {
    private static final long serialVersionUID = 1L;

    /**
     * This holds all the slots. It may or may not be thread-safe, and may expand itself to a
     * different data structure depending on the size of the object.
     */
    private volatile SlotMap slotMap;

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
            return new ThreadSafeSlotMapContainer(initialSize);
        } else if (initialSize == 0) {
            return SlotMapContainer.EMPTY_SLOT_MAP;
        } else if (initialSize > SlotMapContainer.LARGE_HASH_SIZE) {
            return new HashSlotMap();
        } else {
            return new EmbeddedSlotMap();
        }
    }

    @SuppressWarnings("AndroidJdkLibsChecker")
    private static final VarHandle SLOT_MAP = getSlotMapHandle();

    @SuppressWarnings("AndroidJdkLibsChecker")
    private static VarHandle getSlotMapHandle() {
        try {
            return MethodHandles.lookup()
                    .findVarHandle(ScriptableObject.class, "slotMap", SlotMap.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new Error(e);
        }
    }

    final SlotMap getMap() {
        return slotMap;
    }

    final void setMap(SlotMap newMap) {
        slotMap = newMap;
    }

    @SuppressWarnings("AndroidJdkLibsChecker")
    final SlotMap checkAndReplaceMap(SlotMap oldMap, SlotMap newMap) {
        return (SlotMap) SLOT_MAP.compareAndExchange(this, oldMap, newMap);
    }
}
