package org.mozilla.javascript;

/**
 * Extends {@link SlotMap} with a set of "WithLock" methods. which will not acquire the lock. These
 * should only be used internally by implementation, or by other {@link SlotMap}s which share the
 * same lock.
 */
interface LockAwareSlotMap extends SlotMap {
    /** The equivalent of {@link SlotMap#size()}. */
    int sizeWithLock();

    /** The equivalent of {@link SlotMap#isEmpty()}. */
    boolean isEmptyWithLock();

    /** The equivalent of {@link SlotMap#modify(SlotMapOwner, Object, int, int)}. */
    Slot modifyWithLock(SlotMapOwner owner, Object key, int index, int attributes);

    /** The equivalent of {@link SlotMap#query(Object, int)}. */
    Slot queryWithLock(Object key, int index);

    /**
     * The equivalent of {@link SlotMap#compute(SlotMapOwner, Object, int,
     * org.mozilla.javascript.SlotMap.SlotComputer)}.
     */
    <S extends Slot> S computeWithLock(
            SlotMapOwner owner, Object key, int index, SlotComputer<S> compute);

    /** The equivalent of {@link SlotMap#add(SlotMapOwner, Slot)}. */
    void addWithLock(SlotMapOwner owner, Slot newSlot);
}
