/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1999.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1997-1999
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * André Bargull
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU General Public License Version 2 or later (the "GPL"), in which
 * case the provisions of the GPL are applicable instead of those above. If
 * you wish to allow use of your version of this file only under the terms of
 * the GPL and not to allow others to use your version of this file under the
 * MPL, indicate your decision by deleting the provisions above and replacing
 * them with the notice and other provisions required by the GPL. If you do
 * not delete the provisions above, a recipient may use your version of this
 * file under either the MPL or the GPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.javascript;

import static org.mozilla.javascript.ScriptableObject.DONTENUM;
import static org.mozilla.javascript.ScriptableObject.PERMANENT;
import static org.mozilla.javascript.ScriptableObject.READONLY;
import static org.mozilla.javascript.ScriptableObject.ensureScriptable;
import static org.mozilla.javascript.ScriptableObject.getProperty;

/**
 * Property Descriptor per ECMAScript 5 [8.10]
 *
 */
class PropertyDescriptor {
    private static final int VALUE = 0x01;
    private static final int GET = 0x02;
    private static final int SET = 0x04;
    private static final int WRITABLE = 0x08;
    private static final int ENUMERABLE = 0x10;
    private static final int CONFIGURABLE = 0x20;

    private static final int POPULATED_ACCESSOR_DESC = GET | SET | ENUMERABLE
            | CONFIGURABLE;
    private static final int POPULATED_DATA_DESC = VALUE | WRITABLE
            | ENUMERABLE | CONFIGURABLE;

    private int present = 0;
    // default attribute values per 8.6.1, table 7
    private Object value = Undefined.instance;
    private Object getter = Undefined.instance;
    private Object setter = Undefined.instance;
    private int attributes = READONLY | DONTENUM | PERMANENT;

    private PropertyDescriptor() {
    }

    /**
     * Creates a shallow copy of the supplied property descriptor
     */
    PropertyDescriptor(PropertyDescriptor desc) {
        this.present = desc.present;
        this.value = desc.value;
        this.getter = desc.getter;
        this.setter = desc.setter;
        this.attributes = desc.attributes;
    }

    /**
     * Creates a new data property descriptor with an initial value:<br>
     * <code>{[[Value]]: ?}</code>
     */
    PropertyDescriptor(Object value) {
        this.value = value;
        this.present = VALUE;
    }

    /**
     * Creates a new data property descriptor with an initial value and initial
     * attributes:<br>
     * <code>{[[Value]]: ?, [[Writable]]: ?, [[Enumerable]]: ?,
     * [[Configurable]]: ?}</code>
     */
    PropertyDescriptor(Object value, int attributes) {
        this.value = value;
        this.attributes = attributes;
        this.present = VALUE | WRITABLE | ENUMERABLE | CONFIGURABLE;
    }

    /**
     * Creates a new accessor property descriptor with initial getter and
     * setter:<br>
     * <code>{[[Get]]: ?, [[Set]]: ?}</code>
     */
    PropertyDescriptor(Object getter, Object setter) {
        this.getter = getter;
        this.setter = setter;
        this.present = GET | SET;
    }

    /**
     * Creates a new accessor property descriptor with initial getter and setter
     * and initial attributes:<br>
     * <code>{[[Get]]: ?, [[Set]]: ?, [[Enumerable]]: ?, [[Configurable]]: ?}
     * </code>
     */
    PropertyDescriptor(Object getter, Object setter, int attributes) {
        this.getter = getter;
        this.setter = setter;
        this.attributes = attributes;
        this.present = GET | SET | ENUMERABLE | CONFIGURABLE;
    }

    /**
     * ECMAScript 5: 8.10.4 FromPropertyDescriptor (Desc)<br>
     * Returns {@code undefined} if the input property descriptor is
     * {@code null}, otherwise returns a {@link Scriptable} representing the
     * fields of this property descriptor.
     */
    static Object fromPropertyDescriptor(Context cx, Scriptable scope,
            PropertyDescriptor desc) throws IllegalArgumentException {
        /* 8.10.4, step 1. */
        if (desc == null) {
            return Undefined.instance;
        }
        // FromPropertyDescriptor expects a fully populated descriptor
        int present = desc.getPresent();
        if ((present & ~POPULATED_ACCESSOR_DESC) != 0
                && (present & ~POPULATED_DATA_DESC) != 0) {
            throw new IllegalArgumentException(String.valueOf(present));
        }

        /* 8.10.4, step 2. */
        Scriptable obj = ensureScriptable(cx.newObject(scope));
        /* 8.10.4, step 3-6. */
        if (desc.isDataDescriptor()) {
            obj.put("value", obj, desc.getValue(), false);
            obj.put("writable", obj, desc.isWritable(), false);
        } else {
            obj.put("get", obj, desc.getGetter(), false);
            obj.put("set", obj, desc.getSetter(), false);
        }
        obj.put("enumerable", obj, desc.isEnumerable(), false);
        obj.put("configurable", obj, desc.isConfigurable(), false);
        /* 8.10.4, step 7. */
        return obj;
    }

    /**
     * ECMAScript 5: 8.10.5 ToPropertyDescriptor (Desc)<br>
     * Returns a new property descriptor from the input argument {@code object},
     * if {@code object} is not an instance of {@link Scriptable}, a TypeError
     * is thrown.
     */
    static PropertyDescriptor toPropertyDescriptor(Object object) {
        /* 8.10.5, step 1. */
        Scriptable obj = ensureScriptable(object);
        /* 8.10.5, step 2-8. */
        PropertyDescriptor desc = new PropertyDescriptor();
        if (ScriptableObject.hasProperty(obj, "enumerable")) {
            Object enumerable = getProperty(obj, "enumerable");
            desc.setEnumerable(ScriptRuntime.toBoolean(enumerable));
        }
        if (ScriptableObject.hasProperty(obj, "configurable")) {
            Object configurable = getProperty(obj, "configurable");
            desc.setConfigurable(ScriptRuntime.toBoolean(configurable));
        }
        if (ScriptableObject.hasProperty(obj, "value")) {
            Object value = ScriptableObject.getProperty(obj, "value");
            desc.setValue(value);
        }
        if (ScriptableObject.hasProperty(obj, "writable")) {
            Object writable = getProperty(obj, "writable");
            desc.setWritable(ScriptRuntime.toBoolean(writable));
        }
        if (ScriptableObject.hasProperty(obj, "get")) {
            Object getter = ScriptableObject.getProperty(obj, "get");
            if (getter != Undefined.instance && !(getter instanceof Callable)) {
                throw ScriptRuntime.notFunctionError(getter);
            }
            desc.setGetter(getter);
        }
        if (ScriptableObject.hasProperty(obj, "set")) {
            Object setter = ScriptableObject.getProperty(obj, "set");
            if (setter != Undefined.instance && !(setter instanceof Callable)) {
                throw ScriptRuntime.notFunctionError(setter);
            }
            desc.setSetter(setter);
        }
        /* 8.10.5, step 9. */
        if ((desc.present & (GET | SET)) != 0
                && (desc.present & (VALUE | WRITABLE)) != 0) {
            throw ScriptRuntime.typeError0("msg.both.data.and.accessor.desc");
        }
        /* 8.10.5, step 10. */
        return desc;
    }

    /**
     * ECMAScript 5: 8.10.1 IsAccessorDescriptor (Desc)<br>
     * Returns {@code true} if this object is an accessor property descriptor
     */
    public final boolean isAccessorDescriptor() {
        return (present & (GET | SET)) != 0;
    }

    /**
     * ECMAScript 5: 8.10.2 IsDataDescriptor (Desc)<br>
     * Returns {@code true} if this object is a data property descriptor
     */
    public final boolean isDataDescriptor() {
        return (present & (VALUE | WRITABLE)) != 0;
    }

    /**
     * ECMAScript 5: 8.10.3 IsGenericDescriptor (Desc)<br>
     * Returns {@code true} if this object is a generic property descriptor
     */
    public final boolean isGenericDescriptor() {
        return (present & (GET | SET | VALUE | WRITABLE)) == 0;
    }

    /**
     * Returns the attribute set of this property descriptor
     *
     * @see ScriptableObject#READONLY
     * @see ScriptableObject#DONTENUM
     * @see ScriptableObject#PERMANENT
     */
    public final int getAttributes() {
        return attributes;
    }

    /**
     * Returns a bit mask of the currently set attributes
     *
     * @see ScriptableObject#READONLY
     * @see ScriptableObject#DONTENUM
     * @see ScriptableObject#PERMANENT
     */
    public final int getAttributeMask() {
        int mask = 0;
        if ((present & WRITABLE) != 0) {
            mask |= READONLY;
        }
        if ((present & ENUMERABLE) != 0) {
            mask |= DONTENUM;
        }
        if ((present & CONFIGURABLE) != 0) {
            mask |= PERMANENT;
        }
        return mask;
    }

    /**
     * Returns a bit mask of the currently set fields of this property
     * descriptor
     */
    public final int getPresent() {
        return present;
    }

    /**
     * Returns {@code true} if the <tt>[[Value]]</tt> field is present
     */
    public final boolean hasValue() {
        return (present & VALUE) != 0;
    }

    /**
     * Returns {@code true} if the <tt>[[Get]]</tt> field is present
     */
    public final boolean hasGetter() {
        return (present & GET) != 0;
    }

    /**
     * Returns {@code true} if the <tt>[[Set]]</tt> field is present
     */
    public final boolean hasSetter() {
        return (present & SET) != 0;
    }

    /**
     * Returns {@code true} if the <tt>[[Writable]]</tt> field is present
     */
    public final boolean hasWritable() {
        return (present & WRITABLE) != 0;
    }

    /**
     * Returns {@code true} if the <tt>[[Enumerable]]</tt> field is present
     */
    public final boolean hasEnumerable() {
        return (present & ENUMERABLE) != 0;
    }

    /**
     * Returns {@code true} if the <tt>[[Configurable]]</tt> field is present
     */
    public final boolean hasConfigurable() {
        return (present & CONFIGURABLE) != 0;
    }

    /**
     * Returns the <tt>[[Value]]</tt> field or its default value
     */
    public final Object getValue() {
        return value;
    }

    /**
     * Sets the <tt>[[Value]]</tt> field to the argument value
     */
    public final void setValue(Object value) {
        present |= VALUE;
        this.value = value;
    }

    /**
     * Returns the <tt>[[Get]]</tt> field or its default value
     */
    public final Object getGetter() {
        return getter;
    }

    /**
     * Sets the <tt>[[Get]]</tt> field to the argument value
     */
    public final void setGetter(Object getter) {
        present |= GET;
        this.getter = getter;
    }

    /**
     * Returns the <tt>[[Set]]</tt> field or its default value
     */
    public final Object getSetter() {
        return setter;
    }

    /**
     * Sets the <tt>[[Set]]</tt> field to the argument value
     */
    public final void setSetter(Object setter) {
        present |= SET;
        this.setter = setter;
    }

    /**
     * Returns the <tt>[[Writable]]</tt> field or its default value
     */
    public final boolean isWritable() {
        return (attributes & READONLY) == 0;
    }

    /**
     * Sets the <tt>[[Writable]]</tt> field to the argument value
     */
    public final void setWritable(boolean writable) {
        present |= WRITABLE;
        attributes = (writable ? attributes & ~READONLY : attributes | READONLY);
    }

    /**
     * Returns the <tt>[[Enumerable]]</tt> field or its default value
     */
    public final boolean isEnumerable() {
        return (attributes & DONTENUM) == 0;
    }

    /**
     * Sets the <tt>[[Enumerable]]</tt> field to the argument value
     */
    public final void setEnumerable(boolean enumerable) {
        present |= ENUMERABLE;
        attributes = (enumerable ? attributes & ~DONTENUM : attributes
                | DONTENUM);
    }

    /**
     * Returns the <tt>[[Configurable]]</tt> field or its default value
     */
    public final boolean isConfigurable() {
        return (attributes & PERMANENT) == 0;
    }

    /**
     * Sets the <tt>[[Configurable]]</tt> field to the argument value
     */
    public final void setConfigurable(boolean configurable) {
        present |= CONFIGURABLE;
        attributes = (configurable ? attributes & ~PERMANENT : attributes
                | PERMANENT);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        if (hasValue()) {
            String value = ScriptRuntime.toString(getValue()).trim();
            sb.append("[[Value]]: ").append(value).append(", ");
        }
        if (hasGetter()) {
            String getter = ScriptRuntime.toString(getGetter()).trim();
            sb.append("[[Get]]: ").append(getter).append(", ");
        }
        if (hasSetter()) {
            String setter = ScriptRuntime.toString(getSetter()).trim();
            sb.append("[[Set]]: ").append(setter).append(", ");
        }
        if (hasWritable()) {
            sb.append("[[Writable]]: ").append(isWritable()).append(", ");
        }
        if (hasEnumerable()) {
            sb.append("[[Enumerable]]: ").append(isEnumerable()).append(", ");
        }
        if (hasConfigurable()) {
            sb.append("[[Configurable]]: ").append(isConfigurable()).append(", ");
        }
        if (sb.length() != 1) {
            // if not only initial '{', remove trailing ", "
            sb.setLength(sb.length() - 2);
        }
        sb.append('}');
        return sb.toString();
    }
}
