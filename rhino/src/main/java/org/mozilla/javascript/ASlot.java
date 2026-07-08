package org.mozilla.javascript;

import java.io.Serializable;
import org.mozilla.javascript.ScriptableObject.DescriptorInfo;

/**
 * A Slot is the base class for all properties stored in the ScriptableObject class. There are a
 * number of different types of slots. This base class represents an "ordinary" property such as a
 * primitive type or another object. Separate classes are used to represent properties that have
 * various types of getter and setter methods.
 */
public abstract class ASlot<T extends PropHolder<T>> implements Serializable {
    private short attributes;
    Object value;
    transient ASlot<T> next; // next in hash table bucket
    transient ASlot<T> orderedNext; // next in linked list

    ASlot(int attributes) {
        this.attributes = (short) attributes;
    }

    abstract ASlot<T> copySlot();

    /**
     * Return true if this is a base-class "Slot". Sadly too much code breaks if we try to do this
     * any other way.
     */
    boolean isValueSlot() {
        return true;
    }

    /**
     * Return true if this is a "setter slot" which, which we need to know for some legacy support.
     */
    boolean isSetterSlot() {
        return false;
    }

    protected ASlot(ASlot<T> oldSlot) {
        attributes = oldSlot.attributes;
        value = oldSlot.value;
        next = oldSlot.next;
        orderedNext = oldSlot.orderedNext;
    }

    public final boolean setValue(Object value, T owner, T start) {
        return setValue(value, owner, start, Context.isCurrentContextStrict());
    }

    public boolean setValue(Object value, T owner, T start, boolean isThrow) {
        if ((attributes & ScriptableObject.READONLY) != 0) {
            if (isThrow) {
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

    public Object getValue(T start) {
        return value;
    }

    int getAttributes() {
        return attributes;
    }

    void setAttributes(int value) {
        ScriptableObject.checkValidAttributes(value);
        attributes = (short) value;
    }

    DescriptorInfo getPropertyDescriptor(Context cx, T scope) {
        return ScriptableObject.buildDataDescriptor(value, attributes);
    }

    protected abstract void throwNoSetterException(T start, Object newValue);

    /**
     * Return a JavaScript function that represents the "setter". This is used by some legacy
     * functionality. Return null if there is no setter.
     */
    Function getSetterFunction(String name, T scope) {
        return null;
    }

    /** Same for the "getter." */
    Function getGetterFunction(String name, T scope) {
        return null;
    }

    /**
     * Compare the JavaScript function that represents the "setter" to the provided Object. We do
     * this to avoid generating a new function object when it might not be required. Specifically,
     * if we have a cached funciion object that has not yet been generated then we don't have to
     * generate it because it cannot be the same as the provided function.
     */
    boolean isSameSetterFunction(Object function) {
        return false;
    }

    /** Same for the "getter" function. */
    boolean isSameGetterFunction(Object function) {
        return false;
    }

    public abstract boolean keyMatches(Object key, int indexOrHash);

    public abstract Object getKey();

    public abstract Object getName();

    public abstract int getIndexOrHash();
}
