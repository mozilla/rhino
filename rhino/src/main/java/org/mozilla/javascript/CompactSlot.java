package org.mozilla.javascript;

public abstract class CompactSlot<T extends PropHolder<T>, O extends ScriptableObject>
        extends Slot<T> {

    public abstract static class Descriptor<
            T extends CompactSlot<U, O>, U extends PropHolder<U>, O extends ScriptableObject> {

        public abstract T createSlot(O owner, int attr);
    }

    CompactSlot(int attr) {
        super(attr);
    }

    CompactSlot(CompactSlot<T, O> oldSlot) {
        super(oldSlot);
    }
}
