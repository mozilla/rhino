package org.mozilla.javascript.lc.type.impl;

import java.lang.reflect.Array;
import org.mozilla.javascript.lc.type.TypeFormatContext;
import org.mozilla.javascript.lc.type.TypeInfo;

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
    public final String toString() {
        return toString(TypeFormatContext.DEFAULT);
    }

    @Override
    public String toString(TypeFormatContext ctx) {
        var builder = new StringBuilder();
        ctx.append(builder, this);
        return builder.toString();
    }
}
