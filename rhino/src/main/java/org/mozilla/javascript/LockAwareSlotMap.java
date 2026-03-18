package org.mozilla.javascript;

/**
 * Extends {@link SlotMap} with a set of "WithLock" methods. which will not acquire the lock. These
 * should only be used internally by implementation, or by other {@link SlotMap}s which share the
 * same lock.
 */
interface LockAwareSlotMap<T extends PropHolder<T>> extends SlotMap<T> {
    /** The equivalent of {@link SlotMap#size()}. */
    int sizeWithLock();

    /** The equivalent of {@link SlotMap#isEmpty()}. */
    boolean isEmptyWithLock();

    /** The equivalent of {@link SlotMap#modify(SlotMapOwner, Object, int, int)}. */
    Slot<T> modifyWithLock(SlotMapOwner<T> owner, Object key, int index, int attributes);

    /** The equivalent of {@link SlotMap#query(Object, int)}. */
    Slot<T> queryWithLock(Object key, int index);

    /**
     * The equivalent of {@link SlotMap#compute(SlotMapOwner, Object, int,
     * org.mozilla.javascript.SlotMap.SlotComputer)}.
     */
    <S extends Slot<T>> S computeWithLock(
            SlotMapOwner<T> owner,
            CompoundOperationMap<T> compoundOp,
            Object key,
            int index,
            SlotComputer<S, T> compute);

    /** The equivalent of {@link SlotMap#add(SlotMapOwner, Slot)}. */
    void addWithLock(SlotMapOwner<T> owner, Slot<T> newSlot);

    long getReadLock();

    long getWriteLock();

    void releaseLock(long lock);

    @Override
    default CompoundOperationMap<T> startCompoundOp(SlotMapOwner<T> owner, boolean forWriting) {
        long stamp = forWriting ? getWriteLock() : getReadLock();
        return new ThreadSafeCompoundOperationMap<T>(owner, this, stamp);
    }
}
