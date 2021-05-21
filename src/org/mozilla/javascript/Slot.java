package org.mozilla.javascript;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * A Slot is the base class for all properties stored in the ScriptableObject class. There are a
 * number of different types of slots. This base class represents an "ordinary" property such as a
 * primitive type or another object. Separate classes are used to represent properties that have
 * various types of getter and setter methods.
 */
public class Slot implements Serializable {
    private static final long serialVersionUID = -6090581677123995491L;
    Object name; // This can change due to caching
    int indexOrHash;
    private short attributes;
    Object value;
    transient Slot next; // next in hash table bucket
    transient Slot orderedNext; // next in linked list

    Slot(Object name, int indexOrHash, int attributes) {
        this.name = name;
        this.indexOrHash = indexOrHash;
        this.attributes = (short) attributes;
    }

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

    protected Slot(Slot oldSlot) {
        name = oldSlot.name;
        indexOrHash = oldSlot.indexOrHash;
        attributes = oldSlot.attributes;
        value = oldSlot.value;
        next = oldSlot.next;
        orderedNext = oldSlot.orderedNext;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (name != null) {
            indexOrHash = name.hashCode();
        }
    }

    public boolean setValue(Object value, Scriptable owner, Scriptable start) {
        if ((attributes & ScriptableObject.READONLY) != 0) {
            if (Context.isCurrentContextStrict()) {
                throw ScriptRuntime.typeErrorById("msg.modify.readonly", name);
            }
            return true;
        }
        if (owner == start) {
            this.value = value;
            return true;
        }
        return false;
    }

    public Object getValue(Scriptable start) {
        return value;
    }

    int getAttributes() {
        return attributes;
    }

    synchronized void setAttributes(int value) {
        ScriptableObject.checkValidAttributes(value);
        attributes = (short) value;
    }

    ScriptableObject getPropertyDescriptor(Context cx, Scriptable scope) {
        return ScriptableObject.buildDataDescriptor(scope, value, attributes);
    }

    protected void throwNoSetterException(Scriptable start, Object newValue) {
        Context cx = Context.getContext();
        if (cx.isStrictMode()
                ||
                // Based on TC39 ES3.1 Draft of 9-Feb-2009, 8.12.4, step 2,
                // we should throw a TypeError in this case.
                cx.hasFeature(Context.FEATURE_STRICT_MODE)) {

            String prop = "";
            if (name != null) {
                prop = "[" + start.getClassName() + "]." + name;
            }
            throw ScriptRuntime.typeErrorById(
                    "msg.set.prop.no.setter", prop, Context.toString(newValue));
        }
    }

    /**
     * Return a JavaScript function that represents the "setter". This is used by some legacy
     * functionality. Return null if there is no setter.
     */
    Function getSetterFunction(String name, Scriptable scope) {
        return null;
    }

    /** Same for the "getter." */
    Function getGetterFunction(String name, Scriptable scope) {
        return null;
    }
}
