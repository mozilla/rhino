package org.mozilla.javascript;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Objects;

public abstract class CompactSlot<
                T extends CompactSlot.Descriptor<T, U, O>,
                U extends PropHolder<U>,
                O extends ScriptableObject>
        extends Slot<U> {

    public abstract static class Descriptor<
                    T extends CompactSlot.Descriptor<T, U, O>,
                    U extends PropHolder<U>,
                    O extends ScriptableObject>
            implements Serializable {
        private final Object name;
        private int indexOrHash;

        protected Descriptor(Object name, int indexOrHash) {
            this.name = name;
            this.indexOrHash = name == null ? indexOrHash : name.hashCode();
        }

        public Object getName() {
            return name;
        }

        public int getIndexOrHash() {
            return indexOrHash;
        }

        public abstract CompactSlot<T, U, O> createSlot(O owner, int attr);

        public abstract Object getValue(CompactSlot<T, U, O> slot, U start);

        public abstract boolean setValue(
                CompactSlot<T, U, O> slot, Object value, U owner, U start, boolean isThrow);

        public void setAttributes(CompactSlot<T, U, O> slot, int value) {
            ScriptableObject.checkValidAttributes(value);
            slot.attributes = (short) value;
        }

        PropertyDescriptor getPropertyDescriptor(CompactSlot<T, U, O> slot, Context cx, U start) {
            return ScriptableObject.buildDataDescriptor(
                    getValue(slot, start), slot.getAttributes());
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            if (name != null) {
                indexOrHash = name.hashCode();
            }
        }
    }

    private short attributes;
    protected final T descriptor;

    CompactSlot(T descriptor, int attr) {
        super();
        this.attributes = (short) attr;
        this.descriptor = descriptor;
    }

    CompactSlot(CompactSlot<T, U, O> oldSlot) {
        super(oldSlot);
        this.attributes = oldSlot.attributes;
        this.descriptor = oldSlot.descriptor;
    }

    protected final Object getRawValue() {
        return value;
    }

    protected final void setRawValue(Object value) {
        this.value = value;
    }

    @Override
    public final Object getValue(U start) {
        return descriptor.getValue(this, start);
    }

    @Override
    public final boolean setValue(Object value, U owner, U start, boolean isThrow) {
        return descriptor.setValue(this, value, owner, start, isThrow);
    }

    @Override
    final int getAttributes() {
        return attributes;
    }

    @Override
    final void setAttributes(int value) {
        descriptor.setAttributes(this, value);
    }

    @Override
    final PropertyDescriptor getPropertyDescriptor(Context cx, U start) {
        return descriptor.getPropertyDescriptor(this, cx, start);
    }

    @Override
    public final boolean keyMatches(Object key, int indexOrHash) {
        return indexOrHash == this.descriptor.getIndexOrHash()
                && Objects.equals(this.descriptor.getName(), key);
    }

    @Override
    public final Object getKey() {
        return descriptor.getName() != null ? descriptor.getName() : descriptor.getIndexOrHash();
    }

    @Override
    public final Object getName() {
        return descriptor.getName();
    }

    @Override
    public final int getIndexOrHash() {
        return descriptor.getIndexOrHash();
    }

    @Override
    protected void throwNoSetterException(U start, Object newValue) {
        Context cx = Context.getContext();
        if (cx.isStrictMode()
                ||
                // Based on TC39 ES3.1 Draft of 9-Feb-2009, 8.12.4, step 2,
                // we should throw a TypeError in this case.
                cx.hasFeature(Context.FEATURE_STRICT_MODE)) {

            String prop = "";
            if (descriptor.getName() != null) {
                prop = "[" + ((Scriptable) start).getClassName() + "]." + descriptor.getName();
            }
            throw ScriptRuntime.typeErrorById(
                    "msg.set.prop.no.setter", prop, Context.toString(newValue));
        }
    }
}
