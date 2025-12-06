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

    default void append(StringBuilder builder, TypeInfo type) {
        if (type == TypeInfo.NONE) {
            builder.append(getFormattedNone());
        } else if (type.isArray()) {
            appendArray(builder, type);
        } else if (type instanceof VariableTypeInfo) {
            appendVariable(builder, (VariableTypeInfo) type);
        } else if (type instanceof ParameterizedTypeInfo) {
            appendParameterized(builder, (ParameterizedTypeInfo) type);
        } else {
            builder.append(type.toString(this));
        }
    }

    default void appendArray(StringBuilder builder, TypeInfo type) {
        append(builder, type.getComponentType());
        builder.append('[').append(']');
    }

    default void appendParameterized(StringBuilder builder, ParameterizedTypeInfo type) {
        append(builder, type.rawType());

        builder.append('<');
        var iterator = type.params().iterator();
        if (iterator.hasNext()) {
            append(builder, iterator.next());
            while (iterator.hasNext()) {
                builder.append(',');
                builder.append(' ');
                append(builder, iterator.next());
            }
        }
        builder.append('>');
    }

    default void appendVariable(StringBuilder builder, VariableTypeInfo type) {
        builder.append(type.name());
        var mainBound = type.mainBound();
        if (!mainBound.isObjectExact()) {
            builder.append(" extends ");
            append(builder, mainBound);
        }
    }

    /**
     * @see org.mozilla.javascript.lc.type.impl.NoTypeInfo
     * @see org.mozilla.javascript.lc.type.TypeInfo#NONE
     */
    default String getFormattedNone() {
        return "?";
    }
}
