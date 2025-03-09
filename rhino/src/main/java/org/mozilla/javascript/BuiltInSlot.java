package org.mozilla.javascript;

import java.io.Serializable;

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

    private final T builtIn;
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
        this.builtIn = builtIn;
        this.getter = getter;
        this.setter = setter;
        this.attrUpdater = attrUpdater;
        this.propDescSetter = propDescSetter;
    }

    @Override
    public Object getValue(Scriptable start) {
        return getter.apply(builtIn, start);
    }

    @Override
    public boolean setValue(Object value, Scriptable owner, Scriptable start, boolean isThrow) {
        return setter.apply(builtIn, value, owner, start, isThrow);
    }

    @Override
    void setAttributes(int value) {
        attrUpdater.apply(builtIn, value);
        super.setAttributes(value);
    }

    @Override
    ScriptableObject getPropertyDescriptor(Context cx, Scriptable scope) {
        return ScriptableObject.buildDataDescriptor(scope, getValue(builtIn), getAttributes());
    }

    boolean applyNewDescriptor(
            Object id, ScriptableObject desc, boolean checkValid, Object key, int index) {
        return propDescSetter.apply(builtIn, this, id, desc, checkValid, key, index);
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
