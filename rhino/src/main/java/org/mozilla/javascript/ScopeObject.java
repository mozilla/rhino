package org.mozilla.javascript;

import static org.mozilla.javascript.ScriptableObject.PERMANENT;
import static org.mozilla.javascript.ScriptableObject.READONLY;
import static org.mozilla.javascript.ScriptableObject.UNINITIALIZED_CONST;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class ScopeObject extends SlotMapOwner<Scriptable> implements VarScope, Serializable {
    private static final long serialVersionUID = -7471457301304454454L;

    private final VarScope parentScope;

    public ScopeObject(VarScope parentScope) {
        this.parentScope = parentScope;
    }

    @Override
    public VarScope getParentScope() {
        return parentScope;
    }

    @Override
    public ScopeObject getThis() {
        return this;
    }

    /**
     * Sets the value of the named property, creating it if need be.
     *
     * <p>If the property was created using defineProperty, the appropriate setter method is called.
     *
     * <p>If the property's attributes include READONLY, no action is taken. This method will
     * actually set the property in the start object.
     *
     * @param name the name of the property
     * @param start the object whose property is being set
     * @param value value to set the property to
     */
    @Override
    public void put(String name, Scriptable start, Object value) {
        if (putOwnProperty(name, start, value, Context.isCurrentContextStrict())) return;

        if (start == this) throw Kit.codeBug();
        start.put(name, start, value);
    }

    /**
     * Set the value of the named property, and return true if the property is actually defined and
     * can be set. Subclasses of ScriptableObject should override this method, and not "put," for
     * proper strict mode operation in the future.
     *
     * @param isThrow if true, throw an exception as if in strict mode
     */
    protected boolean putOwnProperty(String name, Scriptable start, Object value, boolean isThrow) {
        return putImpl(name, 0, start, value, isThrow);
    }

    /**
     * Sets the value of the indexed property, creating it if need be.
     *
     * @param index the numeric index for the property
     * @param start the object whose property is being set
     * @param value value to set the property to
     */
    @SuppressWarnings("resource")
    @Override
    public void put(int index, Scriptable start, Object value) {
        if (putOwnProperty(index, start, value, Context.isCurrentContextStrict())) return;

        if (start == this) throw Kit.codeBug();
        start.put(index, start, value);
    }

    /**
     * Set the value of the named property, and return true if the property is actually defined and
     * can be set. Subclasses of ScriptableObject should override this method, and not "put," for
     * proper strict mode operation in the future
     *
     * @param isThrow if true, throw an exception as if in strict mode
     */
    protected boolean putOwnProperty(int index, Scriptable start, Object value, boolean isThrow) {
        return putImpl(null, index, start, value, isThrow);
    }

    /** Implementation of put required by SymbolScriptable objects. */
    @Override
    public void put(Symbol key, Scriptable start, Object value) {
        if (putOwnProperty(key, start, value, Context.isCurrentContextStrict())) return;

        if (start == this) throw Kit.codeBug();
        start.put(key, start, value);
    }

    /**
     * Set the value of the named property, and return true if the property is actually defined and
     * can be set. Subclasses of ScriptableObject should override this method, and not "put," for
     * proper strict mode operation in the future
     *
     * @param isThrow if true, throw an exception as if in strict mode
     */
    protected boolean putOwnProperty(Symbol key, Scriptable start, Object value, boolean isThrow) {
        return putImpl(key, 0, start, value, isThrow);
    }

    /**
     * @param key
     * @param index
     * @param start
     * @param value
     * @param isThrow
     * @return false if this != start and no slot was found. true if this == start or this != start
     *     and a READONLY slot was found.
     */
    private boolean putImpl(
            Object key, int index, Scriptable start, Object value, boolean isThrow) {
        Slot<Scriptable> slot;
        if (this != start) {
            slot = getMap().query(key, index);
            if (slot == null) {
                return false;
            }
        } else {
            slot = getMap().modify(this, key, index, 0);
        }
        return slot.setValue(value, this, start, isThrow);
    }

    /**
     * Returns true if the named property is defined.
     *
     * @param name the name of the property
     * @param start the object in which the lookup began
     * @return true if and only if the property was found in the object
     */
    @Override
    public boolean has(String name, Scriptable start) {
        return null != getMap().query(name, 0);
    }

    /**
     * Returns true if the property index is defined.
     *
     * @param index the numeric index for the property
     * @param start the object in which the lookup began
     * @return true if and only if the property was found in the object
     */
    @Override
    public boolean has(int index, Scriptable start) {
        return null != getMap().query(null, index);
    }

    /** A version of "has" that supports symbols. */
    @Override
    public boolean has(Symbol key, Scriptable start) {
        return null != getMap().query(key, 0);
    }

    /**
     * Returns the value of the named property or NOT_FOUND.
     *
     * <p>If the property was created using defineProperty, the appropriate getter method is called.
     *
     * @param name the name of the property
     * @param start the object in which the lookup began
     * @return the value of the property (may be null), or NOT_FOUND
     */
    @Override
    public Object get(String name, Scriptable start) {
        var slot = getMap().query(name, 0);
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
    public Object get(int index, Scriptable start) {
        var slot = getMap().query(null, index);
        if (slot == null) {
            return Scriptable.NOT_FOUND;
        }
        return slot.getValue(start);
    }

    /** Another version of Get that supports Symbol keyed properties. */
    @Override
    public Object get(Symbol key, Scriptable start) {
        var slot = getMap().query(key, 0);
        if (slot == null) {
            return Scriptable.NOT_FOUND;
        }
        return slot.getValue(start);
    }

    @Override
    public void delete(String name) {
        getMap().compute(this, name, 0, ScriptableObject::checkSlotRemoval);
    }

    @Override
    public void delete(int index) {
        getMap().compute(this, null, index, ScriptableObject::checkSlotRemoval);
    }

    @Override
    public void delete(Symbol key) {
        getMap().compute(this, key, 0, ScriptableObject::checkSlotRemoval);
    }

    @Override
    public Object[] getIds() {
        return new Object[0];
    }

    /**
     * Define a JavaScript property.
     *
     * <p>Creates the property with an initial value and sets its attributes.
     *
     * @param propertyName the name of the property to define.
     * @param value the initial value of the property
     * @param attributes the attributes of the JavaScript property
     * @see org.mozilla.javascript.Scriptable#put(String, Scriptable, Object)
     */
    @Override
    public void defineProperty(String propertyName, Object value, int attributes) {
        put(propertyName, this, value);
        setAttributes(propertyName, attributes);
    }

    /**
     * A version of defineProperty that uses a Symbol key.
     *
     * @param key symbol of the property to define.
     * @param value the initial value of the property
     * @param attributes the attributes of the JavaScript property
     */
    @Override
    public void defineProperty(Symbol key, Object value, int attributes) {
        put(key, this, value);
        setAttributes(key, attributes);
    }

    /**
     * Set the attributes of a named property.
     *
     * <p>The property is specified by <code>name</code> as defined for <code>has</code>.
     *
     * <p>The possible attributes are READONLY, DONTENUM, and PERMANENT. Combinations of attributes
     * are expressed by the bitwise OR of attributes. EMPTY is the state of no attributes set. Any
     * unused bits are reserved for future use.
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
    @Override
    public void setAttributes(String name, int attributes) {
        Slot attrSlot = getMap().modify(this, name, 0, 0);
        attrSlot.setAttributes(attributes);
    }

    /** Set attributes of a Symbol-keyed property. */
    @Override
    public void setAttributes(Symbol key, int attributes) {
        Slot attrSlot = getMap().modify(this, key, 0, 0);
        attrSlot.setAttributes(attributes);
    }

    /**
     * Sets the value of the named const property, creating it if need be.
     *
     * <p>If the property was created using defineProperty, the appropriate setter method is called.
     *
     * <p>If the property's attributes include READONLY, no action is taken. This method will
     * actually set the property in the start object.
     *
     * @param name the name of the property
     * @param start the object whose property is being set
     * @param value value to set the property to
     */
    @Override
    public void putConst(String name, Scriptable start, Object value) {
        if (putConstImpl(name, 0, start, value, ScriptableObject.READONLY)) return;

        if (start == this) throw Kit.codeBug();
        if (start instanceof ConstProperties) {
            @SuppressWarnings("unchecked")
            var cstart = ((ConstProperties<Scriptable>) start);
            cstart.putConst(name, start, value);
        } else start.put(name, start, value);
    }

    @Override
    public void defineConst(String name, Scriptable start) {
        if (putConstImpl(name, 0, start, Undefined.instance, ScriptableObject.UNINITIALIZED_CONST))
            return;

        if (start == this) throw Kit.codeBug();
        if (start instanceof ConstProperties) {
            @SuppressWarnings("unchecked")
            var cstart = ((ConstProperties<Scriptable>) start);
            cstart.defineConst(name, start);
        }
    }

    /**
     * Returns true if the named property is defined as a const on this object.
     *
     * @param name the name of the property
     * @return true if the named property is defined as a const, false otherwise.
     */
    @Override
    public boolean isConst(String name) {
        Slot slot = getMap().query(name, 0);
        if (slot == null) {
            return false;
        }
        return (slot.getAttributes() & (PERMANENT | READONLY)) == (PERMANENT | READONLY);
    }

    /**
     * @param name
     * @param index
     * @param start
     * @param value
     * @param constFlag EMPTY means normal put. UNINITIALIZED_CONST means defineConstProperty.
     *     READONLY means const initialization expression.
     * @return false if this != start and no slot was found. true if this == start or this != start
     *     and a READONLY slot was found.
     */
    private boolean putConstImpl(
            String name, int index, Scriptable start, Object value, int constFlag) {
        assert (constFlag != ScriptableObject.EMPTY);
        Slot<Scriptable> slot;
        if (this != start) {
            slot = getMap().query(name, index);
            if (slot == null) {
                return false;
            }
        } else {
            // either const hoisted declaration or initialization
            slot = getMap().modify(this, name, index, ScriptableObject.CONST);
            int attr = slot.getAttributes();
            if ((attr & READONLY) == 0)
                throw Context.reportRuntimeErrorById("msg.var.redecl", name);
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

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        writeMaps(out);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        readMaps(in);
    }
}
