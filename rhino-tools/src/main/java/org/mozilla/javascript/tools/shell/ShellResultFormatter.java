/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tools.shell;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

final class ShellResultFormatter {
    private static final int MAX_DEPTH = 4;
    private static final int MAX_ENTRIES = 20;

    private final Set<ScriptableObject> ancestors =
            Collections.newSetFromMap(new IdentityHashMap<>());

    static String format(Object value) {
        return new ShellResultFormatter().formatValue(value, 0);
    }

    private String formatValue(Object value, int depth) {
        if (value == Scriptable.NOT_FOUND) {
            return "<empty>";
        }
        if (ScriptRuntime.isSymbol(value)) {
            return value.toString();
        }
        if (value instanceof NativeArray || value instanceof NativeObject) {
            return formatContainer((ScriptableObject) value, depth);
        }
        if (depth > 0 && value instanceof CharSequence) {
            return quote(value.toString());
        }
        return Context.toString(value);
    }

    private String formatContainer(ScriptableObject value, int depth) {
        if (ancestors.contains(value)) {
            return "[Circular]";
        }
        if (depth >= MAX_DEPTH) {
            return "[" + value.getClassName() + "]";
        }
        ancestors.add(value);
        try {
            if (value instanceof NativeArray array) {
                return formatArray(array, depth + 1);
            }
            return formatObject(value, depth + 1);
        } finally {
            ancestors.remove(value);
        }
    }

    private String formatArray(NativeArray array, int depth) {
        StringBuilder result = new StringBuilder("[");
        long length = array.getLength();
        int limit = (int) Math.min(length, MAX_ENTRIES);
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                result.append(", ");
            }
            result.append(formatProperty(array, null, i, depth));
        }
        if (length > limit) {
            result.append(", ...");
        }
        return result.append(']').toString();
    }

    private String formatObject(ScriptableObject value, int depth) {
        StringBuilder result = new StringBuilder("{");
        Object[] ids = value.getIds();
        int limit = Math.min(ids.length, MAX_ENTRIES);
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                result.append(", ");
            }
            Object id = ids[i];
            if (id instanceof Integer index) {
                result.append(index).append(": ").append(formatProperty(value, null, index, depth));
            } else {
                String name = (String) id;
                result.append(quote(name))
                        .append(": ")
                        .append(formatProperty(value, name, 0, depth));
            }
        }
        if (ids.length > limit) {
            result.append(", ...");
        }
        return result.append('}').toString();
    }

    private String formatProperty(ScriptableObject value, String name, int index, int depth) {
        if (value.getGetterOrSetter(name, index, value, false) instanceof Function) {
            return "[Getter]";
        }
        if (value.getGetterOrSetter(name, index, value, true) instanceof Function) {
            return "[Setter]";
        }
        return formatValue(name == null ? value.get(index, value) : value.get(name, value), depth);
    }

    private static String quote(String value) {
        return '"' + ScriptRuntime.escapeString(value) + '"';
    }
}
