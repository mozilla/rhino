package org.mozilla.javascript.nat.type.impl;

import java.lang.reflect.Array;

import org.mozilla.javascript.nat.type.TypeInfo;
import org.mozilla.javascript.nat.type.format.TypeFormatContext;

public abstract class TypeInfoBase implements TypeInfo {
    private volatile Object emptyArray;

    @Override
    public Object newArray(int length) {
        if (length == 0) {
            if (emptyArray == null) {
                synchronized (this) {
                    if (emptyArray == null) {
                        emptyArray = Array.newInstance(asClass(), 0);
                    }
                }
            }

            return emptyArray;
        }

        return Array.newInstance(asClass(), length);
    }

    @Override
    public String toString() {
        var builder = new StringBuilder();
        append(TypeFormatContext.DEFAULT, builder);
        return builder.toString();
    }
}
