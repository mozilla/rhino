package org.mozilla.javascript.lc.type.impl;

import org.mozilla.javascript.lc.type.ParameterizedTypeInfo;
import org.mozilla.javascript.lc.type.TypeFormatContext;
import org.mozilla.javascript.lc.type.TypeInfo;
import org.mozilla.javascript.lc.type.VariableTypeInfo;

/**
 * @author ZZZank
 */
public class ClassSignatureFormatContext implements TypeFormatContext {
    @Override
    public String getClassName(Class<?> c) {
        return c.getName();
    }

    @Override
    public void append(StringBuilder builder, TypeInfo type) {
        builder.append(type.asClass().getName());
    }

    @Override
    public void appendParameterized(StringBuilder builder, ParameterizedTypeInfo type) {
        append(builder, type.rawType());
    }

    @Override
    public void appendVariable(StringBuilder builder, VariableTypeInfo type) {
        append(builder, type.mainBound());
    }

    @Override
    public void appendArray(StringBuilder builder, TypeInfo type) {
        builder.append(type.asClass().getName());
    }

    @Override
    public String getFormattedNone() {
        return Object.class.getName();
    }
}
