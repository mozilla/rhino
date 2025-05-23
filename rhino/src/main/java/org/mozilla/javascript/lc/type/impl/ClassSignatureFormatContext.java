package org.mozilla.javascript.lc.type.impl;

import org.mozilla.javascript.lc.type.ParameterizedTypeInfo;
import org.mozilla.javascript.lc.type.TypeFormatContext;

/**
 * @author ZZZank
 */
public class ClassSignatureFormatContext implements TypeFormatContext {
    @Override
    public String getClassName(Class<?> c) {
        return c.getName();
    }

    @Override
    public void appendSpace(StringBuilder builder) {}

    @Override
    public void formatParameterized(StringBuilder builder, ParameterizedTypeInfo type) {
        type.rawType().append(this, builder);
    }

    @Override
    public String getFormattedNone() {
        return Object.class.getName();
    }
}
