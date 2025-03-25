package org.mozilla.javascript.nat.type.format;

import org.mozilla.javascript.nat.type.definition.ParameterizedTypeInfo;

/**
 * @author ZZZank
 */
public class ClassSignatureFormatContext implements TypeFormatContext {
    @Override
    public String getClassName(Class<?> c) {
        return c.getName();
    }

    @Override
    public void appendSpace(StringBuilder builder) {
    }

    @Override
    public void formatParameterized(StringBuilder builder, ParameterizedTypeInfo type) {
        type.rawType().append(this, builder);
    }

    @Override
    public String getFormattedNone() {
        return Object.class.getName();
    }
}
