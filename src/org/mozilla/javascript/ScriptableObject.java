/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// API class

package org.mozilla.javascript;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.mozilla.javascript.ScriptRuntime.StringIdOrIndex;
import org.mozilla.javascript.annotations.JSConstructor;
import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;
import org.mozilla.javascript.annotations.JSStaticFunction;
import org.mozilla.javascript.debug.DebuggableObject;

/**
 * This is the default implementation of the Scriptable interface. This
 * class provides convenient default behavior that makes it easier to
 * define host objects.
 * <p>
 * Various properties and methods of JavaScript objects can be conveniently
 * defined using methods of ScriptableObject.
 * <p>
 * Classes extending ScriptableObject must define the getClassName method.
 *
 * @see org.mozilla.javascript.Scriptable
 * @author Norris Boyd
 */

public abstract class ScriptableObject implements Scriptable,
                                                  SymbolScriptable,
                                                  Serializable,
                                                  DebuggableObject,
                                                  ConstProperties
{

    private static final long serialVersionUID = 2829861078851942586L;

    /**
     * The empty property attribute.
     *
     * Used by getAttributes() and setAttributes().
     *
     * @see org.mozilla.javascript.ScriptableObject#getAttributes(String)
     * @see org.mozilla.javascript.ScriptableObject#setAttributes(String, int)
     */
    public static final int EMPTY =     0x00;

    /**
     * Property attribute indicating assignment to this property is ignored.
     *
     * @see org.mozilla.javascript.ScriptableObject
     *      #put(String, Scriptable, Object)
     * @see org.mozilla.javascript.ScriptableObject#getAttributes(String)
     * @see org.mozilla.javascript.ScriptableObject#setAttributes(String, int)
     */
    public static final int READONLY =  0x01;

    /**
     * Property attribute indicating property is not enumerated.
     *
     * Only enumerated properties will be returned by getIds().
     *
     * @see org.mozilla.javascript.ScriptableObject#getIds()
     * @see org.mozilla.javascript.ScriptableObject#getAttributes(String)
     * @see org.mozilla.javascript.ScriptableObject#setAttributes(String, int)
     */
    public static final int DONTENUM =  0x02;

    /**
     * Property attribute indicating property cannot be deleted.
     *
     * @see org.mozilla.javascript.ScriptableObject#delete(String)
     * @see org.mozilla.javascript.ScriptableObject#getAttributes(String)
     * @see org.mozilla.javascript.ScriptableObject#setAttributes(String, int)
     */
    public static final int PERMANENT = 0x04;

    /**
     * Property attribute indicating that this is a const property that has not
     * been assigned yet.  The first 'const' assignment to the property will
     * clear this bit.
     */
    public static final int UNINITIALIZED_CONST = 0x08;

    public static final int CONST = PERMANENT|READONLY|UNINITIALIZED_CONST;
    /**
     * The prototype of this object.
     */
    private Scriptable prototypeObject;

    /**
     * The parent scope of this object.
     */
    private Scriptable parentScopeObject;

    /**
     * This holds all the slots. It may or may not be thread-safe, and may expand itself to
     * a different data structure depending on the size of the object.
     */
    private transient SlotMapContainer slotMap;

    // Where external array data is stored.
    private transient ExternalArrayData externalData;

    private volatile Map<Object,Object> associatedValues;

    enum SlotAccess {
        QUERY, MODIFY, MODIFY_CONST, MODIFY_GETTER_SETTER, CONVERT_ACCESSOR_TO_DATA
    }

    private boolean isExtensible = true;
    private boolean isSealed = false;

    private static final Method GET_ARRAY_LENGTH;

    static {
        try {
            GET_ARRAY_LENGTH = ScriptableObject.class.getMethod("getExternalArrayLength");
        } catch (NoSuchMethodException nsm) {
            throw new RuntimeException(nsm);
        }
    }

    /**
     * This is the object that is stored in the SlotMap. For historical reasons it remains
     * inside this class. SlotMap references a number of members of this class directly.
     */
    static class Slot implements Serializable
    {
        private static final long serialVersionUID = -6090581677123995491L;
        Object name; // This can change due to caching
        int indexOrHash;
        private short attributes;
        Object value;
        transient Slot next; // next in hash table bucket
        transient Slot orderedNext; // next in linked list

        Slot(Object name, int indexOrHash, int attributes)
        {
            this.name = name;
            this.indexOrHash = indexOrHash;
            this.attributes = (short)attributes;
        }

        private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException
        {
            in.defaultReadObject();
            if (name != null) {
                indexOrHash = name.hashCode();
            }
        }

        boolean setValue(Object value, Scriptable owner, Scriptable start) {
            if ((attributes & READONLY) != 0) {
                if (Context.getContext().isStrictMode()) {
                    throw ScriptRuntime.typeError1("msg.modify.readonly", name);
                }
                return true;
            }
            if (owner == start) {
                this.value = value;
                return true;
            }
            return false;
        }

        Object getValue(Scriptable start) {
            return value;
        }

        int getAttributes()
        {
            return attributes;
        }

        synchronized void setAttributes(int value)
        {
            checkValidAttributes(value);
            attributes = (short)value;
        }

        ScriptableObject getPropertyDescriptor(Context cx, Scriptable scope) {
            return buildDataDescriptor(scope, value, attributes);
        }

    }

    protected static ScriptableObject buildDataDescriptor(Scriptable scope,
                                                          Object value,
                                                          int attributes) {
        ScriptableObject desc = new NativeObject();
        ScriptRuntime.setBuiltinProtoAndParent(desc, scope, TopLevel.Builtins.Object);
        desc.defineProperty("value", value, EMPTY);
        desc.defineProperty("writable", Boolean.valueOf((attributes & READONLY) == 0), EMPTY);
        desc.defineProperty("enumerable", Boolean.valueOf((attributes & DONTENUM) == 0), EMPTY);
        desc.defineProperty("configurable", Boolean.valueOf((attributes & PERMANENT) == 0), EMPTY);
        return desc;
    }

    /**
     * A GetterSlot is a specialication of a Slot for properties that are assigned functions
     * via Object.defineProperty() and its friends instead of regular values.
     */
    static final class GetterSlot extends Slot
    {
        private static final long serialVersionUID = -4900574849788797588L;

        Object getter;
        Object setter;

        GetterSlot(Object name, int indexOrHash, int attributes)
        {
            super(name, indexOrHash, attributes);
        }

        @Override
        ScriptableObject getPropertyDescriptor(Context cx, Scriptable scope) {
            int attr = getAttributes();
            ScriptableObject desc = new NativeObject();
            ScriptRuntime.setBuiltinProtoAndParent(desc, scope, TopLevel.Builtins.Object);
            desc.defineProperty("enumerable", Boolean.valueOf((attr & DONTENUM) == 0), EMPTY);
            desc.defineProperty("configurable", Boolean.valueOf((attr & PERMANENT) == 0), EMPTY);
            if (getter == null && setter == null) {
                desc.defineProperty("writable", Boolean.valueOf((attr & READONLY) == 0), EMPTY);
            }

            String fName = name == null ? "f" : name.toString();
            if (getter != null) {
                if ( getter instanceof MemberBox ) {
                    desc.defineProperty("get", new FunctionObject(fName, ((MemberBox)getter).member(), scope), EMPTY);
                } else if ( getter instanceof Member ) {
                    desc.defineProperty("get", new FunctionObject(fName, (Member)getter, scope), EMPTY);
                } else {
                    desc.defineProperty("get", getter, EMPTY);
                }
            }
            if (setter != null) {
                if ( setter instanceof MemberBox ) {
                    desc.defineProperty("set", new FunctionObject(fName, ((MemberBox) setter).member(), scope), EMPTY);
                } else if ( setter instanceof Member ) {
                    desc.defineProperty("set", new FunctionObject(fName, (Member) setter, scope), EMPTY);
                } else {
                    desc.defineProperty("set", setter, EMPTY);
                }
            }
            return desc;
        }

        @Override
        boolean setValue(Object value, Scriptable owner, Scriptable start) {
            if (setter == null) {
                if (getter != null) {
                    Context cx = Context.getContext();
                    if (cx.isStrictMode() ||
                        // Based on TC39 ES3.1 Draft of 9-Feb-2009, 8.12.4, step 2,
                        // we should throw a TypeError in this case.
                        cx.hasFeature(Context.FEATURE_STRICT_MODE)) {

                        String prop = "";
                        if (name != null) {
                            prop = "[" + start.getClassName() + "]." + name;
                        }
                        throw ScriptRuntime.typeError2("msg.set.prop.no.setter", prop, Context.toString(value));
                    }
                    // Assignment to a property with only a getter defined. The
                    // assignment is ignored. See bug 478047.
                    return true;
                }
            } else {
                Context cx = Context.getContext();
                if (setter instanceof MemberBox) {
                    MemberBox nativeSetter = (MemberBox)setter;
                    Class<?> pTypes[] = nativeSetter.argTypes;
                    // XXX: cache tag since it is already calculated in
                    // defineProperty ?
                    Class<?> valueType = pTypes[pTypes.length - 1];
                    int tag = FunctionObject.getTypeTag(valueType);
                    Object actualArg = FunctionObject.convertArg(cx, start,
                                                                 value, tag);
                    Object setterThis;
                    Object[] args;
                    if (nativeSetter.delegateTo == null) {
                        setterThis = start;
                        args = new Object[] { actualArg };
                    } else {
                        setterThis = nativeSetter.delegateTo;
                        args = new Object[] { start, actualArg };
                    }
                    nativeSetter.invoke(setterThis, args);
                } else if (setter instanceof Function) {
                    Function f = (Function)setter;
                    f.call(cx, f.getParentScope(), start,
                           new Object[] { value });
                }
                return true;
            }
            return super.setValue(value, owner, start);
        }

        @Override
        Object getValue(Scriptable start) {
            if (getter != null) {
                if (getter instanceof MemberBox) {
                    MemberBox nativeGetter = (MemberBox)getter;
                    Object getterThis;
                    Object[] args;
                    if (nativeGetter.delegateTo == null) {
                        getterThis = start;
                        args = ScriptRuntime.emptyArgs;
                    } else {
                        getterThis = nativeGetter.delegateTo;
                        args = new Object[] { start };
                    }
                    return nativeGetter.invoke(getterThis, args);
                } else if (getter instanceof Function) {
                    Function f = (Function)getter;
                    Context cx = Context.getContext();
                    return f.call(cx, f.getParentScope(), start,
                                  ScriptRuntime.emptyArgs);
                }
            }
            Object val = this.value;
            if (val instanceof LazilyLoadedCtor) {
                LazilyLoadedCtor initializer = (LazilyLoadedCtor)val;
                try {
                    initializer.init();
                } finally {
                    this.value = val = initializer.getValue();
                }
            }
            return val;
        }
    }

    static void checkValidAttributes(int attributes)
    {
        final int mask = READONLY | DONTENUM | PERMANENT | UNINITIALIZED_CONST;
        if ((attributes & ~mask) != 0) {
            throw new IllegalArgumentException(String.valueOf(attributes));
        }
    }

    private static SlotMapContainer createSlotMap(int initialSize)
    {
        Context cx = Context.getCurrentContext();
        if ((cx != null) && cx.hasFeature(Context.FEATURE_THREAD_SAFE_OBJECTS)) {
            return new ThreadSafeSlotMapContainer(initialSize);
        }
        return new SlotMapContainer(initialSize);
    }

    public ScriptableObject()
    {
        slotMap = createSlotMap(0);
    }

    public ScriptableObject(Scriptable scope, Scriptable prototype)
    {
        if (scope == null)
            throw new IllegalArgumentException();

        parentScopeObject = scope;
        prototypeObject = prototype;
        slotMap = createSlotMap(0);
    }

    /**
     * Gets the value that will be returned by calling the typeof operator on this object.
     * @return default is "object" unless {@link #avoidObjectDetection()} is <code>true</code> in which
     * case it returns "undefined"
     */
    public String getTypeOf() {
        return avoidObjectDetection() ? "undefined" : "object";
    }

    /**
     * Return the name of the class.
     *
     * This is typically the same name as the constructor.
     * Classes extending ScriptableObject must implement this abstract
     * method.
     */
    @Override
    public abstract String getClassName();

    /**
     * Returns true if the named property is defined.
     *
     * @param name the name of the property
     * @param start the object in which the lookup began
     * @return true if and only if the property was found in the object
     */
    @Override
    public boolean has(String name, Scriptable start)
    {
        return null != slotMap.query(name, 0);
    }

    /**
     * Returns true if the property index is defined.
     *
     * @param index the numeric index for the property
     * @param start the object in which the lookup began
     * @return true if and only if the property was found in the object
     */
    @Override
    public boolean has(int index, Scriptable start)
    {
        if (externalData != null) {
            return (index < externalData.getArrayLength());
        }
        return null != slotMap.query(null, index);
    }

    /**
     * A version of "has" that supports symbols.
     */
    @Override
    public boolean has(Symbol key, Scriptable start)
    {
        return null != slotMap.query(key, 0);
    }

    /**
     * Returns the value of the named property or NOT_FOUND.
     *
     * If the property was created using defineProperty, the
     * appropriate getter method is called.
     *
     * @param name the name of the property
     * @param start the object in which the lookup began
     * @return the value of the property (may be null), or NOT_FOUND
     */
    @Override
    public Object get(String name, Scriptable start)
    {
        Slot slot = slotMap.query(name, 0);
        if (slot == null) {
            return Scriptable.NOT_FOUND;
        }
        return slot.getValue(start);
    }

    /**
     * Returns the value of the indexed property or NOT_FOUND.
     *
     * @param index the numeric index for the property
     * @param start the object in which the lookup began
     * @return the value of the property (may be null), or NOT_FOUND
     */
    @Override
    public Object get(int index, Scriptable start)
    {
        if (externalData != null) {
            if (index < externalData.getArrayLength()) {
                return externalData.getArrayElement(index);
            }
            return Scriptable.NOT_FOUND;
        }

        Slot slot = slotMap.query(null, index);
        if (slot == null) {
            return Scriptable.NOT_FOUND;
        }
        return slot.getValue(start);
    }

    /**
     * Another version of Get that supports Symbol keyed properties.
     */
    @Override
    public Object get(Symbol key, Scriptable start)
    {
        Slot slot = slotMap.query(key, 0);
        if (slot == null) {
            return Scriptable.NOT_FOUND;
        }
        return slot.getValue(start);
    }

    /**
     * Sets the value of the named property, creating it if need be.
     *
     * If the property was created using defineProperty, the
     * appropriate setter method is called. <p>
     *
     * If the property's attributes include READONLY, no action is
     * taken.
     * This method will actually set the property in the start
     * object.
     *
     * @param name the name of the property
     * @param start the object whose property is being set
     * @param value value to set the property to
     */
    @Override
    public void put(String name, Scriptable start, Object value)
    {
        if (putImpl(name, 0, start, value))
            return;

        if (start == this) throw Kit.codeBug();
        start.put(name, start, value);
    }

    /**
     * Sets the value of the indexed property, creating it if need be.
     *
     * @param index the numeric index for the property
     * @param start the object whose property is being set
     * @param value value to set the property to
     */
    @Override
    public void put(int index, Scriptable start, Object value)
    {
        if (externalData != null) {
            if (index < externalData.getArrayLength()) {
                externalData.setArrayElement(index, value);
            } else {
                throw new JavaScriptException(
                    ScriptRuntime.newNativeError(Context.getCurrentContext(), this,
                                                 TopLevel.NativeErrors.RangeError,
                                                 new Object[] { "External array index out of bounds " }),
                    null, 0);
            }
            return;
        }

        if (putImpl(null, index, start, value))
            return;

        if (start == this) throw Kit.codeBug();
        start.put(index, start, value);
    }

    /**
     * Implementation of put required by SymbolScriptable objects.
     */
    @Override
    public void put(Symbol key, Scriptable start, Object value)
    {
        if (putImpl(key, 0, start, value))
            return;

        if (start == this) throw Kit.codeBug();
        ensureSymbolScriptable(start).put(key, start, value);
    }

    /**
     * Removes a named property from the object.
     *
     * If the property is not found, or it has the PERMANENT attribute,
     * no action is taken.
     *
     * @param name the name of the property
     */
    @Override
    public void delete(String name)
    {
        checkNotSealed(name, 0);
        slotMap.remove(name, 0);
    }

    /**
     * Removes the indexed property from the object.
     *
     * If the property is not found, or it has the PERMANENT attribute,
     * no action is taken.
     *
     * @param index the numeric index for the property
     */
    @Override
    public void delete(int index)
    {
        checkNotSealed(null, index);
        slotMap.remove(null, index);
    }

    /**
     * Removes an object like the others, but using a Symbol as the key.
     */
    @Override
    public void delete(Symbol key)
    {
        checkNotSealed(key, 0);
        slotMap.remove(key, 0);
    }

    /**
     * Sets the value of the named const property, creating it if need be.
     *
     * If the property was created using defineProperty, the
     * appropriate setter method is called. <p>
     *
     * If the property's attributes include READONLY, no action is
     * taken.
     * This method will actually set the property in the start
     * object.
     *
     * @param name the name of the property
     * @param start the object whose property is being set
     * @param value value to set the property to
     */
    @Override
    public void putConst(String name, Scriptable start, Object value)
    {
        if (putConstImpl(name, 0, start, value, READONLY))
            return;

        if (start == this) throw Kit.codeBug();
        if (start instanceof ConstProperties)
            ((ConstProperties)start).putConst(name, start, value);
        else
            start.put(name, start, value);
    }

    @Override
    public void defineConst(String name, Scriptable start)
    {
        if (putConstImpl(name, 0, start, Undefined.instance, UNINITIALIZED_CONST))
            return;

        if (start == this) throw Kit.codeBug();
        if (start instanceof ConstProperties)
            ((ConstProperties)start).defineConst(name, start);
    }

    /**
     * Returns true if the named property is defined as a const on this object.
     * @param name
     * @return true if the named property is defined as a const, false
     * otherwise.
     */
    @Override
    public boolean isConst(String name)
    {
        Slot slot = slotMap.query(name, 0);
        if (slot == null) {
            return false;
        }
        return (slot.getAttributes() & (PERMANENT|READONLY)) ==
                                       (PERMANENT|READONLY);

    }

    /**
     * @deprecated Use {@link #getAttributes(String name)}. The engine always
     * ignored the start argument.
     */
    @Deprecated
    public final int getAttributes(String name, Scriptable start)
    {
        return getAttributes(name);
    }

    /**
     * @deprecated Use {@link #getAttributes(int index)}. The engine always
     * ignored the start argument.
     */
    @Deprecated
    public final int getAttributes(int index, Scriptable start)
    {
        return getAttributes(index);
    }

    /**
     * @deprecated Use {@link #setAttributes(String name, int attributes)}.
     * The engine always ignored the start argument.
     */
    @Deprecated
    public final void setAttributes(String name, Scriptable start,
                                    int attributes)
    {
        setAttributes(name, attributes);
    }

    /**
     * @deprecated Use {@link #setAttributes(int index, int attributes)}.
     * The engine always ignored the start argument.
     */
    @Deprecated
    public void setAttributes(int index, Scriptable start,
                              int attributes)
    {
        setAttributes(index, attributes);
    }

    /**
     * Get the attributes of a named property.
     *
     * The property is specified by <code>name</code>
     * as defined for <code>has</code>.<p>
     *
     * @param name the identifier for the property
     * @return the bitset of attributes
     * @exception EvaluatorException if the named property is not found
     * @see org.mozilla.javascript.ScriptableObject#has(String, Scriptable)
     * @see org.mozilla.javascript.ScriptableObject#READONLY
     * @see org.mozilla.javascript.ScriptableObject#DONTENUM
     * @see org.mozilla.javascript.ScriptableObject#PERMANENT
     * @see org.mozilla.javascript.ScriptableObject#EMPTY
     */
    public int getAttributes(String name)
    {
        return findAttributeSlot(name, 0, SlotAccess.QUERY).getAttributes();
    }

    /**
     * Get the attributes of an indexed property.
     *
     * @param index the numeric index for the property
     * @exception EvaluatorException if the named property is not found
     *            is not found
     * @return the bitset of attributes
     * @see org.mozilla.javascript.ScriptableObject#has(String, Scriptable)
     * @see org.mozilla.javascript.ScriptableObject#READONLY
     * @see org.mozilla.javascript.ScriptableObject#DONTENUM
     * @see org.mozilla.javascript.ScriptableObject#PERMANENT
     * @see org.mozilla.javascript.ScriptableObject#EMPTY
     */
    public int getAttributes(int index)
    {
        return findAttributeSlot(null, index, SlotAccess.QUERY).getAttributes();
    }

    public int getAttributes(Symbol sym)
    {
        return findAttributeSlot(sym, SlotAccess.QUERY).getAttributes();
    }


    /**
     * Set the attributes of a named property.
     *
     * The property is specified by <code>name</code>
     * as defined for <code>has</code>.<p>
     *
     * The possible attributes are READONLY, DONTENUM,
     * and PERMANENT. Combinations of attributes
     * are expressed by the bitwise OR of attributes.
     * EMPTY is the state of no attributes set. Any unused
     * bits are reserved for future use.
     *
     * @param name the name of the property
     * @param attributes the bitset of attributes
     * @exception EvaluatorException if the named property is not found
     * @see org.mozilla.javascript.Scriptable#has(String, Scriptable)
     * @see org.mozilla.javascript.ScriptableObject#READONLY
     * @see org.mozilla.javascript.ScriptableObject#DONTENUM
     * @see org.mozilla.javascript.ScriptableObject#PERMANENT
     * @see org.mozilla.javascript.ScriptableObject#EMPTY
     */
    public void setAttributes(String name, int attributes)
    {
        checkNotSealed(name, 0);
        findAttributeSlot(name, 0, SlotAccess.MODIFY).setAttributes(attributes);
    }

    /**
     * Set the attributes of an indexed property.
     *
     * @param index the numeric index for the property
     * @param attributes the bitset of attributes
     * @exception EvaluatorException if the named property is not found
     * @see org.mozilla.javascript.Scriptable#has(String, Scriptable)
     * @see org.mozilla.javascript.ScriptableObject#READONLY
     * @see org.mozilla.javascript.ScriptableObject#DONTENUM
     * @see org.mozilla.javascript.ScriptableObject#PERMANENT
     * @see org.mozilla.javascript.ScriptableObject#EMPTY
     */
    public void setAttributes(int index, int attributes)
    {
        checkNotSealed(null, index);
        findAttributeSlot(null, index, SlotAccess.MODIFY).setAttributes(attributes);
    }

    /**
     * Set attributes of a Symbol-keyed property.
     */
    public void setAttributes(Symbol key, int attributes)
    {
        checkNotSealed(key, 0);
        findAttributeSlot(key, SlotAccess.MODIFY).setAttributes(attributes);
    }

    /**
     * XXX: write docs.
     */
    public void setGetterOrSetter(String name, int index,
                                  Callable getterOrSetter, boolean isSetter)
    {
        setGetterOrSetter(name, index, getterOrSetter, isSetter, false);
    }

    private void setGetterOrSetter(String name, int index, Callable getterOrSetter,
                                   boolean isSetter, boolean force)
    {
        if (name != null && index != 0)
            throw new IllegalArgumentException(name);

        if (!force) {
            checkNotSealed(name, index);
        }

        final GetterSlot gslot;
        if (isExtensible()) {
            gslot = (GetterSlot)slotMap.get(name, index, SlotAccess.MODIFY_GETTER_SETTER);
        } else {
            Slot slot = slotMap.query(name, index);
            if (!(slot instanceof GetterSlot))
                return;
            gslot = (GetterSlot) slot;
        }

        if (!force) {
            int attributes = gslot.getAttributes();
            if ((attributes & READONLY) != 0) {
                throw Context.reportRuntimeError1("msg.modify.readonly", name);
            }
        }
        if (isSetter) {
            gslot.setter = getterOrSetter;
        } else {
            gslot.getter = getterOrSetter;
        }
        gslot.value = Undefined.instance;
    }

    /**
     * Get the getter or setter for a given property. Used by __lookupGetter__
     * and __lookupSetter__.
     *
     * @param name Name of the object. If nonnull, index must be 0.
     * @param index Index of the object. If nonzero, name must be null.
     * @param isSetter If true, return the setter, otherwise return the getter.
     * @exception IllegalArgumentException if both name and index are nonnull
     *            and nonzero respectively.
     * @return Null if the property does not exist. Otherwise returns either
     *         the getter or the setter for the property, depending on
     *         the value of isSetter (may be undefined if unset).
     */
    public Object getGetterOrSetter(String name, int index, boolean isSetter)
    {
        if (name != null && index != 0)
            throw new IllegalArgumentException(name);
        Slot slot = slotMap.query(name, index);
        if (slot == null)
            return null;
        if (slot instanceof GetterSlot) {
            GetterSlot gslot = (GetterSlot)slot;
            Object result = isSetter ? gslot.setter : gslot.getter;
            return result != null ? result : Undefined.instance;
        }
        return Undefined.instance;
    }

    /**
     * Returns whether a property is a getter or a setter
     * @param name property name
     * @param index property index
     * @param setter true to check for a setter, false for a getter
     * @return whether the property is a getter or a setter
     */
    protected boolean isGetterOrSetter(String name, int index, boolean setter) {
        Slot slot = slotMap.query(name, index);
        if (slot instanceof GetterSlot) {
            if (setter && ((GetterSlot)slot).setter != null) return true;
            if (!setter && ((GetterSlot)slot).getter != null) return true;
        }
        return false;
    }

    void addLazilyInitializedValue(String name, int index,
                                   LazilyLoadedCtor init, int attributes)
    {
        if (name != null && index != 0)
            throw new IllegalArgumentException(name);
        checkNotSealed(name, index);
        GetterSlot gslot = (GetterSlot)slotMap.get(name, index,
            SlotAccess.MODIFY_GETTER_SETTER);
        gslot.setAttributes(attributes);
        gslot.getter = null;
        gslot.setter = null;
        gslot.value = init;
    }

    /**
     * Attach the specified object to this object, and delegate all indexed property lookups to it. In other words,
     * if the object has 3 elements, then an attempt to look up or modify "[0]", "[1]", or "[2]" will be delegated
     * to this object. Additional indexed properties outside the range specified, and additional non-indexed
     * properties, may still be added. The object specified must implement the ExternalArrayData interface.
     *
     * @param array the List to use for delegated property access. Set this to null to revert back to regular
     *              property access.
     * @since 1.7.6
     */
    public void setExternalArrayData(ExternalArrayData array)
    {
        externalData = array;

        if (array == null) {
            delete("length");
        } else {
            // Define "length" to return whatever length the List gives us.
            defineProperty("length", null,
                           GET_ARRAY_LENGTH, null, READONLY | DONTENUM);
        }
    }

    /**
     * Return the array that was previously set by the call to "setExternalArrayData".
     *
     * @return the array, or null if it was never set
     * @since 1.7.6
     */
    public ExternalArrayData getExternalArrayData()
    {
        return externalData;
    }

    /**
     * This is a function used by setExternalArrayData to dynamically get the "length" property value.
     */
    public Object getExternalArrayLength()
    {
        return Integer.valueOf(externalData == null ? 0 : externalData.getArrayLength());
    }

    /**
     * Returns the prototype of the object.
     */
    @Override
    public Scriptable getPrototype()
    {
        return prototypeObject;
    }

    /**
     * Sets the prototype of the object.
     */
    @Override
    public void setPrototype(Scriptable m)
    {
        if (!isExtensible()
                && Context.getContext().getLanguageVersion() >= Context.VERSION_1_8) {
            throw ScriptRuntime.typeError0("msg.not.extensible");
        }
        prototypeObject = m;
    }

    /**
     * Returns the parent (enclosing) scope of the object.
     */
    @Override
    public Scriptable getParentScope()
    {
        return parentScopeObject;
    }

    /**
     * Sets the parent (enclosing) scope of the object.
     */
    @Override
    public void setParentScope(Scriptable m)
    {
        parentScopeObject = m;
    }

    /**
     * Returns an array of ids for the properties of the object.
     *
     * <p>Any properties with the attribute DONTENUM are not listed. <p>
     *
     * @return an array of java.lang.Objects with an entry for every
     * listed property. Properties accessed via an integer index will
     * have a corresponding
     * Integer entry in the returned array. Properties accessed by
     * a String will have a String entry in the returned array.
     */
    @Override
    public Object[] getIds() {
        return getIds(false, false);
    }

    /**
     * Returns an array of ids for the properties of the object.
     *
     * <p>All properties, even those with attribute DONTENUM, are listed. <p>
     *
     * @return an array of java.lang.Objects with an entry for every
     * listed property. Properties accessed via an integer index will
     * have a corresponding
     * Integer entry in the returned array. Properties accessed by
     * a String will have a String entry in the returned array.
     */
    @Override
    public Object[] getAllIds() {
        return getIds(true, false);
    }

    /**
     * Implements the [[DefaultValue]] internal method.
     *
     * <p>Note that the toPrimitive conversion is a no-op for
     * every type other than Object, for which [[DefaultValue]]
     * is called. See ECMA 9.1.<p>
     *
     * A <code>hint</code> of null means "no hint".
     *
     * @param typeHint the type hint
     * @return the default value for the object
     *
     * See ECMA 8.6.2.6.
     */
    @Override
    public Object getDefaultValue(Class<?> typeHint)
    {
        return getDefaultValue(this, typeHint);
    }

    public static Object getDefaultValue(Scriptable object, Class<?> typeHint)
    {
        Context cx = null;
        for (int i=0; i < 2; i++) {
            boolean tryToString;
            if (typeHint == ScriptRuntime.StringClass) {
                tryToString = (i == 0);
            } else {
                tryToString = (i == 1);
            }

            String methodName;
            if (tryToString) {
                methodName = "toString";
            } else {
                methodName = "valueOf";
            }
            Object v = getProperty(object, methodName);
            if (!(v instanceof Function))
                continue;
            Function fun = (Function) v;
            if (cx == null) {
                cx = Context.getContext();
            }
            v = fun.call(cx, fun.getParentScope(), object, ScriptRuntime.emptyArgs);
            if (v != null) {
                if (!(v instanceof Scriptable)) {
                    return v;
                }
                if (typeHint == ScriptRuntime.ScriptableClass
                    || typeHint == ScriptRuntime.FunctionClass)
                {
                    return v;
                }
                if (tryToString && v instanceof Wrapper) {
                    // Let a wrapped java.lang.String pass for a primitive
                    // string.
                    Object u = ((Wrapper)v).unwrap();
                    if (u instanceof String)
                        return u;
                }
            }
        }
        // fall through to error
        String arg = (typeHint == null) ? "undefined" : typeHint.getName();
        throw ScriptRuntime.typeError1("msg.default.value", arg);
    }

    /**
     * Implements the instanceof operator.
     *
     * <p>This operator has been proposed to ECMA.
     *
     * @param instance The value that appeared on the LHS of the instanceof
     *              operator
     * @return true if "this" appears in value's prototype chain
     *
     */
    @Override
    public boolean hasInstance(Scriptable instance) {
        // Default for JS objects (other than Function) is to do prototype
        // chasing.  This will be overridden in NativeFunction and non-JS
        // objects.

        return ScriptRuntime.jsDelegatesTo(instance, this);
    }

    /**
     * Emulate the SpiderMonkey (and Firefox) feature of allowing
     * custom objects to avoid detection by normal "object detection"
     * code patterns. This is used to implement document.all.
     * See https://bugzilla.mozilla.org/show_bug.cgi?id=412247.
     * This is an analog to JOF_DETECTING from SpiderMonkey; see
     * https://bugzilla.mozilla.org/show_bug.cgi?id=248549.
     * Other than this special case, embeddings should return false.
     * @return true if this object should avoid object detection
     * @since 1.7R1
     */
    public boolean avoidObjectDetection() {
        return false;
    }

    /**
     * Custom <code>==</code> operator.
     * Must return {@link Scriptable#NOT_FOUND} if this object does not
     * have custom equality operator for the given value,
     * <code>Boolean.TRUE</code> if this object is equivalent to <code>value</code>,
     * <code>Boolean.FALSE</code> if this object is not equivalent to
     * <code>value</code>.
     * <p>
     * The default implementation returns Boolean.TRUE
     * if <code>this == value</code> or {@link Scriptable#NOT_FOUND} otherwise.
     * It indicates that by default custom equality is available only if
     * <code>value</code> is <code>this</code> in which case true is returned.
     */
    protected Object equivalentValues(Object value)
    {
        return (this == value) ? Boolean.TRUE : Scriptable.NOT_FOUND;
    }

    /**
     * Defines JavaScript objects from a Java class that implements Scriptable.
     *
     * If the given class has a method
     * <pre>
     * static void init(Context cx, Scriptable scope, boolean sealed);</pre>
     *
     * or its compatibility form
     * <pre>
     * static void init(Scriptable scope);</pre>
     *
     * then it is invoked and no further initialization is done.<p>
     *
     * However, if no such a method is found, then the class's constructors and
     * methods are used to initialize a class in the following manner.<p>
     *
     * First, the zero-parameter constructor of the class is called to
     * create the prototype. If no such constructor exists,
     * a {@link EvaluatorException} is thrown. <p>
     *
     * Next, all methods are scanned for special prefixes that indicate that they
     * have special meaning for defining JavaScript objects.
     * These special prefixes are
     * <ul>
     * <li><code>jsFunction_</code> for a JavaScript function
     * <li><code>jsStaticFunction_</code> for a JavaScript function that
     *           is a property of the constructor
     * <li><code>jsGet_</code> for a getter of a JavaScript property
     * <li><code>jsSet_</code> for a setter of a JavaScript property
     * <li><code>jsConstructor</code> for a JavaScript function that
     *           is the constructor
     * </ul><p>
     *
     * If the method's name begins with "jsFunction_", a JavaScript function
     * is created with a name formed from the rest of the Java method name
     * following "jsFunction_". So a Java method named "jsFunction_foo" will
     * define a JavaScript method "foo". Calling this JavaScript function
     * will cause the Java method to be called. The parameters of the method
     * must be of number and types as defined by the FunctionObject class.
     * The JavaScript function is then added as a property
     * of the prototype. <p>
     *
     * If the method's name begins with "jsStaticFunction_", it is handled
     * similarly except that the resulting JavaScript function is added as a
     * property of the constructor object. The Java method must be static.
     *
     * If the method's name begins with "jsGet_" or "jsSet_", the method is
     * considered to define a property. Accesses to the defined property
     * will result in calls to these getter and setter methods. If no
     * setter is defined, the property is defined as READONLY.<p>
     *
     * If the method's name is "jsConstructor", the method is
     * considered to define the body of the constructor. Only one
     * method of this name may be defined. You may use the varargs forms
     * for constructors documented in {@link FunctionObject#FunctionObject(String, Member, Scriptable)}
     *
     * If no method is found that can serve as constructor, a Java
     * constructor will be selected to serve as the JavaScript
     * constructor in the following manner. If the class has only one
     * Java constructor, that constructor is used to define
     * the JavaScript constructor. If the the class has two constructors,
     * one must be the zero-argument constructor (otherwise an
     * {@link EvaluatorException} would have already been thrown
     * when the prototype was to be created). In this case
     * the Java constructor with one or more parameters will be used
     * to define the JavaScript constructor. If the class has three
     * or more constructors, an {@link EvaluatorException}
     * will be thrown.<p>
     *
     * Finally, if there is a method
     * <pre>
     * static void finishInit(Scriptable scope, FunctionObject constructor,
     *                        Scriptable prototype)</pre>
     *
     * it will be called to finish any initialization. The <code>scope</code>
     * argument will be passed, along with the newly created constructor and
     * the newly created prototype.<p>
     *
     * @param scope The scope in which to define the constructor.
     * @param clazz The Java class to use to define the JavaScript objects
     *              and properties.
     * @exception IllegalAccessException if access is not available
     *            to a reflected class member
     * @exception InstantiationException if unable to instantiate
     *            the named class
     * @exception InvocationTargetException if an exception is thrown
     *            during execution of methods of the named class
     * @see org.mozilla.javascript.Function
     * @see org.mozilla.javascript.FunctionObject
     * @see org.mozilla.javascript.ScriptableObject#READONLY
     * @see org.mozilla.javascript.ScriptableObject
     *      #defineProperty(String, Class, int)
     */
    public static <T extends Scriptable> void defineClass(
            Scriptable scope, Class<T> clazz)
        throws IllegalAccessException, InstantiationException,
               InvocationTargetException
    {
        defineClass(scope, clazz, false, false);
    }

    /**
     * Defines JavaScript objects from a Java class, optionally
     * allowing sealing.
     *
     * Similar to <code>defineClass(Scriptable scope, Class clazz)</code>
     * except that sealing is allowed. An object that is sealed cannot have
     * properties added or removed. Note that sealing is not allowed in
     * the current ECMA/ISO language specification, but is likely for
     * the next version.
     *
     * @param scope The scope in which to define the constructor.
     * @param clazz The Java class to use to define the JavaScript objects
     *              and properties. The class must implement Scriptable.
     * @param sealed Whether or not to create sealed standard objects that
     *               cannot be modified.
     * @exception IllegalAccessException if access is not available
     *            to a reflected class member
     * @exception InstantiationException if unable to instantiate
     *            the named class
     * @exception InvocationTargetException if an exception is thrown
     *            during execution of methods of the named class
     * @since 1.4R3
     */
    public static <T extends Scriptable> void defineClass(
            Scriptable scope, Class<T> clazz, boolean sealed)
        throws IllegalAccessException, InstantiationException,
               InvocationTargetException
    {
        defineClass(scope, clazz, sealed, false);
    }

    /**
     * Defines JavaScript objects from a Java class, optionally
     * allowing sealing and mapping of Java inheritance to JavaScript
     * prototype-based inheritance.
     *
     * Similar to <code>defineClass(Scriptable scope, Class clazz)</code>
     * except that sealing and inheritance mapping are allowed. An object
     * that is sealed cannot have properties added or removed. Note that
     * sealing is not allowed in the current ECMA/ISO language specification,
     * but is likely for the next version.
     *
     * @param scope The scope in which to define the constructor.
     * @param clazz The Java class to use to define the JavaScript objects
     *              and properties. The class must implement Scriptable.
     * @param sealed Whether or not to create sealed standard objects that
     *               cannot be modified.
     * @param mapInheritance Whether or not to map Java inheritance to
     *                       JavaScript prototype-based inheritance.
     * @return the class name for the prototype of the specified class
     * @exception IllegalAccessException if access is not available
     *            to a reflected class member
     * @exception InstantiationException if unable to instantiate
     *            the named class
     * @exception InvocationTargetException if an exception is thrown
     *            during execution of methods of the named class
     * @since 1.6R2
     */
    public static <T extends Scriptable> String defineClass(
            Scriptable scope, Class<T> clazz, boolean sealed,
            boolean mapInheritance)
        throws IllegalAccessException, InstantiationException,
               InvocationTargetException
    {
        BaseFunction ctor = buildClassCtor(scope, clazz, sealed,
                                           mapInheritance);
        if (ctor == null)
            return null;
        String name = ctor.getClassPrototype().getClassName();
        defineProperty(scope, name, ctor, ScriptableObject.DONTENUM);
        return name;
    }

    static <T extends Scriptable> BaseFunction buildClassCtor(
            Scriptable scope, Class<T> clazz,
            boolean sealed,
            boolean mapInheritance)
        throws IllegalAccessException, InstantiationException,
               InvocationTargetException
    {
        Method[] methods = FunctionObject.getMethodList(clazz);
        for (int i=0; i < methods.length; i++) {
            Method method = methods[i];
            if (!method.getName().equals("init"))
                continue;
            Class<?>[] parmTypes = method.getParameterTypes();
            if (parmTypes.length == 3 &&
                parmTypes[0] == ScriptRuntime.ContextClass &&
                parmTypes[1] == ScriptRuntime.ScriptableClass &&
                parmTypes[2] == Boolean.TYPE &&
                Modifier.isStatic(method.getModifiers()))
            {
                Object args[] = { Context.getContext(), scope,
                                  sealed ? Boolean.TRUE : Boolean.FALSE };
                method.invoke(null, args);
                return null;
            }
            if (parmTypes.length == 1 &&
                parmTypes[0] == ScriptRuntime.ScriptableClass &&
                Modifier.isStatic(method.getModifiers()))
            {
                Object args[] = { scope };
                method.invoke(null, args);
                return null;
            }

        }

        // If we got here, there isn't an "init" method with the right
        // parameter types.

        Constructor<?>[] ctors = clazz.getConstructors();
        Constructor<?> protoCtor = null;
        for (int i=0; i < ctors.length; i++) {
            if (ctors[i].getParameterTypes().length == 0) {
                protoCtor = ctors[i];
                break;
            }
        }
        if (protoCtor == null) {
            throw Context.reportRuntimeError1(
                      "msg.zero.arg.ctor", clazz.getName());
        }

        Scriptable proto = (Scriptable) protoCtor.newInstance(ScriptRuntime.emptyArgs);
        String className = proto.getClassName();

        // check for possible redefinition
        Object existing = getProperty(getTopLevelScope(scope), className);
        if (existing instanceof BaseFunction) {
            Object existingProto = ((BaseFunction)existing).getPrototypeProperty();
            if (existingProto != null && clazz.equals(existingProto.getClass())) {
                return (BaseFunction)existing;
            }
        }

        // Set the prototype's prototype, trying to map Java inheritance to JS
        // prototype-based inheritance if requested to do so.
        Scriptable superProto = null;
        if (mapInheritance) {
            Class<? super T> superClass = clazz.getSuperclass();
            if (ScriptRuntime.ScriptableClass.isAssignableFrom(superClass) &&
                !Modifier.isAbstract(superClass.getModifiers()))
            {
                Class<? extends Scriptable> superScriptable =
                    extendsScriptable(superClass);
                String name = ScriptableObject.defineClass(scope,
                        superScriptable, sealed, mapInheritance);
                if (name != null) {
                    superProto = ScriptableObject.getClassPrototype(scope, name);
                }
            }
        }
        if (superProto == null) {
            superProto = ScriptableObject.getObjectPrototype(scope);
        }
        proto.setPrototype(superProto);

        // Find out whether there are any methods that begin with
        // "js". If so, then only methods that begin with special
        // prefixes will be defined as JavaScript entities.
        final String functionPrefix = "jsFunction_";
        final String staticFunctionPrefix = "jsStaticFunction_";
        final String getterPrefix = "jsGet_";
        final String setterPrefix = "jsSet_";
        final String ctorName = "jsConstructor";

        Member ctorMember = findAnnotatedMember(methods, JSConstructor.class);
        if (ctorMember == null) {
            ctorMember = findAnnotatedMember(ctors, JSConstructor.class);
        }
        if (ctorMember == null) {
            ctorMember = FunctionObject.findSingleMethod(methods, ctorName);
        }
        if (ctorMember == null) {
            if (ctors.length == 1) {
                ctorMember = ctors[0];
            } else if (ctors.length == 2) {
                if (ctors[0].getParameterTypes().length == 0)
                    ctorMember = ctors[1];
                else if (ctors[1].getParameterTypes().length == 0)
                    ctorMember = ctors[0];
            }
            if (ctorMember == null) {
                throw Context.reportRuntimeError1(
                          "msg.ctor.multiple.parms", clazz.getName());
            }
        }

        FunctionObject ctor = new FunctionObject(className, ctorMember, scope);
        if (ctor.isVarArgsMethod()) {
            throw Context.reportRuntimeError1
                ("msg.varargs.ctor", ctorMember.getName());
        }
        ctor.initAsConstructor(scope, proto);

        Method finishInit = null;
        HashSet<String> staticNames = new HashSet<String>(),
                        instanceNames = new HashSet<String>();
        for (Method method : methods) {
            if (method == ctorMember) {
                continue;
            }
            String name = method.getName();
            if (name.equals("finishInit")) {
                Class<?>[] parmTypes = method.getParameterTypes();
                if (parmTypes.length == 3 &&
                    parmTypes[0] == ScriptRuntime.ScriptableClass &&
                    parmTypes[1] == FunctionObject.class &&
                    parmTypes[2] == ScriptRuntime.ScriptableClass &&
                    Modifier.isStatic(method.getModifiers()))
                {
                    finishInit = method;
                    continue;
                }
            }
            // ignore any compiler generated methods.
            if (name.indexOf('$') != -1)
                continue;
            if (name.equals(ctorName))
                continue;

            Annotation annotation = null;
            String prefix = null;
            if (method.isAnnotationPresent(JSFunction.class)) {
                annotation = method.getAnnotation(JSFunction.class);
            } else if (method.isAnnotationPresent(JSStaticFunction.class)) {
                annotation = method.getAnnotation(JSStaticFunction.class);
            } else if (method.isAnnotationPresent(JSGetter.class)) {
                annotation = method.getAnnotation(JSGetter.class);
            } else if (method.isAnnotationPresent(JSSetter.class)) {
                continue;
            }

            if (annotation == null) {
                if (name.startsWith(functionPrefix)) {
                    prefix = functionPrefix;
                } else if (name.startsWith(staticFunctionPrefix)) {
                    prefix = staticFunctionPrefix;
                } else if (name.startsWith(getterPrefix)) {
                    prefix = getterPrefix;
                } else {
                    // note that setterPrefix is among the unhandled names here -
                    // we deal with that when we see the getter
                    continue;
                }
            }

            boolean isStatic = annotation instanceof JSStaticFunction
                    || prefix == staticFunctionPrefix;
            HashSet<String> names = isStatic ? staticNames : instanceNames;
            String propName = getPropertyName(name, prefix, annotation);
            if (names.contains(propName)) {
                throw Context.reportRuntimeError2("duplicate.defineClass.name",
                        name, propName);
            }
            names.add(propName);
            name = propName;

            if (annotation instanceof JSGetter || prefix == getterPrefix) {
                if (!(proto instanceof ScriptableObject)) {
                    throw Context.reportRuntimeError2(
                        "msg.extend.scriptable",
                        proto.getClass().toString(), name);
                }
                Method setter = findSetterMethod(methods, name, setterPrefix);
                int attr = ScriptableObject.PERMANENT |
                           ScriptableObject.DONTENUM  |
                           (setter != null ? 0
                                           : ScriptableObject.READONLY);
                ((ScriptableObject) proto).defineProperty(name, null,
                                                          method, setter,
                                                          attr);
                continue;
            }

            if (isStatic && !Modifier.isStatic(method.getModifiers())) {
                throw Context.reportRuntimeError(
                        "jsStaticFunction must be used with static method.");
            }

            FunctionObject f = new FunctionObject(name, method, proto);
            if (f.isVarArgsConstructor()) {
                throw Context.reportRuntimeError1
                    ("msg.varargs.fun", ctorMember.getName());
            }
            defineProperty(isStatic ? ctor : proto, name, f, DONTENUM);
            if (sealed) {
                f.sealObject();
            }
        }

        // Call user code to complete initialization if necessary.
        if (finishInit != null) {
            Object[] finishArgs = { scope, ctor, proto };
            finishInit.invoke(null, finishArgs);
        }

        // Seal the object if necessary.
        if (sealed) {
            ctor.sealObject();
            if (proto instanceof ScriptableObject) {
                ((ScriptableObject) proto).sealObject();
            }
        }

        return ctor;
    }

    private static Member findAnnotatedMember(AccessibleObject[] members,
                                              Class<? extends Annotation> annotation) {
        for (AccessibleObject member : members) {
            if (member.isAnnotationPresent(annotation)) {
                return (Member) member;
            }
        }
        return null;
    }

    private static Method findSetterMethod(Method[] methods,
                                           String name,
                                           String prefix) {
        String newStyleName = "set"
                + Character.toUpperCase(name.charAt(0))
                + name.substring(1);
        for (Method method : methods) {
            JSSetter annotation = method.getAnnotation(JSSetter.class);
            if (annotation != null) {
                if (name.equals(annotation.value()) ||
                        ("".equals(annotation.value()) && newStyleName.equals(method.getName()))) {
                    return method;
                }
            }
        }
        String oldStyleName = prefix + name;
        for (Method method : methods) {
            if (oldStyleName.equals(method.getName())) {
                return method;
            }
        }
        return null;
    }

    private static String getPropertyName(String methodName,
                                          String prefix,
                                          Annotation annotation) {
        if (prefix != null) {
            return methodName.substring(prefix.length());
        }
        String propName = null;
        if (annotation instanceof JSGetter) {
            propName = ((JSGetter) annotation).value();
            if (propName == null || propName.length() == 0) {
                if (methodName.length() > 3 && methodName.startsWith("get")) {
                    propName = methodName.substring(3);
                    if (Character.isUpperCase(propName.charAt(0))) {
                        if (propName.length() == 1) {
                            propName = propName.toLowerCase();
                        } else if (!Character.isUpperCase(propName.charAt(1))){
                            propName = Character.toLowerCase(propName.charAt(0))
                                    + propName.substring(1);
                        }
                    }
                }
            }
        } else if (annotation instanceof JSFunction) {
            propName = ((JSFunction) annotation).value();
        } else if (annotation instanceof JSStaticFunction) {
            propName = ((JSStaticFunction) annotation).value();
        }
        if (propName == null || propName.length() == 0) {
            propName = methodName;
        }
        return propName;
    }

    @SuppressWarnings({"unchecked"})
    private static <T extends Scriptable> Class<T> extendsScriptable(Class<?> c)
    {
        if (ScriptRuntime.ScriptableClass.isAssignableFrom(c))
            return (Class<T>) c;
        return null;
    }

    /**
     * Define a JavaScript property.
     *
     * Creates the property with an initial value and sets its attributes.
     *
     * @param propertyName the name of the property to define.
     * @param value the initial value of the property
     * @param attributes the attributes of the JavaScript property
     * @see org.mozilla.javascript.Scriptable#put(String, Scriptable, Object)
     */
    public void defineProperty(String propertyName, Object value,
                               int attributes)
    {
        checkNotSealed(propertyName, 0);
        put(propertyName, this, value);
        setAttributes(propertyName, attributes);
    }

    /**
     * A version of defineProperty that uses a Symbol key.
     * @param key symbol of the property to define.
     * @param value the initial value of the property
     * @param attributes the attributes of the JavaScript property
     */
    public void defineProperty(Symbol key, Object value,
                               int attributes)
    {
        checkNotSealed(key, 0);
        put(key, this, value);
        setAttributes(key, attributes);
    }

    /**
     * Utility method to add properties to arbitrary Scriptable object.
     * If destination is instance of ScriptableObject, calls
     * defineProperty there, otherwise calls put in destination
     * ignoring attributes
     * @param destination ScriptableObject to define the property on
     * @param propertyName the name of the property to define.
     * @param value the initial value of the property
     * @param attributes the attributes of the JavaScript property
     */
    public static void defineProperty(Scriptable destination,
                                      String propertyName, Object value,
                                      int attributes)
    {
        if (!(destination instanceof ScriptableObject)) {
            destination.put(propertyName, destination, value);
            return;
        }
        ScriptableObject so = (ScriptableObject)destination;
        so.defineProperty(propertyName, value, attributes);
    }

    /**
     * Utility method to add properties to arbitrary Scriptable object.
     * If destination is instance of ScriptableObject, calls
     * defineProperty there, otherwise calls put in destination
     * ignoring attributes
     * @param destination ScriptableObject to define the property on
     * @param propertyName the name of the property to define.
     */
    public static void defineConstProperty(Scriptable destination,
                                           String propertyName)
    {
        if (destination instanceof ConstProperties) {
            ConstProperties cp = (ConstProperties)destination;
            cp.defineConst(propertyName, destination);
        } else
            defineProperty(destination, propertyName, Undefined.instance, CONST);
    }

    /**
     * Define a JavaScript property with getter and setter side effects.
     *
     * If the setter is not found, the attribute READONLY is added to
     * the given attributes. <p>
     *
     * The getter must be a method with zero parameters, and the setter, if
     * found, must be a method with one parameter.<p>
     *
     * @param propertyName the name of the property to define. This name
     *                    also affects the name of the setter and getter
     *                    to search for. If the propertyId is "foo", then
     *                    <code>clazz</code> will be searched for "getFoo"
     *                    and "setFoo" methods.
     * @param clazz the Java class to search for the getter and setter
     * @param attributes the attributes of the JavaScript property
     * @see org.mozilla.javascript.Scriptable#put(String, Scriptable, Object)
     */
    public void defineProperty(String propertyName, Class<?> clazz,
                               int attributes)
    {
        int length = propertyName.length();
        if (length == 0) throw new IllegalArgumentException();
        char[] buf = new char[3 + length];
        propertyName.getChars(0, length, buf, 3);
        buf[3] = Character.toUpperCase(buf[3]);
        buf[0] = 'g';
        buf[1] = 'e';
        buf[2] = 't';
        String getterName = new String(buf);
        buf[0] = 's';
        String setterName = new String(buf);

        Method[] methods = FunctionObject.getMethodList(clazz);
        Method getter = FunctionObject.findSingleMethod(methods, getterName);
        Method setter = FunctionObject.findSingleMethod(methods, setterName);
        if (setter == null)
            attributes |= ScriptableObject.READONLY;
        defineProperty(propertyName, null, getter,
                       setter == null ? null : setter, attributes);
    }

    /**
     * Define a JavaScript property.
     *
     * Use this method only if you wish to define getters and setters for
     * a given property in a ScriptableObject. To create a property without
     * special getter or setter side effects, use
     * <code>defineProperty(String,int)</code>.
     *
     * If <code>setter</code> is null, the attribute READONLY is added to
     * the given attributes.<p>
     *
     * Several forms of getters or setters are allowed. In all cases the
     * type of the value parameter can be any one of the following types:
     * Object, String, boolean, Scriptable, byte, short, int, long, float,
     * or double. The runtime will perform appropriate conversions based
     * upon the type of the parameter (see description in FunctionObject).
     * The first forms are nonstatic methods of the class referred to
     * by 'this':
     * <pre>
     * Object getFoo();
     * void setFoo(SomeType value);</pre>
     * Next are static methods that may be of any class; the object whose
     * property is being accessed is passed in as an extra argument:
     * <pre>
     * static Object getFoo(Scriptable obj);
     * static void setFoo(Scriptable obj, SomeType value);</pre>
     * Finally, it is possible to delegate to another object entirely using
     * the <code>delegateTo</code> parameter. In this case the methods are
     * nonstatic methods of the class delegated to, and the object whose
     * property is being accessed is passed in as an extra argument:
     * <pre>
     * Object getFoo(Scriptable obj);
     * void setFoo(Scriptable obj, SomeType value);</pre>
     *
     * @param propertyName the name of the property to define.
     * @param delegateTo an object to call the getter and setter methods on,
     *                   or null, depending on the form used above.
     * @param getter the method to invoke to get the value of the property
     * @param setter the method to invoke to set the value of the property
     * @param attributes the attributes of the JavaScript property
     */
    public void defineProperty(String propertyName, Object delegateTo,
                               Method getter, Method setter, int attributes)
    {
        MemberBox getterBox = null;
        if (getter != null) {
            getterBox = new MemberBox(getter);

            boolean delegatedForm;
            if (!Modifier.isStatic(getter.getModifiers())) {
                delegatedForm = (delegateTo != null);
                getterBox.delegateTo = delegateTo;
            } else {
                delegatedForm = true;
                // Ignore delegateTo for static getter but store
                // non-null delegateTo indicator.
                getterBox.delegateTo = Void.TYPE;
            }

            String errorId = null;
            Class<?>[] parmTypes = getter.getParameterTypes();
            if (parmTypes.length == 0) {
                if (delegatedForm) {
                    errorId = "msg.obj.getter.parms";
                }
            } else if (parmTypes.length == 1) {
                Object argType = parmTypes[0];
                // Allow ScriptableObject for compatibility
                if (!(argType == ScriptRuntime.ScriptableClass ||
                      argType == ScriptRuntime.ScriptableObjectClass))
                {
                    errorId = "msg.bad.getter.parms";
                } else if (!delegatedForm) {
                    errorId = "msg.bad.getter.parms";
                }
            } else {
                errorId = "msg.bad.getter.parms";
            }
            if (errorId != null) {
                throw Context.reportRuntimeError1(errorId, getter.toString());
            }
        }

        MemberBox setterBox = null;
        if (setter != null) {
            if (setter.getReturnType() != Void.TYPE)
                throw Context.reportRuntimeError1("msg.setter.return",
                                                  setter.toString());

            setterBox = new MemberBox(setter);

            boolean delegatedForm;
            if (!Modifier.isStatic(setter.getModifiers())) {
                delegatedForm = (delegateTo != null);
                setterBox.delegateTo = delegateTo;
            } else {
                delegatedForm = true;
                // Ignore delegateTo for static setter but store
                // non-null delegateTo indicator.
                setterBox.delegateTo = Void.TYPE;
            }

            String errorId = null;
            Class<?>[] parmTypes = setter.getParameterTypes();
            if (parmTypes.length == 1) {
                if (delegatedForm) {
                    errorId = "msg.setter2.expected";
                }
            } else if (parmTypes.length == 2) {
                Object argType = parmTypes[0];
                // Allow ScriptableObject for compatibility
                if (!(argType == ScriptRuntime.ScriptableClass ||
                      argType == ScriptRuntime.ScriptableObjectClass))
                {
                    errorId = "msg.setter2.parms";
                } else if (!delegatedForm) {
                    errorId = "msg.setter1.parms";
                }
            } else {
                errorId = "msg.setter.parms";
            }
            if (errorId != null) {
                throw Context.reportRuntimeError1(errorId, setter.toString());
            }
        }

        GetterSlot gslot = (GetterSlot)slotMap.get(propertyName, 0,
            SlotAccess.MODIFY_GETTER_SETTER);
        gslot.setAttributes(attributes);
        gslot.getter = getterBox;
        gslot.setter = setterBox;
    }

    /**
     * Defines one or more properties on this object.
     *
     * @param cx the current Context
     * @param props a map of property ids to property descriptors
     */
    public void defineOwnProperties(Context cx, ScriptableObject props) {
        Object[] ids = props.getIds(false, true);
        ScriptableObject[] descs = new ScriptableObject[ids.length];
        for (int i = 0, len = ids.length; i < len; ++i) {
            Object descObj = ScriptRuntime.getObjectElem(props, ids[i], cx);
            ScriptableObject desc = ensureScriptableObject(descObj);
            checkPropertyDefinition(desc);
            descs[i] = desc;
        }
        for (int i = 0, len = ids.length; i < len; ++i) {
            defineOwnProperty(cx, ids[i], descs[i]);
        }
    }

    /**
     * Defines a property on an object.
     *
     * @param cx the current Context
     * @param id the name/index of the property
     * @param desc the new property descriptor, as described in 8.6.1
     */
    public void defineOwnProperty(Context cx, Object id, ScriptableObject desc) {
        checkPropertyDefinition(desc);
        defineOwnProperty(cx, id, desc, true);
    }

    /**
     * Defines a property on an object.
     *
     * Based on [[DefineOwnProperty]] from 8.12.10 of the spec.
     *
     * @param cx the current Context
     * @param id the name/index of the property
     * @param desc the new property descriptor, as described in 8.6.1
     * @param checkValid whether to perform validity checks
     */
    protected void defineOwnProperty(Context cx, Object id, ScriptableObject desc,
                                     boolean checkValid) {

        Slot slot = getSlot(cx, id, SlotAccess.QUERY);
        boolean isNew = slot == null;

        if (checkValid) {
            ScriptableObject current = slot == null ?
                    null : slot.getPropertyDescriptor(cx, this);
            checkPropertyChange(id, current, desc);
        }

        boolean isAccessor = isAccessorDescriptor(desc);
        final int attributes;

        if (slot == null) { // new slot
            slot = getSlot(cx, id, isAccessor ? SlotAccess.MODIFY_GETTER_SETTER : SlotAccess.MODIFY);
            attributes = applyDescriptorToAttributeBitset(DONTENUM|READONLY|PERMANENT, desc);
        } else {
            attributes = applyDescriptorToAttributeBitset(slot.getAttributes(), desc);
        }

        if (isAccessor) {
            if ( !(slot instanceof GetterSlot) ) {
                slot = getSlot(cx, id, SlotAccess.MODIFY_GETTER_SETTER);
            }

            GetterSlot gslot = (GetterSlot) slot;

            Object getter = getProperty(desc, "get");
            if (getter != NOT_FOUND) {
                gslot.getter = getter;
            }
            Object setter = getProperty(desc, "set");
            if (setter != NOT_FOUND) {
                gslot.setter = setter;
            }

            gslot.value = Undefined.instance;
            gslot.setAttributes(attributes);
        } else {
            if (slot instanceof GetterSlot && isDataDescriptor(desc)) {
                slot = getSlot(cx, id, SlotAccess.CONVERT_ACCESSOR_TO_DATA);
            }

            Object value = getProperty(desc, "value");
            if (value != NOT_FOUND) {
                slot.value = value;
            } else if (isNew) {
                slot.value = Undefined.instance;
            }
            slot.setAttributes(attributes);
        }
    }

    protected void checkPropertyDefinition(ScriptableObject desc) {
        Object getter = getProperty(desc, "get");
        if (getter != NOT_FOUND && getter != Undefined.instance
                && !(getter instanceof Callable)) {
            throw ScriptRuntime.notFunctionError(getter);
        }
        Object setter = getProperty(desc, "set");
        if (setter != NOT_FOUND && setter != Undefined.instance
                && !(setter instanceof Callable)) {
            throw ScriptRuntime.notFunctionError(setter);
        }
        if (isDataDescriptor(desc) && isAccessorDescriptor(desc)) {
            throw ScriptRuntime.typeError0("msg.both.data.and.accessor.desc");
        }
    }

    protected void checkPropertyChange(Object id, ScriptableObject current,
                                       ScriptableObject desc) {
        if (current == null) { // new property
            if (!isExtensible()) throw ScriptRuntime.typeError0("msg.not.extensible");
        } else {
            if (isFalse(current.get("configurable", current))) {
                if (isTrue(getProperty(desc, "configurable")))
                    throw ScriptRuntime.typeError1(
                        "msg.change.configurable.false.to.true", id);
                if (isTrue(current.get("enumerable", current)) != isTrue(getProperty(desc, "enumerable")))
                    throw ScriptRuntime.typeError1(
                        "msg.change.enumerable.with.configurable.false", id);
                boolean isData = isDataDescriptor(desc);
                boolean isAccessor = isAccessorDescriptor(desc);
                if (!isData && !isAccessor) {
                    // no further validation required for generic descriptor
                } else if (isData && isDataDescriptor(current)) {
                    if (isFalse(current.get("writable", current))) {
                        if (isTrue(getProperty(desc, "writable")))
                            throw ScriptRuntime.typeError1(
                                "msg.change.writable.false.to.true.with.configurable.false", id);

                        if (!sameValue(getProperty(desc, "value"), current.get("value", current)))
                            throw ScriptRuntime.typeError1(
                                "msg.change.value.with.writable.false", id);
                    }
                } else if (isAccessor && isAccessorDescriptor(current)) {
                    if (!sameValue(getProperty(desc, "set"), current.get("set", current)))
                        throw ScriptRuntime.typeError1(
                            "msg.change.setter.with.configurable.false", id);

                    if (!sameValue(getProperty(desc, "get"), current.get("get", current)))
                        throw ScriptRuntime.typeError1(
                            "msg.change.getter.with.configurable.false", id);
                } else {
                    if (isDataDescriptor(current))
                        throw ScriptRuntime.typeError1(
                            "msg.change.property.data.to.accessor.with.configurable.false", id);
                    throw ScriptRuntime.typeError1(
                        "msg.change.property.accessor.to.data.with.configurable.false", id);
                }
            }
        }
    }

    protected static boolean isTrue(Object value) {
        return (value != NOT_FOUND) && ScriptRuntime.toBoolean(value);
    }

    protected static boolean isFalse(Object value) {
        return !isTrue(value);
    }

    /**
     * Implements SameValue as described in ES5 9.12, additionally checking
     * if new value is defined.
     * @param newValue the new value
     * @param currentValue the current value
     * @return true if values are the same as defined by ES5 9.12
     */
    protected boolean sameValue(Object newValue, Object currentValue) {
        if (newValue == NOT_FOUND) {
            return true;
        }
        if (currentValue == NOT_FOUND) {
            currentValue = Undefined.instance;
        }
        // Special rules for numbers: NaN is considered the same value,
        // while zeroes with different signs are considered different.
        if (currentValue instanceof Number && newValue instanceof Number) {
            double d1 = ((Number)currentValue).doubleValue();
            double d2 = ((Number)newValue).doubleValue();
            if (Double.isNaN(d1) && Double.isNaN(d2)) {
                return true;
            }
            if (d1 == 0.0 && Double.doubleToLongBits(d1) != Double.doubleToLongBits(d2)) {
                return false;
            }
        }
        return ScriptRuntime.shallowEq(currentValue, newValue);
    }

    protected int applyDescriptorToAttributeBitset(int attributes,
                                                   ScriptableObject desc)
    {
        Object enumerable = getProperty(desc, "enumerable");
        if (enumerable != NOT_FOUND) {
            attributes = ScriptRuntime.toBoolean(enumerable)
                    ? attributes & ~DONTENUM : attributes | DONTENUM;
        }

        Object writable = getProperty(desc, "writable");
        if (writable != NOT_FOUND) {
            attributes = ScriptRuntime.toBoolean(writable)
                    ? attributes & ~READONLY : attributes | READONLY;
        }

        Object configurable = getProperty(desc, "configurable");
        if (configurable != NOT_FOUND) {
            attributes = ScriptRuntime.toBoolean(configurable)
                    ? attributes & ~PERMANENT : attributes | PERMANENT;
        }

        return attributes;
    }

    /**
     * Implements IsDataDescriptor as described in ES5 8.10.2
     * @param desc a property descriptor
     * @return true if this is a data descriptor.
     */
    protected boolean isDataDescriptor(ScriptableObject desc) {
        return hasProperty(desc, "value") || hasProperty(desc, "writable");
    }

    /**
     * Implements IsAccessorDescriptor as described in ES5 8.10.1
     * @param desc a property descriptor
     * @return true if this is an accessor descriptor.
     */
    protected boolean isAccessorDescriptor(ScriptableObject desc) {
        return hasProperty(desc, "get") || hasProperty(desc, "set");
    }

    /**
     * Implements IsGenericDescriptor as described in ES5 8.10.3
     * @param desc a property descriptor
     * @return true if this is a generic descriptor.
     */
    protected boolean isGenericDescriptor(ScriptableObject desc) {
        return !isDataDescriptor(desc) && !isAccessorDescriptor(desc);
    }

    protected static Scriptable ensureScriptable(Object arg) {
        if ( !(arg instanceof Scriptable) )
            throw ScriptRuntime.typeError1("msg.arg.not.object", ScriptRuntime.typeof(arg));
        return (Scriptable) arg;
    }

    protected static SymbolScriptable ensureSymbolScriptable(Object arg) {
        if ( !(arg instanceof SymbolScriptable) )
            throw ScriptRuntime.typeError1("msg.object.not.symbolscriptable", ScriptRuntime.typeof(arg));
        return (SymbolScriptable) arg;
    }

    protected static ScriptableObject ensureScriptableObject(Object arg) {
        if ( !(arg instanceof ScriptableObject) )
            throw ScriptRuntime.typeError1("msg.arg.not.object", ScriptRuntime.typeof(arg));
        return (ScriptableObject) arg;
    }

    /**
     * Search for names in a class, adding the resulting methods
     * as properties.
     *
     * <p> Uses reflection to find the methods of the given names. Then
     * FunctionObjects are constructed from the methods found, and
     * are added to this object as properties with the given names.
     *
     * @param names the names of the Methods to add as function properties
     * @param clazz the class to search for the Methods
     * @param attributes the attributes of the new properties
     * @see org.mozilla.javascript.FunctionObject
     */
    public void defineFunctionProperties(String[] names, Class<?> clazz,
                                         int attributes)
    {
        Method[] methods = FunctionObject.getMethodList(clazz);
        for (int i=0; i < names.length; i++) {
            String name = names[i];
            Method m = FunctionObject.findSingleMethod(methods, name);
            if (m == null) {
                throw Context.reportRuntimeError2(
                    "msg.method.not.found", name, clazz.getName());
            }
            FunctionObject f = new FunctionObject(name, m, this);
            defineProperty(name, f, attributes);
        }
    }

    /**
     * Get the Object.prototype property.
     * See ECMA 15.2.4.
     * @param scope an object in the scope chain
     */
    public static Scriptable getObjectPrototype(Scriptable scope) {
        return TopLevel.getBuiltinPrototype(getTopLevelScope(scope),
                TopLevel.Builtins.Object);
    }

    /**
     * Get the Function.prototype property.
     * See ECMA 15.3.4.
     * @param scope an object in the scope chain
     */
    public static Scriptable getFunctionPrototype(Scriptable scope) {
        return TopLevel.getBuiltinPrototype(getTopLevelScope(scope),
                TopLevel.Builtins.Function);
    }

    public static Scriptable getGeneratorFunctionPrototype(Scriptable scope) {
        return TopLevel.getBuiltinPrototype(getTopLevelScope(scope),
                TopLevel.Builtins.GeneratorFunction);
    }

    public static Scriptable getArrayPrototype(Scriptable scope) {
        return TopLevel.getBuiltinPrototype(getTopLevelScope(scope),
                TopLevel.Builtins.Array);
    }

    /**
     * Get the prototype for the named class.
     *
     * For example, <code>getClassPrototype(s, "Date")</code> will first
     * walk up the parent chain to find the outermost scope, then will
     * search that scope for the Date constructor, and then will
     * return Date.prototype. If any of the lookups fail, or
     * the prototype is not a JavaScript object, then null will
     * be returned.
     *
     * @param scope an object in the scope chain
     * @param className the name of the constructor
     * @return the prototype for the named class, or null if it
     *         cannot be found.
     */
    public static Scriptable getClassPrototype(Scriptable scope,
                                               String className)
    {
        scope = getTopLevelScope(scope);
        Object ctor = getProperty(scope, className);
        Object proto;
        if (ctor instanceof BaseFunction) {
            proto = ((BaseFunction)ctor).getPrototypeProperty();
        } else if (ctor instanceof Scriptable) {
            Scriptable ctorObj = (Scriptable)ctor;
            proto = ctorObj.get("prototype", ctorObj);
        } else {
            return null;
        }
        if (proto instanceof Scriptable) {
            return (Scriptable)proto;
        }
        return null;
    }

    /**
     * Get the global scope.
     *
     * <p>Walks the parent scope chain to find an object with a null
     * parent scope (the global object).
     *
     * @param obj a JavaScript object
     * @return the corresponding global scope
     */
    public static Scriptable getTopLevelScope(Scriptable obj)
    {
        for (;;) {
            Scriptable parent = obj.getParentScope();
            if (parent == null) {
                return obj;
            }
            obj = parent;
        }
    }

    public boolean isExtensible() {
      return isExtensible;
    }

    public void preventExtensions() {
      isExtensible = false;
    }

    /**
     * Seal this object.
     *
     * It is an error to add properties to or delete properties from
     * a sealed object. It is possible to change the value of an
     * existing property. Once an object is sealed it may not be unsealed.
     *
     * @since 1.4R3
     */
    public void sealObject() {
        if (!isSealed) {
            final long stamp = slotMap.readLock();
            try {
                for (Slot slot : slotMap) {
                    Object value = slot.value;
                    if (value instanceof LazilyLoadedCtor) {
                        LazilyLoadedCtor initializer = (LazilyLoadedCtor) value;
                        try {
                            initializer.init();
                        } finally {
                            slot.value = initializer.getValue();
                        }
                    }
                }
                isSealed = true;
            } finally {
                slotMap.unlockRead(stamp);
            }
        }
    }

    /**
     * Return true if this object is sealed.
     *
     * @return true if sealed, false otherwise.
     * @since 1.4R3
     * @see #sealObject()
     */
    public final boolean isSealed() {
        return isSealed;
    }

    private void checkNotSealed(Object key, int index)
    {
        if (!isSealed())
            return;

        String str = (key != null) ? key.toString() : Integer.toString(index);
        throw Context.reportRuntimeError1("msg.modify.sealed", str);
    }

    /**
     * Gets a named property from an object or any object in its prototype chain.
     * <p>
     * Searches the prototype chain for a property named <code>name</code>.
     * <p>
     * @param obj a JavaScript object
     * @param name a property name
     * @return the value of a property with name <code>name</code> found in
     *         <code>obj</code> or any object in its prototype chain, or
     *         <code>Scriptable.NOT_FOUND</code> if not found
     * @since 1.5R2
     */
    public static Object getProperty(Scriptable obj, String name)
    {
        Scriptable start = obj;
        Object result;
        do {
            result = obj.get(name, start);
            if (result != Scriptable.NOT_FOUND)
                break;
            obj = obj.getPrototype();
        } while (obj != null);
        return result;
    }

    /**
     * This is a version of getProperty that works with Symbols.
     */
    public static Object getProperty(Scriptable obj, Symbol key)
    {
        Scriptable start = obj;
        Object result;
        do {
            result = ensureSymbolScriptable(obj).get(key, start);
            if (result != Scriptable.NOT_FOUND)
                break;
            obj = obj.getPrototype();
        } while (obj != null);
        return result;
    }

    /**
     * Gets an indexed property from an object or any object in its prototype
     * chain and coerces it to the requested Java type.
     * <p>
     * Searches the prototype chain for a property with integral index
     * <code>index</code>. Note that if you wish to look for properties with numerical
     * but non-integral indicies, you should use getProperty(Scriptable,String) with
     * the string value of the index.
     * <p>
     * @param s a JavaScript object
     * @param index an integral index
     * @param type the required Java type of the result
     * @return the value of a property with name <code>name</code> found in
     *         <code>obj</code> or any object in its prototype chain, or
     *         null if not found. Note that it does not return
     *         {@link Scriptable#NOT_FOUND} as it can ordinarily not be
     *         converted to most of the types.
     * @since 1.7R3
     */
    public static <T> T getTypedProperty(Scriptable s, int index, Class<T> type) {
        Object val = getProperty(s, index);
        if(val == Scriptable.NOT_FOUND) {
            val = null;
        }
        return type.cast(Context.jsToJava(val, type));
    }

    /**
     * Gets an indexed property from an object or any object in its prototype chain.
     * <p>
     * Searches the prototype chain for a property with integral index
     * <code>index</code>. Note that if you wish to look for properties with numerical
     * but non-integral indicies, you should use getProperty(Scriptable,String) with
     * the string value of the index.
     * <p>
     * @param obj a JavaScript object
     * @param index an integral index
     * @return the value of a property with index <code>index</code> found in
     *         <code>obj</code> or any object in its prototype chain, or
     *         <code>Scriptable.NOT_FOUND</code> if not found
     * @since 1.5R2
     */
    public static Object getProperty(Scriptable obj, int index)
    {
        Scriptable start = obj;
        Object result;
        do {
            result = obj.get(index, start);
            if (result != Scriptable.NOT_FOUND)
                break;
            obj = obj.getPrototype();
        } while (obj != null);
        return result;
    }

    /**
     * Gets a named property from an object or any object in its prototype chain
     * and coerces it to the requested Java type.
     * <p>
     * Searches the prototype chain for a property named <code>name</code>.
     * <p>
     * @param s a JavaScript object
     * @param name a property name
     * @param type the required Java type of the result
     * @return the value of a property with name <code>name</code> found in
     *         <code>obj</code> or any object in its prototype chain, or
     *         null if not found. Note that it does not return
     *         {@link Scriptable#NOT_FOUND} as it can ordinarily not be
     *         converted to most of the types.
     * @since 1.7R3
     */
    public static <T> T getTypedProperty(Scriptable s, String name, Class<T> type) {
        Object val = getProperty(s, name);
        if(val == Scriptable.NOT_FOUND) {
            val = null;
        }
        return type.cast(Context.jsToJava(val, type));
    }

    /**
     * Returns whether a named property is defined in an object or any object
     * in its prototype chain.
     * <p>
     * Searches the prototype chain for a property named <code>name</code>.
     * <p>
     * @param obj a JavaScript object
     * @param name a property name
     * @return the true if property was found
     * @since 1.5R2
     */
    public static boolean hasProperty(Scriptable obj, String name)
    {
        return null != getBase(obj, name);
    }

    /**
     * If hasProperty(obj, name) would return true, then if the property that
     * was found is compatible with the new property, this method just returns.
     * If the property is not compatible, then an exception is thrown.
     *
     * A property redefinition is incompatible if the first definition was a
     * const declaration or if this one is.  They are compatible only if neither
     * was const.
     */
    public static void redefineProperty(Scriptable obj, String name,
                                        boolean isConst)
    {
        Scriptable base = getBase(obj, name);
        if (base == null)
            return;
        if (base instanceof ConstProperties) {
            ConstProperties cp = (ConstProperties)base;

            if (cp.isConst(name))
                throw ScriptRuntime.typeError1("msg.const.redecl", name);
        }
        if (isConst)
            throw ScriptRuntime.typeError1("msg.var.redecl", name);
    }
    /**
     * Returns whether an indexed property is defined in an object or any object
     * in its prototype chain.
     * <p>
     * Searches the prototype chain for a property with index <code>index</code>.
     * <p>
     * @param obj a JavaScript object
     * @param index a property index
     * @return the true if property was found
     * @since 1.5R2
     */
    public static boolean hasProperty(Scriptable obj, int index)
    {
        return null != getBase(obj, index);
    }

    /**
     * A version of hasProperty for properties with Symbol keys.
     */
    public static boolean hasProperty(Scriptable obj, Symbol key)
    {
        return null != getBase(obj, key);
    }

    /**
     * Puts a named property in an object or in an object in its prototype chain.
     * <p>
     * Searches for the named property in the prototype chain. If it is found,
     * the value of the property in <code>obj</code> is changed through a call
     * to {@link Scriptable#put(String, Scriptable, Object)} on the
     * prototype passing <code>obj</code> as the <code>start</code> argument.
     * This allows the prototype to veto the property setting in case the
     * prototype defines the property with [[ReadOnly]] attribute. If the
     * property is not found, it is added in <code>obj</code>.
     * @param obj a JavaScript object
     * @param name a property name
     * @param value any JavaScript value accepted by Scriptable.put
     * @since 1.5R2
     */
    public static void putProperty(Scriptable obj, String name, Object value)
    {
        Scriptable base = getBase(obj, name);
        if (base == null)
            base = obj;
        base.put(name, obj, value);
    }

    /**
     * This is a version of putProperty for Symbol keys.
     */
    public static void putProperty(Scriptable obj, Symbol key, Object value)
    {
        Scriptable base = getBase(obj, key);
        if (base == null)
            base = obj;
        ensureSymbolScriptable(base).put(key, obj, value);
    }

    /**
     * Puts a named property in an object or in an object in its prototype chain.
     * <p>
     * Searches for the named property in the prototype chain. If it is found,
     * the value of the property in <code>obj</code> is changed through a call
     * to {@link Scriptable#put(String, Scriptable, Object)} on the
     * prototype passing <code>obj</code> as the <code>start</code> argument.
     * This allows the prototype to veto the property setting in case the
     * prototype defines the property with [[ReadOnly]] attribute. If the
     * property is not found, it is added in <code>obj</code>.
     * @param obj a JavaScript object
     * @param name a property name
     * @param value any JavaScript value accepted by Scriptable.put
     * @since 1.5R2
     */
    public static void putConstProperty(Scriptable obj, String name, Object value)
    {
        Scriptable base = getBase(obj, name);
        if (base == null)
            base = obj;
        if (base instanceof ConstProperties)
            ((ConstProperties)base).putConst(name, obj, value);
    }

    /**
     * Puts an indexed property in an object or in an object in its prototype chain.
     * <p>
     * Searches for the indexed property in the prototype chain. If it is found,
     * the value of the property in <code>obj</code> is changed through a call
     * to {@link Scriptable#put(int, Scriptable, Object)} on the prototype
     * passing <code>obj</code> as the <code>start</code> argument. This allows
     * the prototype to veto the property setting in case the prototype defines
     * the property with [[ReadOnly]] attribute. If the property is not found,
     * it is added in <code>obj</code>.
     * @param obj a JavaScript object
     * @param index a property index
     * @param value any JavaScript value accepted by Scriptable.put
     * @since 1.5R2
     */
    public static void putProperty(Scriptable obj, int index, Object value)
    {
        Scriptable base = getBase(obj, index);
        if (base == null)
            base = obj;
        base.put(index, obj, value);
    }

    /**
     * Removes the property from an object or its prototype chain.
     * <p>
     * Searches for a property with <code>name</code> in obj or
     * its prototype chain. If it is found, the object's delete
     * method is called.
     * @param obj a JavaScript object
     * @param name a property name
     * @return true if the property doesn't exist or was successfully removed
     * @since 1.5R2
     */
    public static boolean deleteProperty(Scriptable obj, String name)
    {
        Scriptable base = getBase(obj, name);
        if (base == null)
            return true;
        base.delete(name);
        return !base.has(name, obj);
    }

    /**
     * Removes the property from an object or its prototype chain.
     * <p>
     * Searches for a property with <code>index</code> in obj or
     * its prototype chain. If it is found, the object's delete
     * method is called.
     * @param obj a JavaScript object
     * @param index a property index
     * @return true if the property doesn't exist or was successfully removed
     * @since 1.5R2
     */
    public static boolean deleteProperty(Scriptable obj, int index)
    {
        Scriptable base = getBase(obj, index);
        if (base == null)
            return true;
        base.delete(index);
        return !base.has(index, obj);
    }

    /**
     * Returns an array of all ids from an object and its prototypes.
     * <p>
     * @param obj a JavaScript object
     * @return an array of all ids from all object in the prototype chain.
     *         If a given id occurs multiple times in the prototype chain,
     *         it will occur only once in this list.
     * @since 1.5R2
     */
    public static Object[] getPropertyIds(Scriptable obj)
    {
        if (obj == null) {
            return ScriptRuntime.emptyArgs;
        }
        Object[] result = obj.getIds();
        ObjToIntMap map = null;
        for (;;) {
            obj = obj.getPrototype();
            if (obj == null) {
                break;
            }
            Object[] ids = obj.getIds();
            if (ids.length == 0) {
                continue;
            }
            if (map == null) {
                if (result.length == 0) {
                    result = ids;
                    continue;
                }
                map = new ObjToIntMap(result.length + ids.length);
                for (int i = 0; i != result.length; ++i) {
                    map.intern(result[i]);
                }
                result = null; // Allow to GC the result
            }
            for (int i = 0; i != ids.length; ++i) {
                map.intern(ids[i]);
            }
        }
        if (map != null) {
            result = map.getKeys();
        }
        return result;
    }

    /**
     * Call a method of an object.
     * @param obj the JavaScript object
     * @param methodName the name of the function property
     * @param args the arguments for the call
     *
     * @see Context#getCurrentContext()
     */
    public static Object callMethod(Scriptable obj, String methodName,
                                    Object[] args)
    {
        return callMethod(null, obj, methodName, args);
    }

    /**
     * Call a method of an object.
     * @param cx the Context object associated with the current thread.
     * @param obj the JavaScript object
     * @param methodName the name of the function property
     * @param args the arguments for the call
     */
    public static Object callMethod(Context cx, Scriptable obj,
                                    String methodName,
                                    Object[] args)
    {
        Object funObj = getProperty(obj, methodName);
        if (!(funObj instanceof Function)) {
            throw ScriptRuntime.notFunctionError(obj, methodName);
        }
        Function fun = (Function)funObj;
        // XXX: What should be the scope when calling funObj?
        // The following favor scope stored in the object on the assumption
        // that is more useful especially under dynamic scope setup.
        // An alternative is to check for dynamic scope flag
        // and use ScriptableObject.getTopLevelScope(fun) if the flag is not
        // set. But that require access to Context and messy code
        // so for now it is not checked.
        Scriptable scope = ScriptableObject.getTopLevelScope(obj);
        if (cx != null) {
            return fun.call(cx, scope, obj, args);
        }
        return Context.call(null, fun, scope, obj, args);
    }

    private static Scriptable getBase(Scriptable obj, String name)
    {
        do {
            if (obj.has(name, obj))
                break;
            obj = obj.getPrototype();
        } while(obj != null);
        return obj;
    }

    private static Scriptable getBase(Scriptable obj, int index)
    {
        do {
            if (obj.has(index, obj))
                break;
            obj = obj.getPrototype();
        } while(obj != null);
        return obj;
    }

    private static Scriptable getBase(Scriptable obj, Symbol key)
    {
        do {
            if (ensureSymbolScriptable(obj).has(key, obj))
                break;
            obj = obj.getPrototype();
        } while(obj != null);
        return obj;
    }

    /**
     * Get arbitrary application-specific value associated with this object.
     * @param key key object to select particular value.
     * @see #associateValue(Object key, Object value)
     */
    public final Object getAssociatedValue(Object key)
    {
        Map<Object,Object> h = associatedValues;
        if (h == null)
            return null;
        return h.get(key);
    }

    /**
     * Get arbitrary application-specific value associated with the top scope
     * of the given scope.
     * The method first calls {@link #getTopLevelScope(Scriptable scope)}
     * and then searches the prototype chain of the top scope for the first
     * object containing the associated value with the given key.
     *
     * @param scope the starting scope.
     * @param key key object to select particular value.
     * @see #getAssociatedValue(Object key)
     */
    public static Object getTopScopeValue(Scriptable scope, Object key)
    {
        scope = ScriptableObject.getTopLevelScope(scope);
        for (;;) {
            if (scope instanceof ScriptableObject) {
                ScriptableObject so = (ScriptableObject)scope;
                Object value = so.getAssociatedValue(key);
                if (value != null) {
                    return value;
                }
            }
            scope = scope.getPrototype();
            if (scope == null) {
                return null;
            }
        }
    }

    /**
     * Associate arbitrary application-specific value with this object.
     * Value can only be associated with the given object and key only once.
     * The method ignores any subsequent attempts to change the already
     * associated value.
     * <p> The associated values are not serialized.
     * @param key key object to select particular value.
     * @param value the value to associate
     * @return the passed value if the method is called first time for the
     * given key or old value for any subsequent calls.
     * @see #getAssociatedValue(Object key)
     */
    public synchronized final Object associateValue(Object key, Object value)
    {
        if (value == null) throw new IllegalArgumentException();
        Map<Object,Object> h = associatedValues;
        if (h == null) {
            h = new HashMap<Object,Object>();
            associatedValues = h;
        }
        return Kit.initHash(h, key, value);
    }

    /**
     *
     * @param key
     * @param index
     * @param start
     * @param value
     * @return false if this != start and no slot was found.  true if this == start
     * or this != start and a READONLY slot was found.
     */
    private boolean putImpl(Object key, int index, Scriptable start,
                            Object value)
    {
        // This method is very hot (basically called on each assignment)
        // so we inline the extensible/sealed checks below.
        Slot slot;
        if (this != start) {
            slot = slotMap.query(key, index);
            if(!isExtensible
                    && (slot == null
                        || (!(slot instanceof GetterSlot)
                                && (slot.getAttributes() & READONLY) != 0))
                    && Context.getContext().isStrictMode()) {
                throw ScriptRuntime.typeError0("msg.not.extensible");
            }
            if (slot == null) {
                return false;
            }
        } else if (!isExtensible) {
            slot = slotMap.query(key, index);
            if((slot == null
                        || (!(slot instanceof GetterSlot) && (slot.getAttributes() & READONLY) != 0))
                    && Context.getContext().isStrictMode()) {
                throw ScriptRuntime.typeError0("msg.not.extensible");
            }
            if (slot == null) {
                return true;
            }
        } else {
            if (isSealed) {
                checkNotSealed(key, index);
            }
            slot = slotMap.get(key, index, SlotAccess.MODIFY);
        }
        return slot.setValue(value, this, start);
    }


    /**
     *
     * @param name
     * @param index
     * @param start
     * @param value
     * @param constFlag EMPTY means normal put.  UNINITIALIZED_CONST means
     * defineConstProperty.  READONLY means const initialization expression.
     * @return false if this != start and no slot was found.  true if this == start
     * or this != start and a READONLY slot was found.
     */
    private boolean putConstImpl(String name, int index, Scriptable start,
                                 Object value, int constFlag)
    {
        assert (constFlag != EMPTY);
        if (!isExtensible) {
            Context cx = Context.getContext();
            if (cx.isStrictMode()) {
                throw ScriptRuntime.typeError0("msg.not.extensible");
            }
        }
        Slot slot;
        if (this != start) {
            slot = slotMap.query(name, index);
            if (slot == null) {
                return false;
            }
        } else if (!isExtensible()) {
            slot = slotMap.query(name, index);
            if (slot == null) {
                return true;
            }
        } else {
            checkNotSealed(name, index);
            // either const hoisted declaration or initialization
            slot = slotMap.get(name, index, SlotAccess.MODIFY_CONST);
            int attr = slot.getAttributes();
            if ((attr & READONLY) == 0)
                throw Context.reportRuntimeError1("msg.var.redecl", name);
            if ((attr & UNINITIALIZED_CONST) != 0) {
                slot.value = value;
                // clear the bit on const initialization
                if (constFlag != UNINITIALIZED_CONST)
                    slot.setAttributes(attr & ~UNINITIALIZED_CONST);
            }
            return true;
        }
        return slot.setValue(value, this, start);
    }

    private Slot findAttributeSlot(String name, int index, SlotAccess accessType)
    {
        Slot slot = slotMap.get(name, index, accessType);
        if (slot == null) {
            String str = (name != null ? name : Integer.toString(index));
            throw Context.reportRuntimeError1("msg.prop.not.found", str);
        }
        return slot;
    }

    private Slot findAttributeSlot(Symbol key, SlotAccess accessType)
    {
        Slot slot = slotMap.get(key, 0, accessType);
        if (slot == null) {
            throw Context.reportRuntimeError1("msg.prop.not.found", key);
        }
        return slot;
    }

    Object[] getIds(boolean getNonEnumerable, boolean getSymbols) {
        Object[] a;
        int externalLen = (externalData == null ? 0 : externalData.getArrayLength());

        if (externalLen == 0) {
            a = ScriptRuntime.emptyArgs;
        } else {
            a = new Object[externalLen];
            for (int i = 0; i < externalLen; i++) {
                a[i] = Integer.valueOf(i);
            }
        }
        if (slotMap.isEmpty()) {
            return a;
        }

        int c = externalLen;
        final long stamp = slotMap.readLock();
        try {
            for (Slot slot : slotMap) {
                if ((getNonEnumerable || (slot.getAttributes() & DONTENUM) == 0) &&
                    (getSymbols || !(slot.name instanceof Symbol))) {
                    if (c == externalLen) {
                        // Special handling to combine external array with additional properties
                        Object[] oldA = a;
                        a = new Object[slotMap.dirtySize() + externalLen];
                        if (oldA != null) {
                            System.arraycopy(oldA, 0, a, 0, externalLen);
                        }
                    }
                    a[c++] = slot.name != null
                        ? slot.name
                        : Integer.valueOf(slot.indexOrHash);
                }
            }
        } finally {
            slotMap.unlockRead(stamp);
        }

        Object[] result;
        if (c == (a.length + externalLen)) {
            result = a;
        } else {
            result = new Object[c];
            System.arraycopy(a, 0, result, 0, c);
        }

        Context cx = Context.getCurrentContext();
        if ((cx != null) && cx.hasFeature(Context.FEATURE_ENUMERATE_IDS_FIRST)) {
            // Move all the numeric IDs to the front in numeric order
            Arrays.sort(result, KEY_COMPARATOR);
        }

        return result;
    }

    private void writeObject(ObjectOutputStream out)
        throws IOException
    {
        out.defaultWriteObject();
        final long stamp = slotMap.readLock();
        try {
            int objectsCount = slotMap.dirtySize();
            if (objectsCount == 0) {
                out.writeInt(0);
            } else {
                out.writeInt(objectsCount);
                for (Slot slot : slotMap) {
                    out.writeObject(slot);
                }
            }
        } finally {
            slotMap.unlockRead(stamp);
        }
    }

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();

        int tableSize = in.readInt();
        slotMap = createSlotMap(tableSize);
        for (int i = 0; i < tableSize; i++) {
            Slot slot = (Slot)in.readObject();
            slotMap.addSlot(slot);
        }
    }

    protected ScriptableObject getOwnPropertyDescriptor(Context cx, Object id) {
        Slot slot = getSlot(cx, id, SlotAccess.QUERY);
        if (slot == null) return null;
        Scriptable scope = getParentScope();
        return slot.getPropertyDescriptor(cx, (scope == null ? this : scope));
    }

    protected Slot getSlot(Context cx, Object id, SlotAccess accessType) {
        if (id instanceof Symbol) {
            return slotMap.get(id, 0, accessType);
        }
        StringIdOrIndex s = ScriptRuntime.toStringIdOrIndex(cx, id);
        if (s.stringId == null) {
            return slotMap.get(null, s.index, accessType);
        }
        return slotMap.get(s.stringId, 0, accessType);
    }

    // Partial implementation of java.util.Map. See NativeObject for
    // a subclass that implements java.util.Map.

    public int size() {
        return slotMap.size();
    }

    public boolean isEmpty() {
        return slotMap.isEmpty();
    }


    public Object get(Object key) {
        Object value = null;
        if (key instanceof String) {
            value = get((String) key, this);
        } else if (key instanceof Symbol) {
            value = get((Symbol) key, this);
        } else if (key instanceof Number) {
            value = get(((Number) key).intValue(), this);
        }
        if (value == Scriptable.NOT_FOUND || value == Undefined.instance) {
            return null;
        } else if (value instanceof Wrapper) {
            return ((Wrapper) value).unwrap();
        } else {
            return value;
        }
    }

    private static final Comparator<Object> KEY_COMPARATOR = new KeyComparator();

    /**
     * This comparator sorts property fields in spec-compliant order. Numeric ids first, in numeric
     * order, followed by string ids, in insertion order. Since this class already keeps string keys
     * in insertion-time order, we treat all as equal. The "Arrays.sort" method will then not
     * change their order, but simply move all the numeric properties to the front, since this
     * method is defined to be stable.
     */
    public static final class KeyComparator
        implements Comparator<Object>, Serializable
    {
        private static final long serialVersionUID = 6411335891523988149L;

        @Override
        public int compare(Object o1, Object o2)
        {
            if (o1 instanceof Integer) {
                if (o2 instanceof Integer) {
                    int i1 = ((Integer) o1).intValue();
                    int i2 = ((Integer) o2).intValue();
                    if (i1 < i2) {
                        return -1;
                    }
                    if (i1 > i2) {
                        return 1;
                    }
                    return 0;
                }
                return -1;
            }
            if (o2 instanceof Integer) {
                return 1;
            }
            return 0;
        }
    }
}
