package org.mozilla.javascript;

import java.io.Serializable;

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
public class BuiltInSlot<T extends ScriptableObject> extends Slot {

    public interface Getter<U extends ScriptableObject> extends Serializable {
        Object apply(U builtIn, Scriptable start);
    }

    public interface Setter<U extends ScriptableObject> extends Serializable {
        boolean apply(U builtIn, Object value, Scriptable owner, Scriptable start, boolean isThrow);
    }

    public interface AttributeSetter<U extends ScriptableObject> extends Serializable {
        void apply(U builtIn, int attributes);
    }

    public interface PropDescriptionSetter<U extends ScriptableObject> extends Serializable {
        boolean apply(
                U builtIn,
                BuiltInSlot<U> current,
                Object id,
                ScriptableObject desc,
                boolean checkValid,
                Object key,
                int index);
    }

    private final Getter<T> getter;
    private final Setter<T> setter;
    private final AttributeSetter<T> attrUpdater;
    private final PropDescriptionSetter<T> propDescSetter;

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
        super(name, index, attr);
        this.value = builtIn;
        this.getter = getter;
        this.setter = setter;
        this.attrUpdater = attrUpdater;
        this.propDescSetter = propDescSetter;
    }

    BuiltInSlot(BuiltInSlot<T> slot) {
        super(slot);
        this.getter = slot.getter;
        this.setter = slot.setter;
        this.attrUpdater = slot.attrUpdater;
        this.propDescSetter = slot.propDescSetter;
    }

    @Override
    Slot copySlot() {
        var res = new BuiltInSlot<T>(this);
        res.next = null;
        res.orderedNext = null;
        return res;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object getValue(Scriptable start) {
        return getter.apply(((T) this.value), start);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean setValue(Object value, Scriptable owner, Scriptable start, boolean isThrow) {
        return setter.apply(((T) this.value), value, owner, start, isThrow);
    }

    @Override
    @SuppressWarnings("unchecked")
    void setAttributes(int value) {
        attrUpdater.apply(((T) this.value), value);
        super.setAttributes(value);
    }

    @Override
    @SuppressWarnings("unchecked")
    ScriptableObject getPropertyDescriptor(Context cx, Scriptable scope) {
        return ScriptableObject.buildDataDescriptor(
                scope, getValue((T) this.value), getAttributes());
    }

    @SuppressWarnings("unchecked")
    boolean applyNewDescriptor(
            Object id, ScriptableObject desc, boolean checkValid, Object key, int index) {
        return propDescSetter.apply(((T) this.value), this, id, desc, checkValid, key, index);
    }

    private static <T extends ScriptableObject> boolean defaultPropDescSetter(
            T builtIn,
            BuiltInSlot<T> current,
            Object id,
            ScriptableObject desc,
            boolean checkValid,
            Object key,
            int index) {
        return ScriptableObject.defineOrdinaryProperty(builtIn, id, desc, checkValid, key, index);
    }
}
