/* -*- Mode: java; tab-width: 4; indent-tabs-mode: 1; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Base class for native object implementation that uses IdFunctionObject to export its methods to
 * script via &lt;class-name&gt;.prototype object.
 *
 * <p>Any descendant should implement at least the following methods: findInstanceIdInfo
 * getInstanceIdName execIdCall methodArity
 *
 * <p>To define non-function properties, the descendant should override getInstanceIdValue
 * setInstanceIdValue to get/set property value and provide its default attributes.
 *
 * <p>To customize initialization of constructor and prototype objects, descendant may override
 * scopeInit or fillConstructorProperties methods.
 */
public abstract class IdScriptableObject extends ScriptableObject implements IdFunctionCall {
    private static final long serialVersionUID = -3744239272168621609L;
    private transient PrototypeValues prototypeValues;

    private static final class PrototypeValues implements Serializable {
        private static final long serialVersionUID = 3038645279153854371L;

        private static final int NAME_SLOT = 1;
        private static final int SLOT_SPAN = 2;

        private IdScriptableObject obj;
        private int maxId;
        private Object[] valueArray;
        private short[] attributeArray;

        // The following helps to avoid creation of valueArray during runtime
        // initialization for common case of "constructor" property
        int constructorId;
        private IdFunctionObject constructor;
        private short constructorAttrs;

        PrototypeValues(IdScriptableObject obj, int maxId) {
            if (obj == null) throw new IllegalArgumentException();
            if (maxId < 1) throw new IllegalArgumentException();
            this.obj = obj;
            this.maxId = maxId;
        }

        final int getMaxId() {
            return maxId;
        }

        final void initValue(int id, String name, Object value, int attributes) {
            if (!(1 <= id && id <= maxId)) throw new IllegalArgumentException();
            if (name == null) throw new IllegalArgumentException();
            if (value == NOT_FOUND) throw new IllegalArgumentException();
            ScriptableObject.checkValidAttributes(attributes);
            if (obj.findPrototypeId(name) != id) throw new IllegalArgumentException(name);

            if (id == constructorId) {
                if (!(value instanceof IdFunctionObject)) {
                    throw new IllegalArgumentException(
                            "constructor should be initialized with IdFunctionObject");
                }
                constructor = (IdFunctionObject) value;
                constructorAttrs = (short) attributes;
                return;
            }

            initSlot(id, name, value, attributes);
        }

        final void initValue(int id, Symbol key, Object value, int attributes) {
            if (!(1 <= id && id <= maxId)) throw new IllegalArgumentException();
            if (key == null) throw new IllegalArgumentException();
            if (value == NOT_FOUND) throw new IllegalArgumentException();
            ScriptableObject.checkValidAttributes(attributes);
            if (obj.findPrototypeId(key) != id) throw new IllegalArgumentException(key.toString());

            if (id == constructorId) {
                if (!(value instanceof IdFunctionObject)) {
                    throw new IllegalArgumentException(
                            "constructor should be initialized with IdFunctionObject");
                }
                constructor = (IdFunctionObject) value;
                constructorAttrs = (short) attributes;
                return;
            }

            initSlot(id, key, value, attributes);
        }

        private void initSlot(int id, Object name, Object value, int attributes) {
            Object[] array = valueArray;
            if (array == null) throw new IllegalStateException();

            if (value == null) {
                value = UniqueTag.NULL_VALUE;
            }
            int index = (id - 1) * SLOT_SPAN;
            synchronized (this) {
                Object value2 = array[index];
                if (value2 == null) {
                    array[index] = value;
                    array[index + NAME_SLOT] = name;
                    attributeArray[id - 1] = (short) attributes;
                } else {
                    if (!name.equals(array[index + NAME_SLOT])) throw new IllegalStateException();
                }
            }
        }

        final IdFunctionObject createPrecachedConstructor() {
            if (constructorId != 0) throw new IllegalStateException();
            constructorId = obj.findPrototypeId("constructor");
            if (constructorId == 0) {
                throw new IllegalStateException("No id for constructor property");
            }
            obj.initPrototypeId(constructorId);
            if (constructor == null) {
                throw new IllegalStateException(
                        obj.getClass().getName()
                                + ".initPrototypeId() did not "
                                + "initialize id="
                                + constructorId);
            }
            constructor.initFunction(obj.getClassName(), ScriptableObject.getTopLevelScope(obj));
            constructor.markAsConstructor(obj);
            return constructor;
        }

        final int findId(String name) {
            return obj.findPrototypeId(name);
        }

        final int findId(Symbol key) {
            return obj.findPrototypeId(key);
        }

        final boolean has(int id) {
            Object[] array = valueArray;
            if (array == null) {
                // Not yet initialized, assume all exists
                return true;
            }
            int valueSlot = (id - 1) * SLOT_SPAN;
            Object value = array[valueSlot];
            if (value == null) {
                // The particular entry has not been yet initialized
                return true;
            }
            return value != NOT_FOUND;
        }

        final Object get(int id) {
            Object value = ensureId(id);
            if (value == UniqueTag.NULL_VALUE) {
                value = null;
            }
            return value;
        }

        final void set(int id, Scriptable start, Object value) {
            if (value == NOT_FOUND) throw new IllegalArgumentException();
            ensureId(id);
            int attr = attributeArray[id - 1];
            if ((attr & READONLY) == 0) {
                if (start == obj) {
                    if (value == null) {
                        value = UniqueTag.NULL_VALUE;
                    }
                    int valueSlot = (id - 1) * SLOT_SPAN;
                    synchronized (this) {
                        valueArray[valueSlot] = value;
                    }
                } else {
                    int nameSlot = (id - 1) * SLOT_SPAN + NAME_SLOT;
                    Object name = valueArray[nameSlot];
                    if (name instanceof Symbol) {
                        if (start instanceof SymbolScriptable) {
                            ((SymbolScriptable) start).put((Symbol) name, start, value);
                        }
                    } else {
                        start.put((String) name, start, value);
                    }
                }
            }
        }

        final void delete(int id) {
            ensureId(id);
            int attr = attributeArray[id - 1];
            // non-configurable
            if ((attr & PERMANENT) != 0) {
                Context cx = Context.getContext();
                if (cx.isStrictMode()) {
                    int nameSlot = (id - 1) * SLOT_SPAN + NAME_SLOT;

                    String name = null;
                    if (valueArray[nameSlot] instanceof String)
                        name = (String) valueArray[nameSlot];
                    else if (valueArray[nameSlot] instanceof Symbol) {
                        name = valueArray[nameSlot].toString();
                    }
                    throw ScriptRuntime.typeErrorById(
                            "msg.delete.property.with.configurable.false", name);
                }
            } else {
                int valueSlot = (id - 1) * SLOT_SPAN;
                synchronized (this) {
                    valueArray[valueSlot] = NOT_FOUND;
                    attributeArray[id - 1] = EMPTY;
                }
            }
        }

        final int getAttributes(int id) {
            ensureId(id);
            return attributeArray[id - 1];
        }

        final void setAttributes(int id, int attributes) {
            ScriptableObject.checkValidAttributes(attributes);
            ensureId(id);
            synchronized (this) {
                attributeArray[id - 1] = (short) attributes;
            }
        }

        final Object[] getNames(boolean getAll, boolean getSymbols, Object[] extraEntries) {
            Object[] names = null;
            int count = 0;
            for (int id = 1; id <= maxId; ++id) {
                Object value = ensureId(id);
                if (getAll || (attributeArray[id - 1] & DONTENUM) == 0) {
                    if (value != NOT_FOUND) {
                        int nameSlot = (id - 1) * SLOT_SPAN + NAME_SLOT;
                        Object name = valueArray[nameSlot];
                        if (name instanceof String) {
                            if (names == null) {
                                names = new Object[maxId];
                            }
                            names[count++] = name;
                        } else if (getSymbols && (name instanceof Symbol)) {
                            if (names == null) {
                                names = new Object[maxId];
                            }
                            names[count++] = name.toString();
                        }
                    }
                }
            }
            if (count == 0) {
                return extraEntries;
            } else if (extraEntries == null || extraEntries.length == 0) {
                if (count != names.length) {
                    Object[] tmp = new Object[count];
                    System.arraycopy(names, 0, tmp, 0, count);
                    names = tmp;
                }
                return names;
            } else {
                int extra = extraEntries.length;
                Object[] tmp = new Object[extra + count];
                System.arraycopy(extraEntries, 0, tmp, 0, extra);
                System.arraycopy(names, 0, tmp, extra, count);
                return tmp;
            }
        }

        private Object ensureId(int id) {
            Object[] array = valueArray;
            synchronized (this) {
                if (array == null) {
                    array = valueArray;
                    if (array == null) {
                        array = new Object[maxId * SLOT_SPAN];
                        valueArray = array;
                        attributeArray = new short[maxId];
                    }
                }
            }
            int valueSlot = (id - 1) * SLOT_SPAN;
            Object value = array[valueSlot];
            if (value == null) {
                if (id == constructorId) {
                    initSlot(constructorId, "constructor", constructor, constructorAttrs);
                    constructor = null; // no need to refer it any longer
                } else {
                    obj.initPrototypeId(id);
                }
                value = array[valueSlot];
                if (value == null) {
                    throw new IllegalStateException(
                            obj.getClass().getName()
                                    + ".initPrototypeId(int id) "
                                    + "did not initialize id="
                                    + id);
                }
            }
            return value;
        }
    }

    public IdScriptableObject() {}

    public IdScriptableObject(Scriptable scope, Scriptable prototype) {
        super(scope, prototype);
    }

    protected final boolean defaultHas(String name) {
        return super.has(name, this);
    }

    protected final Object defaultGet(String name) {
        return super.get(name, this);
    }

    protected final void defaultPut(String name, Object value) {
        super.put(name, this, value);
    }

    @Override
    public boolean has(String name, Scriptable start) {
        int info = findInstanceIdInfo(name);
        if (info != 0) {
            int attr = (info >>> 16);
            if ((attr & PERMANENT) != 0) {
                return true;
            }
            int id = (info & 0xFFFF);
            return NOT_FOUND != getInstanceIdValue(id);
        }
        if (prototypeValues != null) {
            int id = prototypeValues.findId(name);
            if (id != 0) {
                return prototypeValues.has(id);
            }
        }
        return super.has(name, start);
    }

    @Override
    public boolean has(Symbol key, Scriptable start) {
        int info = findInstanceIdInfo(key);
        if (info != 0) {
            int attr = (info >>> 16);
            if ((attr & PERMANENT) != 0) {
                return true;
            }
            int id = (info & 0xFFFF);
            return NOT_FOUND != getInstanceIdValue(id);
        }
        if (prototypeValues != null) {
            int id = prototypeValues.findId(key);
            if (id != 0) {
                return prototypeValues.has(id);
            }
        }
        return super.has(key, start);
    }

    @Override
    public Object get(String name, Scriptable start) {
        // Check for slot first for performance. This is a very hot code
        // path that should be further optimized.
        Object value = super.get(name, start);
        if (value != NOT_FOUND) {
            return value;
        }
        int info = findInstanceIdInfo(name);
        if (info != 0) {
            int id = (info & 0xFFFF);
            value = getInstanceIdValue(id);
            if (value != NOT_FOUND) return value;
        }
        if (prototypeValues != null) {
            int id = prototypeValues.findId(name);
            if (id != 0) {
                value = prototypeValues.get(id);
                if (value != NOT_FOUND) return value;
            }
        }
        return NOT_FOUND;
    }

    @Override
    public Object get(Symbol key, Scriptable start) {
        Object value = super.get(key, start);
        if (value != NOT_FOUND) {
            return value;
        }
        int info = findInstanceIdInfo(key);
        if (info != 0) {
            int id = (info & 0xFFFF);
            value = getInstanceIdValue(id);
            if (value != NOT_FOUND) return value;
        }
        if (prototypeValues != null) {
            int id = prototypeValues.findId(key);
            if (id != 0) {
                value = prototypeValues.get(id);
                if (value != NOT_FOUND) return value;
            }
        }
        return NOT_FOUND;
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        int info = findInstanceIdInfo(name);
        if (info != 0) {
            if (start == this && isSealed()) {
                throw Context.reportRuntimeErrorById("msg.modify.sealed", name);
            }
            int attr = (info >>> 16);
            if ((attr & READONLY) == 0) {
                if (start == this) {
                    int id = (info & 0xFFFF);
                    setInstanceIdValue(id, value);
                } else {
                    start.put(name, start, value);
                }
            }
            return;
        }
        if (prototypeValues != null) {
            int id = prototypeValues.findId(name);
            if (id != 0) {
                if (start == this && isSealed()) {
                    throw Context.reportRuntimeErrorById("msg.modify.sealed", name);
                }
                prototypeValues.set(id, start, value);
                return;
            }
        }
        super.put(name, start, value);
    }

    @Override
    public void put(Symbol key, Scriptable start, Object value) {
        int info = findInstanceIdInfo(key);
        if (info != 0) {
            if (start == this && isSealed()) {
                throw Context.reportRuntimeErrorById("msg.modify.sealed");
            }
            int attr = (info >>> 16);
            if ((attr & READONLY) == 0) {
                if (start == this) {
                    int id = (info & 0xFFFF);
                    setInstanceIdValue(id, value);
                } else {
                    ensureSymbolScriptable(start).put(key, start, value);
                }
            }
            return;
        }
        if (prototypeValues != null) {
            int id = prototypeValues.findId(key);
            if (id != 0) {
                if (start == this && isSealed()) {
                    throw Context.reportRuntimeErrorById("msg.modify.sealed");
                }
                prototypeValues.set(id, start, value);
                return;
            }
        }
        super.put(key, start, value);
    }

    @Override
    public void delete(String name) {
        int info = findInstanceIdInfo(name);
        if (info != 0) {
            // Let the super class to throw exceptions for sealed objects
            if (!isSealed()) {
                int attr = (info >>> 16);
                // non-configurable
                if ((attr & PERMANENT) != 0) {
                    Context cx = Context.getContext();
                    if (cx.isStrictMode()) {
                        throw ScriptRuntime.typeErrorById(
                                "msg.delete.property.with.configurable.false", name);
                    }
                } else {
                    int id = (info & 0xFFFF);
                    setInstanceIdValue(id, NOT_FOUND);
                }
                return;
            }
        }
        if (prototypeValues != null) {
            int id = prototypeValues.findId(name);
            if (id != 0) {
                if (!isSealed()) {
                    prototypeValues.delete(id);
                }
                return;
            }
        }
        super.delete(name);
    }

    @Override
    public void delete(Symbol key) {
        int info = findInstanceIdInfo(key);
        if (info != 0) {
            // Let the super class to throw exceptions for sealed objects
            if (!isSealed()) {
                int attr = (info >>> 16);
                // non-configurable
                if ((attr & PERMANENT) != 0) {
                    Context cx = Context.getContext();
                    if (cx.isStrictMode()) {
                        throw ScriptRuntime.typeErrorById(
                                "msg.delete.property.with.configurable.false");
                    }
                } else {
                    int id = (info & 0xFFFF);
                    setInstanceIdValue(id, NOT_FOUND);
                }
                return;
            }
        }
        if (prototypeValues != null) {
            int id = prototypeValues.findId(key);
            if (id != 0) {
                if (!isSealed()) {
                    prototypeValues.delete(id);
                }
                return;
            }
        }
        super.delete(key);
    }

    @Override
    public int getAttributes(String name) {
        int info = findInstanceIdInfo(name);
        if (info != 0) {
            int attr = (info >>> 16);
            return attr;
        }
        if (prototypeValues != null) {
            int id = prototypeValues.findId(name);
            if (id != 0) {
                return prototypeValues.getAttributes(id);
            }
        }
        return super.getAttributes(name);
    }

    @Override
    public int getAttributes(Symbol key) {
        int info = findInstanceIdInfo(key);
        if (info != 0) {
            int attr = (info >>> 16);
            return attr;
        }
        if (prototypeValues != null) {
            int id = prototypeValues.findId(key);
            if (id != 0) {
                return prototypeValues.getAttributes(id);
            }
        }
        return super.getAttributes(key);
    }

    @Override
    public void setAttributes(String name, int attributes) {
        ScriptableObject.checkValidAttributes(attributes);
        int info = findInstanceIdInfo(name);
        if (info != 0) {
            int id = (info & 0xFFFF);
            int currentAttributes = (info >>> 16);
            if (attributes != currentAttributes) {
                setInstanceIdAttributes(id, attributes);
            }
            return;
        }
        if (prototypeValues != null) {
            int id = prototypeValues.findId(name);
            if (id != 0) {
                prototypeValues.setAttributes(id, attributes);
                return;
            }
        }
        super.setAttributes(name, attributes);
    }

    @Override
    Object[] getIds(boolean getNonEnumerable, boolean getSymbols) {
        Object[] result = super.getIds(getNonEnumerable, getSymbols);

        if (prototypeValues != null) {
            result = prototypeValues.getNames(getNonEnumerable, getSymbols, result);
        }

        int maxInstanceId = getMaxInstanceId();
        if (maxInstanceId != 0) {
            Object[] ids = null;
            int count = 0;

            for (int id = maxInstanceId; id != 0; --id) {
                String name = getInstanceIdName(id);
                int info = findInstanceIdInfo(name);
                if (info != 0) {
                    int attr = (info >>> 16);
                    if ((attr & PERMANENT) == 0) {
                        if (NOT_FOUND == getInstanceIdValue(id)) {
                            continue;
                        }
                    }
                    if (getNonEnumerable || (attr & DONTENUM) == 0) {
                        if (count == 0) {
                            // Need extra room for no more then [1..id] names
                            ids = new Object[id];
                        }
                        ids[count++] = name;
                    }
                }
            }
            if (count != 0) {
                if (result.length == 0 && ids.length == count) {
                    result = ids;
                } else {
                    Object[] tmp = new Object[result.length + count];
                    System.arraycopy(result, 0, tmp, 0, result.length);
                    System.arraycopy(ids, 0, tmp, result.length, count);
                    result = tmp;
                }
            }
        }
        return result;
    }

    /** Get maximum id findInstanceIdInfo can generate. */
    protected int getMaxInstanceId() {
        return 0;
    }

    protected static int instanceIdInfo(int attributes, int id) {
        return (attributes << 16) | id;
    }

    /**
     * Map name to id of instance property. Should return 0 if not found or the result of {@link
     * #instanceIdInfo(int, int)}.
     */
    protected int findInstanceIdInfo(String name) {
        return 0;
    }

    /**
     * Map name to id of instance property. Should return 0 if not found or the result of {@link
     * #instanceIdInfo(int, int)}.
     */
    protected int findInstanceIdInfo(Symbol key) {
        return 0;
    }

    /** Map id back to property name it defines. */
    protected String getInstanceIdName(int id) {
        throw new IllegalArgumentException(String.valueOf(id));
    }

    /**
     * Get id value. * If id value is constant, descendant can call cacheIdValue to store * value in
     * the permanent cache. * Default implementation creates IdFunctionObject instance for given id
     * * and cache its value
     */
    protected Object getInstanceIdValue(int id) {
        throw new IllegalStateException(String.valueOf(id));
    }

    /**
     * Set or delete id value. If value == NOT_FOUND , the implementation should make sure that the
     * following getInstanceIdValue return NOT_FOUND.
     */
    protected void setInstanceIdValue(int id, Object value) {
        throw new IllegalStateException(String.valueOf(id));
    }

    /**
     * Update the attributes of the given instance property. Classes which want to support changing
     * property attributes via Object.defineProperty must override this method. The default
     * implementation throws InternalError.
     *
     * @param id the instance property id
     * @param attr the new attribute bitset
     */
    protected void setInstanceIdAttributes(int id, int attr) {
        throw ScriptRuntime.constructError(
                "InternalError",
                "Changing attributes not supported for "
                        + getClassName()
                        + " "
                        + getInstanceIdName(id)
                        + " property");
    }

    /**
     * 'thisObj' will be null if invoked as constructor, in which case * instance of Scriptable
     * should be returned.
     */
    @Override
    public Object execIdCall(
            IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        throw f.unknown();
    }

    public final IdFunctionObject exportAsJSClass(
            int maxPrototypeId, Scriptable scope, boolean sealed) {
        // Set scope and prototype unless this is top level scope itself
        if (scope != this && scope != null) {
            setParentScope(scope);
            setPrototype(getObjectPrototype(scope));
        }

        activatePrototypeMap(maxPrototypeId);
        IdFunctionObject ctor = prototypeValues.createPrecachedConstructor();
        if (sealed) {
            sealObject();
        }
        fillConstructorProperties(ctor);
        if (sealed) {
            ctor.sealObject();
        }
        ctor.exportAsScopeProperty();
        return ctor;
    }

    public final boolean hasPrototypeMap() {
        return prototypeValues != null;
    }

    public final void activatePrototypeMap(int maxPrototypeId) {
        PrototypeValues values = new PrototypeValues(this, maxPrototypeId);
        synchronized (this) {
            if (prototypeValues != null) throw new IllegalStateException();
            prototypeValues = values;
        }
    }

    public final IdFunctionObject initPrototypeMethod(Object tag, int id, String name, int arity) {
        return initPrototypeMethod(tag, id, name, name, arity);
    }

    public final IdFunctionObject initPrototypeMethod(
            Object tag, int id, String propertyName, String functionName, int arity) {
        Scriptable scope = ScriptableObject.getTopLevelScope(this);
        IdFunctionObject function =
                newIdFunction(
                        tag, id, functionName != null ? functionName : propertyName, arity, scope);
        prototypeValues.initValue(id, propertyName, function, DONTENUM);
        return function;
    }

    public final IdFunctionObject initPrototypeMethod(
            Object tag, int id, Symbol key, String functionName, int arity) {
        Scriptable scope = ScriptableObject.getTopLevelScope(this);
        IdFunctionObject function = newIdFunction(tag, id, functionName, arity, scope);
        prototypeValues.initValue(id, key, function, DONTENUM);
        return function;
    }

    public final IdFunctionObject initPrototypeMethod(
            Object tag, int id, Symbol key, String functionName, int arity, int attributes) {
        Scriptable scope = ScriptableObject.getTopLevelScope(this);
        IdFunctionObject function = newIdFunction(tag, id, functionName, arity, scope);
        prototypeValues.initValue(id, key, function, attributes);
        return function;
    }

    public final void initPrototypeConstructor(IdFunctionObject f) {
        int id = prototypeValues.constructorId;
        if (id == 0) throw new IllegalStateException();
        if (f.methodId() != id) throw new IllegalArgumentException();
        if (isSealed()) {
            f.sealObject();
        }
        prototypeValues.initValue(id, "constructor", f, DONTENUM);
    }

    public final void initPrototypeValue(int id, String name, Object value, int attributes) {
        prototypeValues.initValue(id, name, value, attributes);
    }

    public final void initPrototypeValue(int id, Symbol key, Object value, int attributes) {
        prototypeValues.initValue(id, key, value, attributes);
    }

    protected void initPrototypeId(int id) {
        throw new IllegalStateException(String.valueOf(id));
    }

    protected int findPrototypeId(String name) {
        throw new IllegalStateException(name);
    }

    protected int findPrototypeId(Symbol key) {
        return 0;
    }

    protected void fillConstructorProperties(IdFunctionObject ctor) {}

    protected void addIdFunctionProperty(
            Scriptable obj, Object tag, int id, String name, int arity) {
        Scriptable scope = ScriptableObject.getTopLevelScope(obj);
        IdFunctionObject f = newIdFunction(tag, id, name, arity, scope);
        f.addAsProperty(obj);
    }

    /**
     * Utility method to check the type and do the cast or throw an incompatible call error.
     * Possible usage would be to have a private function like realThis:
     *
     * <pre>
     *  private static NativeSomething realThis(Scriptable thisObj, IdFunctionObject f)
     *  {
     *      return ensureType(thisObj, NativeSomething.class, f);
     * }
     * </pre>
     *
     * @param obj the object to check/cast
     * @param clazz the target type
     * @param f function that is attempting to convert 'this' object.
     * @return obj casted to the target type
     * @throws EcmaError if the cast failed.
     */
    protected static <T> T ensureType(Object obj, Class<T> clazz, IdFunctionObject f) {
        return ensureType(obj, clazz, f.getFunctionName());
    }

    @SuppressWarnings("unchecked")
    protected static <T> T ensureType(Object obj, Class<T> clazz, String functionName) {
        if (clazz.isInstance(obj)) {
            return (T) obj;
        }
        if (obj == null) {
            throw ScriptRuntime.typeErrorById(
                    "msg.incompat.call.details", functionName, "null", clazz.getName());
        }
        throw ScriptRuntime.typeErrorById(
                "msg.incompat.call.details",
                functionName,
                obj.getClass().getName(),
                clazz.getName());
    }

    private IdFunctionObject newIdFunction(
            Object tag, int id, String name, int arity, Scriptable scope) {
        IdFunctionObject function = null;
        if (Context.getContext().getLanguageVersion() < Context.VERSION_ES6) {
            function = new IdFunctionObject(this, tag, id, name, arity, scope);
        } else {
            function = new IdFunctionObjectES6(this, tag, id, name, arity, scope);
        }

        if (isSealed()) {
            function.sealObject();
        }
        return function;
    }

    @Override
    protected boolean defineOwnProperty(
            Context cx, Object key, ScriptableObject desc, boolean checkValid) {
        if (key instanceof CharSequence) {
            String name = key.toString();
            int info = findInstanceIdInfo(name);
            if (info != 0) {
                int id = (info & 0xFFFF);
                if (isAccessorDescriptor(desc)) {
                    delete(id); // it will be replaced with a slot
                } else {
                    checkPropertyDefinition(desc);
                    var slot = queryOrFakeSlot(cx, key);
                    checkPropertyChangeForSlot(name, slot, desc);
                    int attr = (info >>> 16);
                    Object value = getProperty(desc, "value");
                    if (value != NOT_FOUND && ((attr & READONLY) == 0 || (attr & PERMANENT) == 0)) {
                        Object currentValue = getInstanceIdValue(id);
                        if (!sameValue(value, currentValue)) {
                            setInstanceIdValue(id, value);
                        }
                    }
                    attr =
                            applyDescriptorToAttributeBitset(
                                    attr,
                                    getProperty(desc, "enumerable"),
                                    getProperty(desc, "writable"),
                                    getProperty(desc, "configurable"));
                    setAttributes(name, attr);
                    return true;
                }
            }
            if (prototypeValues != null) {
                int id = prototypeValues.findId(name);
                if (id != 0) {
                    if (isAccessorDescriptor(desc)) {
                        prototypeValues.delete(id); // it will be replaced with a slot
                    } else {
                        checkPropertyDefinition(desc);
                        var slot = queryOrFakeSlot(cx, key);
                        checkPropertyChangeForSlot(name, slot, desc);
                        int attr = prototypeValues.getAttributes(id);
                        Object value = getProperty(desc, "value");
                        if (value != NOT_FOUND && (attr & READONLY) == 0) {
                            Object currentValue = prototypeValues.get(id);
                            if (!sameValue(value, currentValue)) {
                                prototypeValues.set(id, this, value);
                            }
                        }
                        prototypeValues.setAttributes(
                                id,
                                applyDescriptorToAttributeBitset(
                                        attr,
                                        getProperty(desc, "enumerable"),
                                        getProperty(desc, "writable"),
                                        getProperty(desc, "configurable")));

                        // Handle the regular slot that was created if this property was previously
                        // replaced
                        // with an accessor descriptor.
                        if (super.has(name, this)) {
                            super.delete(name);
                        }

                        return true;
                    }
                }
            }
        }
        return super.defineOwnProperty(cx, key, desc, checkValid);
    }

    @Override
    protected ScriptableObject getOwnPropertyDescriptor(Context cx, Object id) {
        ScriptableObject desc = super.getOwnPropertyDescriptor(cx, id);
        if (desc == null) {
            if (id instanceof String) {
                return getBuiltInDataDescriptor((String) id);
            }

            if (ScriptRuntime.isSymbol(id)) {
                if (id instanceof SymbolKey) {
                    return getBuiltInDataDescriptor((SymbolKey) id);
                }

                return getBuiltInDataDescriptor(((NativeSymbol) id).getKey());
            }
        }
        return desc;
    }

    private Slot queryOrFakeSlot(Context cx, Object id) {
        var slot = querySlot(cx, id);
        if (slot == null) {
            if (id instanceof String) {
                return getBuiltInSlot((String) id);
            }

            if (ScriptRuntime.isSymbol(id)) {
                if (id instanceof SymbolKey) {
                    return getBuiltInSlot((SymbolKey) id);
                }

                return getBuiltInSlot(((NativeSymbol) id).getKey());
            }
        }
        return slot;
    }

    private ScriptableObject getBuiltInDataDescriptor(String name) {
        Scriptable scope = getParentScope();
        if (scope == null) {
            scope = this;
        }

        var slot = getBuiltInSlot(name);
        return slot == null ? null : buildDataDescriptor(scope, slot.value, slot.getAttributes());
    }

    private Slot getBuiltInSlot(String name) {
        Object value = null;
        int attr = EMPTY;

        int info = findInstanceIdInfo(name);
        if (info != 0) {
            int id = (info & 0xFFFF);
            value = getInstanceIdValue(id);
            attr = (info >>> 16);
            var slot = new Slot(name, 0, attr);
            slot.value = value;
            return slot;
        }
        if (prototypeValues != null) {
            int id = prototypeValues.findId(name);
            if (id != 0) {
                value = prototypeValues.get(id);
                attr = prototypeValues.getAttributes(id);
                var slot = new Slot(name, 0, attr);
                slot.value = value;
                return slot;
            }
        }
        return null;
    }

    private ScriptableObject getBuiltInDataDescriptor(Symbol key) {
        Scriptable scope = getParentScope();
        if (scope == null) {
            scope = this;
        }

        var slot = getBuiltInSlot(key);
        return slot == null ? null : buildDataDescriptor(scope, slot.value, slot.getAttributes());
    }

    private Slot getBuiltInSlot(Symbol key) {
        Object value = null;
        int attr = EMPTY;

        if (prototypeValues != null) {
            int id = prototypeValues.findId(key);
            if (id != 0) {
                value = prototypeValues.get(id);
                attr = prototypeValues.getAttributes(id);
                var slot = new Slot(key, 0, attr);
                slot.value = value;
                return slot;
            }
        }
        return null;
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        int maxPrototypeId = stream.readInt();
        if (maxPrototypeId != 0) {
            activatePrototypeMap(maxPrototypeId);
        }
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        int maxPrototypeId = 0;
        if (prototypeValues != null) {
            maxPrototypeId = prototypeValues.getMaxId();
        }
        stream.writeInt(maxPrototypeId);
    }
}
