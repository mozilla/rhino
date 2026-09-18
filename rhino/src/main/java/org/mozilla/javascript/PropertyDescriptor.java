/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * Represents a property descriptor as defined by the ECMAScript specification.
 *
 * <p>A property descriptor is a record that captures information about how a property may be
 * accessed and modified. Property descriptors can describe either data properties (with value and
 * writable attributes) or accessor properties (with get and set attributes).
 *
 * @see <a href="https://tc39.es/ecma262/#sec-property-descriptor-specification-type">ECMAScript
 *     Property Descriptor Specification Type</a>
 */
public final class PropertyDescriptor {
    public Object enumerable = ScriptableObject.NOT_FOUND;
    public Object writable = ScriptableObject.NOT_FOUND;
    public Object configurable = ScriptableObject.NOT_FOUND;
    public Object getter = ScriptableObject.NOT_FOUND;
    public Object setter = ScriptableObject.NOT_FOUND;
    public Object value = ScriptableObject.NOT_FOUND;
    boolean accessorDescriptor;

    /**
     * Create a property descriptor from a JavaScript descriptor object.
     *
     * <p>The descriptor object should have properties corresponding to the descriptor attributes:
     * value, writable, get, set, enumerable, configurable.
     *
     * @param desc the descriptor object
     */
    public PropertyDescriptor(ScriptableObject desc) {
        enumerable = ScriptableObject.getProperty(desc, "enumerable");
        writable = ScriptableObject.getProperty(desc, "writable");
        configurable = ScriptableObject.getProperty(desc, "configurable");
        getter = ScriptableObject.getProperty(desc, "get");
        setter = ScriptableObject.getProperty(desc, "set");
        value = ScriptableObject.getProperty(desc, "value");
        accessorDescriptor =
                getter != ScriptableObject.NOT_FOUND || setter != ScriptableObject.NOT_FOUND;
    }

    /**
     * Create a data property descriptor.
     *
     * @param enumerable whether the property is enumerable
     * @param writable whether the property is writable
     * @param configurable whether the property is configurable
     * @param value the property value
     */
    public PropertyDescriptor(
            boolean enumerable, boolean writable, boolean configurable, Object value) {
        this.enumerable = enumerable;
        this.writable = writable;
        this.configurable = configurable;
        this.getter = ScriptableObject.NOT_FOUND;
        this.setter = ScriptableObject.NOT_FOUND;
        this.value = value;
        accessorDescriptor = false;
    }

    /**
     * Create a property descriptor with explicit attribute objects.
     *
     * <p>Attributes can be either Boolean objects or NOT_FOUND to indicate absence.
     *
     * @param enumerable the enumerable attribute
     * @param writable the writable attribute
     * @param configurable the configurable attribute
     * @param getter the get function
     * @param setter the set function
     * @param value the property value
     */
    public PropertyDescriptor(
            Object enumerable,
            Object writable,
            Object configurable,
            Object getter,
            Object setter,
            Object value) {
        this.enumerable = enumerable;
        this.writable = writable;
        this.configurable = configurable;
        this.getter = getter;
        this.setter = setter;
        this.value = value;
        accessorDescriptor =
                getter != ScriptableObject.NOT_FOUND || setter != ScriptableObject.NOT_FOUND;
    }

    /**
     * Create a property descriptor from a value and attributes.
     *
     * @param value the property value
     * @param attributes the ScriptableObject attributes (READONLY, DONTENUM, PERMANENT)
     * @param defineWritable whether to define the writable attribute
     */
    public PropertyDescriptor(Object value, int attributes, boolean defineWritable) {
        this.value = value;
        if (defineWritable) {
            writable = (attributes & ScriptableObject.READONLY) == 0;
        }
        enumerable = (attributes & ScriptableObject.DONTENUM) == 0;
        configurable = (attributes & ScriptableObject.PERMANENT) == 0;
    }

    /**
     * Convert this descriptor to a JavaScript descriptor object.
     *
     * @param scope the scope in which to create the object
     * @return a JavaScript descriptor object
     */
    public Scriptable toObject(VarScope scope) {
        ScriptableObject desc = new NativeObject();
        ScriptRuntime.setBuiltinProtoAndParent(desc, scope, TopLevel.Builtins.Object);
        if (hasValue()) desc.defineProperty("value", value, ScriptableObject.EMPTY);
        if (hasWritable()) desc.defineProperty("writable", writable, ScriptableObject.EMPTY);
        if (hasGetter()) desc.defineProperty("get", getter, ScriptableObject.EMPTY);
        if (hasSetter()) desc.defineProperty("set", setter, ScriptableObject.EMPTY);
        if (hasEnumerable()) desc.defineProperty("enumerable", enumerable, ScriptableObject.EMPTY);
        if (hasConfigurable())
            desc.defineProperty("configurable", configurable, ScriptableObject.EMPTY);
        return desc;
    }

    public boolean isWritable() {
        return Boolean.TRUE.equals(writable);
    }

    public boolean isWritable(boolean value) {
        return ((Boolean) value).equals(writable);
    }

    public boolean hasWritable() {
        return writable != ScriptableObject.NOT_FOUND;
    }

    public boolean isEnumerable() {
        return Boolean.TRUE.equals(enumerable);
    }

    public boolean isEnumerable(boolean value) {
        return ((Boolean) value).equals(enumerable);
    }

    public boolean hasEnumerable() {
        return enumerable != ScriptableObject.NOT_FOUND;
    }

    public boolean isConfigurable() {
        return Boolean.TRUE.equals(configurable);
    }

    public boolean isConfigurable(boolean value) {
        return ((Boolean) value).equals(configurable);
    }

    public boolean hasConfigurable() {
        return configurable != ScriptableObject.NOT_FOUND;
    }

    public boolean hasValue() {
        return value != ScriptableObject.NOT_FOUND;
    }

    public boolean hasGetter() {
        return getter != ScriptableObject.NOT_FOUND;
    }

    public boolean hasSetter() {
        return setter != ScriptableObject.NOT_FOUND;
    }

    public boolean isDataDescriptor() {
        return hasValue() || hasWritable();
    }

    public boolean isAccessorDescriptor() {
        return hasGetter() || hasSetter();
    }

    public boolean isGenericDescriptor() {
        return !isDataDescriptor() && !isAccessorDescriptor();
    }
}
