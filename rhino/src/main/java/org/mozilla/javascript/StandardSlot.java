package org.mozilla.javascript;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.util.Objects;

/**
 * A Slot is the base class for all properties stored in the ScriptableObject class. There are a
 * number of different types of slots. This base class represents an "ordinary" property such as a
 * primitive type or another object. Separate classes are used to represent properties that have
 * various types of getter and setter methods.
 */
public class StandardSlot<T extends PropHolder<T>> extends Slot<T> {
    @Serial private static final long serialVersionUID = -6090581677123995491L;
    private final Object name;
    private int indexOrHash;
    private short attributes;

    StandardSlot(Object name, int index, int attributes) {
        super();
        this.attributes = (short) attributes;
        this.name = name;
        this.indexOrHash = name == null ? index : name.hashCode();
    }

    @Override
    StandardSlot<T> copySlot() {
        var newSlot = new StandardSlot<T>(this);
        newSlot.next = null;
        newSlot.orderedNext = null;
        return newSlot;
    }

    /**
     * Return true if this is a base-class "Slot". Sadly too much code breaks if we try to do this
     * any other way.
     */
    @Override
    boolean isValueSlot() {
        return true;
    }

    /**
     * Return true if this is a "setter slot" which, which we need to know for some legacy support.
     */
    @Override
    boolean isSetterSlot() {
        return false;
    }

    protected StandardSlot(Slot<T> oldSlot) {
        super(oldSlot);
        attributes = (short) oldSlot.getAttributes();
        name = oldSlot.getName();
        indexOrHash = oldSlot.getIndexOrHash();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (name != null) {
            indexOrHash = name.hashCode();
        }
    }

    @Override
    public Object getValue(T start) {
        return value;
    }

    @Override
    public boolean setValue(Object value, T owner, T start, boolean isThrow) {
        if ((attributes & ScriptableObject.READONLY) != 0) {
            if ((attributes & ScriptableObject.STRICTLY_READONLY) != 0 || isThrow) {
                throw ScriptRuntime.typeErrorById("msg.modify.readonly", getName());
            }
            return true;
        }
        if (owner == start) {
            this.value = value;
            return true;
        }
        return false;
    }

    @Override
    int getAttributes() {
        return attributes;
    }

    @Override
    void setAttributes(int value) {
        ScriptableObject.checkValidAttributes(value);
        attributes = (short) value;
    }

    @Override
    protected void throwNoSetterException(T start, Object newValue) {
        Context cx = Context.getContext();
        if (cx.isStrictMode()
                ||
                // Based on TC39 ES3.1 Draft of 9-Feb-2009, 8.12.4, step 2,
                // we should throw a TypeError in this case.
                cx.hasFeature(Context.FEATURE_STRICT_MODE)) {

            String prop = "";
            if (name != null) {
                prop = "[" + ((Scriptable) start).getClassName() + "]." + name;
            }
            throw ScriptRuntime.typeErrorById(
                    "msg.set.prop.no.setter", prop, Context.toString(newValue));
        }
    }

    /**
     * Return a JavaScript function that represents the "setter". This is used by some legacy
     * functionality. Return null if there is no setter.
     */
    @Override
    Function getSetterFunction(String name, T scope) {
        return null;
    }

    /** Same for the "getter." */
    @Override
    Function getGetterFunction(String name, T scope) {
        return null;
    }

    /**
     * Compare the JavaScript function that represents the "setter" to the provided Object. We do
     * this to avoid generating a new function object when it might not be required. Specifically,
     * if we have a cached funciion object that has not yet been generated then we don't have to
     * generate it because it cannot be the same as the provided function.
     */
    @Override
    boolean isSameSetterFunction(Object function) {
        return false;
    }

    /** Same for the "getter" function. */
    @Override
    boolean isSameGetterFunction(Object function) {
        return false;
    }

    @Override
    public final boolean keyMatches(Object key, int indexOrHash) {
        return indexOrHash == this.indexOrHash && Objects.equals(this.name, key);
    }

    @Override
    public final Object getKey() {
        return name != null ? name : indexOrHash;
    }

    @Override
    public final Object getName() {
        return name;
    }

    @Override
    public final int getIndexOrHash() {
        return indexOrHash;
    }
}
