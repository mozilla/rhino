package org.mozilla.javascript;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import org.mozilla.javascript.ScriptableObject.DescriptorInfo;

/**
 * This is a specialization of property access using some lambda functions designed for properties
 * on built in objects that may be created extremely frequently. It is designed to expose a field on
 * a native Java object as a property via static methods that can get and set this value. Custom
 * operations are also supported for the setting of property attributes (as the class may need to
 * check these during internal operations) and redefinition of the property via a descriptor (as
 * array length has unusual behaviour in this respect).
 *
 * <p>It will generate a plain data descriptor when a property descriptor is produced from it, as
 * the properties we might want to internalise do not necessarily have get and set functions.
 *
 * <p>Holding the `owner` on this object and passing it to the various accessor functions was a
 * design choice to reduce object creation under the current slot implementation, and to facilitate
 * the separation of slots and descriptors in future.
 *
 * <p>The owner must be held specifically as the current slot APIs do not pass in the owner of the
 * map from which a slot was fetched. We store it in the slot's value field as this is not used for
 * any real value storage on a built in slot.
 */
public class BuiltInSlot<T extends ScriptableObject> extends CompactSlot<Scriptable, T> {
    @Serial private static final long serialVersionUID = 8728562620206845355L;

    public interface Getter<T extends ScriptableObject> extends Serializable {
        Object apply(T builtIn, Scriptable start);
    }

    public interface Setter<T extends ScriptableObject> extends Serializable {
        boolean apply(T builtIn, Object value, Scriptable owner, Scriptable start, boolean isThrow);
    }

    public interface AttributeSetter<U extends ScriptableObject> extends Serializable {
        void apply(U builtIn, int attributes);
    }

    public interface PropDescriptionSetter<T extends ScriptableObject> extends Serializable {
        boolean apply(
                T builtIn,
                BuiltInSlot<T> current,
                Object id,
                DescriptorInfo info,
                boolean checkValid,
                Object key,
                int index);
    }

    public static class Descriptor<T extends ScriptableObject>
            extends CompactSlot.Descriptor<BuiltInSlot<T>, Scriptable, T> implements Serializable {
        @Serial private static final long serialVersionUID = 8728562620206845355L;

        private final Object name;
        private int indexOrHash;
        private final Getter<T> getter;
        private final Setter<T> setter;
        private final AttributeSetter<T> attrUpdater;
        private final PropDescriptionSetter<T> propDescSetter;

        public Descriptor(Object name, Getter<T> getter) {
            this(
                    name,
                    0,
                    getter,
                    BuiltInSlot::defaultSetter,
                    BuiltInSlot::defaultAttrSetter,
                    BuiltInSlot::defaultPropDescSetter);
        }

        public Descriptor(Object name, Getter<T> getter, Setter<T> setter) {
            this(
                    name,
                    0,
                    getter,
                    setter,
                    BuiltInSlot::defaultAttrSetter,
                    BuiltInSlot::defaultPropDescSetter);
        }

        public Descriptor(
                Object name, Getter<T> getter, Setter<T> setter, AttributeSetter<T> attrUpdater) {
            this(name, 0, getter, setter, attrUpdater, BuiltInSlot::defaultPropDescSetter);
        }

        public Descriptor(
                Object name,
                Getter<T> getter,
                Setter<T> setter,
                AttributeSetter<T> attrUpdater,
                PropDescriptionSetter<T> propDescSetter) {
            this(name, 0, getter, setter, attrUpdater, propDescSetter);
        }

        public Descriptor(
                Object name,
                int indexOrHash,
                Getter<T> getter,
                Setter<T> setter,
                AttributeSetter<T> attrUpdater,
                PropDescriptionSetter<T> propDescSetter) {
            this.name = name;
            this.indexOrHash = name == null ? indexOrHash : name.hashCode();
            this.getter = getter;
            this.setter = setter;
            this.attrUpdater = attrUpdater;
            this.propDescSetter = propDescSetter;
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            if (name != null) {
                indexOrHash = name.hashCode();
            }
        }

        @Override
        public BuiltInSlot<T> createSlot(T owner, int attr) {
            return new BuiltInSlot<>(this, attr, owner);
        }
    }

    private final Descriptor<T> descriptor;

    BuiltInSlot(Object name, int index, int attr, T builtIn, Getter<T> getter) {
        this(
                name,
                index,
                attr,
                builtIn,
                getter,
                BuiltInSlot::defaultSetter,
                BuiltInSlot::defaultAttrSetter,
                BuiltInSlot::defaultPropDescSetter);
    }

    BuiltInSlot(Object name, int index, int attr, T builtIn, Getter<T> getter, Setter<T> setter) {
        this(
                name,
                index,
                attr,
                builtIn,
                getter,
                setter,
                BuiltInSlot::defaultAttrSetter,
                BuiltInSlot::defaultPropDescSetter);
    }

    BuiltInSlot(
            Object name,
            int index,
            int attr,
            T builtIn,
            Getter<T> getter,
            Setter<T> setter,
            AttributeSetter<T> attrUpdater) {
        this(
                name,
                index,
                attr,
                builtIn,
                getter,
                setter,
                attrUpdater,
                BuiltInSlot::defaultPropDescSetter);
    }

    BuiltInSlot(
            Object name,
            int index,
            int attr,
            T builtIn,
            Getter<T> getter,
            Setter<T> setter,
            AttributeSetter<T> attrUpdater,
            PropDescriptionSetter<T> propDescSetter) {
        this(
                new Descriptor<>(name, index, getter, setter, attrUpdater, propDescSetter),
                attr,
                builtIn);
    }

    BuiltInSlot(Descriptor<T> descriptor, int attr, T builtIn) {
        super(attr);
        this.value = builtIn;
        this.descriptor = descriptor;
    }

    BuiltInSlot(BuiltInSlot<T> slot) {
        super(slot);
        this.descriptor = slot.descriptor;
    }

    @Override
    Slot<Scriptable> copySlot() {
        var res = new BuiltInSlot<T>(this);
        res.next = null;
        res.orderedNext = null;
        return res;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object getValue(Scriptable start) {
        return descriptor.getter.apply(((T) this.value), start);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean setValue(Object value, Scriptable owner, Scriptable start, boolean isThrow) {
        if ((getAttributes() & ScriptableObject.READONLY) != 0) {
            if (isThrow) {
                throw ScriptRuntime.typeErrorById("msg.modify.readonly", getName());
            }
            return true;
        }
        if (owner == start) {
            return descriptor.setter.apply(((T) this.value), value, owner, start, isThrow);
        }
        return false;
    }

    /* When setting a property descriptor we need to set the property
    _without_ the normal checks on readonly and similar. */
    @SuppressWarnings("unchecked")
    public void setValueFromDescriptor(
            Object value, Scriptable owner, Scriptable start, boolean isThrow) {
        descriptor.setter.apply(((T) this.value), value, owner, start, isThrow);
    }

    @Override
    @SuppressWarnings("unchecked")
    void setAttributes(int value) {
        descriptor.attrUpdater.apply(((T) this.value), value);
        super.setAttributes(value);
    }

    @Override
    @SuppressWarnings("unchecked")
    DescriptorInfo getPropertyDescriptor(Context cx, Scriptable start) {
        return ScriptableObject.buildDataDescriptor(getValue((T) this.value), getAttributes());
    }

    @SuppressWarnings("unchecked")
    boolean applyNewDescriptor(
            Object id, DescriptorInfo info, boolean checkValid, Object key, int index) {
        return descriptor.propDescSetter.apply(
                ((T) this.value), this, id, info, checkValid, key, index);
    }

    private static <T extends ScriptableObject> boolean defaultSetter(
            T builtIn, Object value, Scriptable owner, Scriptable start, boolean isThrow) {
        return true;
    }

    private static <T extends ScriptableObject> void defaultAttrSetter(T builtIn, int attributes) {
        // Do nothing.
    }

    private static <T extends ScriptableObject> boolean defaultPropDescSetter(
            T builtIn,
            BuiltInSlot<T> current,
            Object id,
            DescriptorInfo info,
            boolean checkValid,
            Object key,
            int index) {
        try (var map = builtIn.startCompoundOp(true)) {
            return ScriptableObject.defineOrdinaryProperty(
                    ScriptableObject::setSlotValue, builtIn, map, id, info, checkValid, key, index);
        }
    }

    @Override
    protected void throwNoSetterException(Scriptable start, Object newValue) {
        Context cx = Context.getContext();
        if (cx.isStrictMode()
                ||
                // Based on TC39 ES3.1 Draft of 9-Feb-2009, 8.12.4, step 2,
                // we should throw a TypeError in this case.
                cx.hasFeature(Context.FEATURE_STRICT_MODE)) {

            String prop = "";
            if (descriptor.name != null) {
                prop = "[" + ((Scriptable) start).getClassName() + "]." + descriptor.name;
            }
            throw ScriptRuntime.typeErrorById(
                    "msg.set.prop.no.setter", prop, Context.toString(newValue));
        }
    }

    @Override
    public boolean keyMatches(Object key, int indexOrHash) {
        return indexOrHash == this.descriptor.indexOrHash
                && Objects.equals(this.descriptor.name, key);
    }

    @Override
    public Object getKey() {
        return descriptor.name != null ? descriptor.name : descriptor.indexOrHash;
    }

    @Override
    public Object getName() {
        return descriptor.name;
    }

    @Override
    public int getIndexOrHash() {
        return descriptor.indexOrHash;
    }
}
