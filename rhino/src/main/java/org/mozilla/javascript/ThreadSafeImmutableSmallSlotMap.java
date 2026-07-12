package org.mozilla.javascript;

import org.mozilla.javascript.SlotMapOwner.ThreadedAccess;

public class ThreadSafeImmutableSmallSlotMap<T extends PropHolder<T>>
        extends ImmutableSmallSlotMap<T> {

    ThreadSafeImmutableSmallSlotMap(
            ASlot<T> slot0, ASlot<T> slot1, ASlot<T> slot2, ASlot<T> slot3) {
        super(slot0, slot1, slot2, slot3);
    }

    protected SlotMap<T> newMap(SlotMapOwner<T> owner, int expansion) {
        var newMap = new ThreadSafeEmbeddedSlotMap<T>(size() + expansion);
        owner.setMap(newMap);
        if (slot0 != null) newMap.add(null, slot0);
        if (slot1 != null) newMap.add(null, slot1);
        if (slot2 != null) newMap.add(null, slot2);
        if (slot3 != null) newMap.add(null, slot3);
        var currentMap = ThreadedAccess.checkAndReplaceMap(owner, this, newMap);
        return currentMap == this ? newMap : currentMap;
    }
}
