package org.mozilla.javascript.lc.type;

import org.mozilla.javascript.lc.type.impl.ClassSignatureFormatContext;

/**
 * @author ZZZank
 */
public interface TypeFormatContext {
    TypeFormatContext DEFAULT = Class::getName;
    TypeFormatContext SIMPLE = Class::getSimpleName;
    TypeFormatContext CLASS_SIG = new ClassSignatureFormatContext();

    String getClassName(Class<?> c);

    default void appendSpace(StringBuilder builder) {
        builder.append(' ');
    }

    default void formatArray(StringBuilder builder, TypeInfo arrayType) {
        arrayType.getComponentType().append(this, builder);
        builder.append('[').append(']');
    }

    default void formatParameterized(StringBuilder builder, ParameterizedTypeInfo type) {
        type.rawType().append(this, builder);

        builder.append('<');
        var iterator = type.params().iterator();
        if (iterator.hasNext()) {
            iterator.next().append(this, builder);
            while (iterator.hasNext()) {
                builder.append(',');
                appendSpace(builder);
                iterator.next().append(this, builder);
            }
        }
        builder.append('>');
    }

    /**
     * @see org.mozilla.javascript.lc.type.impl.NoTypeInfo
     * @see org.mozilla.javascript.lc.type.TypeInfo#NONE
     */
    default String getFormattedNone() {
        return "?";
    }
}
