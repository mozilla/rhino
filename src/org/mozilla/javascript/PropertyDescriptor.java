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

import static org.mozilla.javascript.Scriptable.NOT_FOUND;
import static org.mozilla.javascript.ScriptableObject.DONTENUM;
import static org.mozilla.javascript.ScriptableObject.EMPTY;
import static org.mozilla.javascript.ScriptableObject.PERMANENT;
import static org.mozilla.javascript.ScriptableObject.READONLY;
import static org.mozilla.javascript.ScriptableObject.ensureScriptableObject;
import static org.mozilla.javascript.ScriptableObject.getProperty;

/**
 * 
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
    private Object value = Undefined.instance;
    private Object getter = Undefined.instance;
    private Object setter = Undefined.instance;
    private int attributes = READONLY | DONTENUM | PERMANENT;

    private PropertyDescriptor() {
    }

    PropertyDescriptor(PropertyDescriptor desc) {
        this.present = desc.present;
        this.value = desc.value;
        this.getter = desc.getter;
        this.setter = desc.setter;
        this.attributes = desc.attributes;
    }

    PropertyDescriptor(Object value) {
        this.value = value;
        this.present = VALUE;
    }

    PropertyDescriptor(Object value, int attributes) {
        this.value = value;
        this.attributes = attributes;
        this.present = VALUE | WRITABLE | ENUMERABLE | CONFIGURABLE;
    }

    PropertyDescriptor(Object getter, Object setter) {
        this.getter = getter;
        this.setter = setter;
        this.present = GET | SET;
    }

    PropertyDescriptor(Object getter, Object setter, int attributes) {
        this.getter = getter;
        this.setter = setter;
        this.attributes = attributes;
        this.present = GET | SET | ENUMERABLE | CONFIGURABLE;
    }

    static Object fromPropertyDescriptor(Context cx, Scriptable scope,
            PropertyDescriptor desc) {
        if (desc == null) {
            return Undefined.instance;
        }
        // FromPropertyDescriptor expects a fully populated descriptor
        int present = desc.getPresent();
        if ((present & ~POPULATED_ACCESSOR_DESC) != 0
                && (present & ~POPULATED_DATA_DESC) != 0) {
            throw new IllegalArgumentException(String.valueOf(present));
        }

        ScriptableObject obj = ensureScriptableObject(cx.newObject(scope));
        if (desc.isDataDescriptor()) {
            obj.defineProperty("value", desc.getValue(), EMPTY);
            obj.defineProperty("writable", desc.isWritable(), EMPTY);
        } else {
            obj.defineProperty("get", desc.getGetter(), EMPTY);
            obj.defineProperty("set", desc.getSetter(), EMPTY);
        }
        obj.defineProperty("enumerable", desc.isEnumerable(), EMPTY);
        obj.defineProperty("configurable", desc.isConfigurable(), EMPTY);
        return obj;
    }

    static PropertyDescriptor toPropertyDescriptor(Object object) {
        ScriptableObject obj = ensureScriptableObject(object);
        PropertyDescriptor desc = new PropertyDescriptor();
        if (ScriptableObject.hasProperty(obj, "enumerable")) {
            Object enumerable = getProperty(obj, "enumerable");
            if (enumerable == NOT_FOUND) enumerable = Boolean.FALSE;
            desc.setEnumerable(ScriptRuntime.toBoolean(enumerable));
        }
        if (ScriptableObject.hasProperty(obj, "configurable")) {
            Object configurable = getProperty(obj, "configurable");
            if (configurable == NOT_FOUND) configurable = Boolean.FALSE;
            desc.setConfigurable(ScriptRuntime.toBoolean(configurable));
        }
        if (ScriptableObject.hasProperty(obj, "value")) {
            Object value = ScriptableObject.getProperty(obj, "value");
            if (value == NOT_FOUND) value = Undefined.instance;
            desc.setValue(value);
        }
        if (ScriptableObject.hasProperty(obj, "writable")) {
            Object writable = getProperty(obj, "writable");
            if (writable == NOT_FOUND) writable = Boolean.FALSE;
            desc.setWritable(ScriptRuntime.toBoolean(writable));
        }
        if (ScriptableObject.hasProperty(obj, "get")) {
            Object getter = ScriptableObject.getProperty(obj, "get");
            if (getter == NOT_FOUND) getter = Undefined.instance;
            if (getter != Undefined.instance && !(getter instanceof Callable)) {
                throw ScriptRuntime.notFunctionError(getter);
            }
            desc.setGetter(getter);
        }
        if (ScriptableObject.hasProperty(obj, "set")) {
            Object setter = ScriptableObject.getProperty(obj, "set");
            if (setter == NOT_FOUND) setter = Undefined.instance;
            if (setter != Undefined.instance && !(setter instanceof Callable)) {
                throw ScriptRuntime.notFunctionError(setter);
            }
            desc.setSetter(setter);
        }
        if ((desc.present & (GET | SET)) != 0
                && (desc.present & (VALUE | WRITABLE)) != 0) {
            throw ScriptRuntime.typeError0("msg.both.data.and.accessor.desc");
        }
        return desc;
    }

    public boolean isAccessorDescriptor() {
        return (present & (GET | SET)) != 0;
    }

    public boolean isDataDescriptor() {
        return (present & (VALUE | WRITABLE)) != 0;
    }

    public boolean isGenericDescriptor() {
        return (present & (GET | SET | VALUE | WRITABLE)) == 0;
    }

    public int getAttributes() {
        return attributes;
    }

    public int getAttributeMask() {
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

    public int getPresent() {
        return present;
    }

    public boolean hasValue() {
        return (present & VALUE) != 0;
    }

    public boolean hasGetter() {
        return (present & GET) != 0;
    }

    public boolean hasSetter() {
        return (present & SET) != 0;
    }

    public boolean hasWritable() {
        return (present & WRITABLE) != 0;
    }

    public boolean hasEnumerable() {
        return (present & ENUMERABLE) != 0;
    }

    public boolean hasConfigurable() {
        return (present & CONFIGURABLE) != 0;
    }

    /**
     * @return the value
     */
    public Object getValue() {
        return value;
    }

    /**
     * @param value
     *            the value to set
     */
    public void setValue(Object value) {
        present |= VALUE;
        this.value = value;
    }

    /**
     * @return the getter
     */
    public Object getGetter() {
        return getter;
    }

    /**
     * @param getter
     *            the getter to set
     */
    public void setGetter(Object getter) {
        present |= GET;
        this.getter = getter;
    }

    /**
     * @return the setter
     */
    public Object getSetter() {
        return setter;
    }

    /**
     * @param setter
     *            the setter to set
     */
    public void setSetter(Object setter) {
        present |= SET;
        this.setter = setter;
    }

    /**
     * @return the writable
     */
    public boolean isWritable() {
        return (attributes & READONLY) == 0;
    }

    /**
     * @param writable
     *            the writable to set
     */
    public void setWritable(boolean writable) {
        present |= WRITABLE;
        attributes = (writable ? attributes & ~READONLY : attributes | READONLY);
    }

    /**
     * @return the enumerable
     */
    public boolean isEnumerable() {
        return (attributes & DONTENUM) == 0;
    }

    /**
     * @param enumerable
     *            the enumerable to set
     */
    public void setEnumerable(boolean enumerable) {
        present |= ENUMERABLE;
        attributes = (enumerable ? attributes & ~DONTENUM : attributes
                | DONTENUM);
    }

    /**
     * @return the configurable
     */
    public boolean isConfigurable() {
        return (attributes & PERMANENT) == 0;
    }

    /**
     * @param configurable
     *            the configurable to set
     */
    public void setConfigurable(boolean configurable) {
        present |= CONFIGURABLE;
        attributes = (configurable ? attributes & ~PERMANENT : attributes
                | PERMANENT);
    }
}
