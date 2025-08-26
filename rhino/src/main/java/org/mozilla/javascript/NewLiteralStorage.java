/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static org.mozilla.javascript.NativeObject.PROTO_PROPERTY;

import java.util.Arrays;

/** Used to store the support structures for a literal object (or array) being built. */
public final class NewLiteralStorage {
    private Object[] keys;
    private int[] getterSetters;
    private Object[] values;
    private int index = 0;

    public NewLiteralStorage(Object[] ids) {
        this.keys = ids;
        this.getterSetters = new int[ids.length];
        this.values = new Object[ids.length];
    }

    public NewLiteralStorage(int length, boolean createKeys) {
        this.keys = createKeys ? new Object[length] : null;
        this.getterSetters = new int[length];
        this.values = new Object[length];
    }

    public void pushValue(Object value) {
        values[index] = value;
        attemptToInferFunctionName(value);
        ++index;
    }

    private void attemptToInferFunctionName(Object value) {
        // Try to infer the name if the value is a normal JS function
        if (this.keys == null
                || (!(value instanceof NativeFunction) && !(value instanceof ArrowFunction))) {
            return;
        }

        BaseFunction fun = (BaseFunction) value;
        if (!"".equals(fun.get("name", fun))) {
            return;
        }

        String prefix = "";
        if (getterSetters[index] == -1) {
            prefix = "get ";
        } else if (getterSetters[index] == +1) {
            prefix = "set ";
        }

        Object propKey = this.keys[index];
        if (propKey instanceof Symbol) {
            // For symbol keys, valid names are: `[foo]`, `get [foo]`
            // However `[]` or `get []` aren't, and become `` and `get `
            String symbolName = ((Symbol) propKey).getName();
            if (!symbolName.isEmpty()) {
                fun.setFunctionName(prefix + "[" + symbolName + "]");
            } else if (!prefix.isEmpty()) {
                fun.setFunctionName(prefix);
            }
        } else {
            // Key was already converted to a string
            if (!propKey.equals(PROTO_PROPERTY)) {
                fun.setFunctionName(prefix + propKey);
            } else {
                // `__proto__` is, as usual, weird and applies only to methods, meaning:
                // - { __proto__(){} } infers the name
                // - { __proto__: function(){} } does not!
                if (fun instanceof NativeFunction && ((NativeFunction) fun).isShorthand()) {
                    fun.setFunctionName(prefix + propKey);
                }
            }
        }
    }

    public void pushGetter(Object value) {
        getterSetters[index] = -1;
        pushValue(value);
    }

    public void pushSetter(Object value) {
        getterSetters[index] = +1;
        pushValue(value);
    }

    public void pushKey(Object key) {
        if (key instanceof Symbol) {
            keys[index] = key;
        } else {
            keys[index] = ScriptRuntime.toString(key);
        }
    }

    public void spread(Context cx, Scriptable scope, Object source) {
        // See ECMAScript 13.2.5.5
        if (source != null && !Undefined.isUndefined(source)) {
            Scriptable src = ScriptRuntime.toObject(cx, scope, source);
            Object[] ids;
            if (src instanceof ScriptableObject) {
                ids = ((ScriptableObject) src).getIds(false, true);
            } else {
                ids = src.getIds();
            }

            // Resize all the arrays
            int newLen = values.length + ids.length;
            keys = Arrays.copyOf(keys, newLen);
            getterSetters = Arrays.copyOf(getterSetters, newLen);
            values = Arrays.copyOf(values, newLen);

            // getIds() can only return a string, int or a symbol
            for (Object id : ids) {
                Object value;
                if (id instanceof String) {
                    value = ScriptableObject.getProperty(src, (String) id);
                } else if (id instanceof Integer) {
                    value = ScriptableObject.getProperty(src, (int) id);
                } else if (ScriptRuntime.isSymbol(id)) {
                    value = ScriptableObject.getProperty(src, (Symbol) id);
                } else {
                    throw Kit.codeBug();
                }

                pushKey(id);
                pushValue(value);
            }
        }
    }

    public Object[] getKeys() {
        return keys;
    }

    public int[] getGetterSetters() {
        return getterSetters;
    }

    public Object[] getValues() {
        return values;
    }
}
