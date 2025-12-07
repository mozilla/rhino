package org.mozilla.javascript.lc.type;

import org.mozilla.javascript.lc.type.impl.ClassNameFormatContext;

/**
 * @author ZZZank
 */
public interface TypeFormatContext {
    /// Full feature formatting context
    ///
    /// | Type | Full representation | Representation using this context |
    /// | - | - | - |
    /// | Class | java.lang.String | java.lang.String |
    /// | Nested Class | Entry (in java.util.Map) | java.util.Map$Entry |
    /// | Array | java.lang.String[] | java.lang.String[] |
    /// | Parameterized | java.lang.List<java.lang.String> | java.util.List<java.lang.String> |
    /// | Variable | T extends java.lang.String | T extends java.lang.String |
    /// | [TypeInfo#NONE] | (No standard representation) | ? |
    TypeFormatContext DEFAULT = Class::getName;
    /// Full feature formatting context with class name simplified
    ///
    /// | Type | Full representation | Representation using this context |
    /// | - | - | - |
    /// | Class | java.lang.String | String |
    /// | Nested Class | Entry (in java.util.Map) | Entry |
    /// | Array | java.lang.String[] | String[] |
    /// | Parameterized | java.lang.List<java.lang.String> | List\<String> |
    /// | Variable | T extends java.lang.String | T extends String |
    /// | [TypeInfo#NONE] | (No standard representation) | ? |
    ///
    /// @see Class#getSimpleName()
    TypeFormatContext SIMPLE = Class::getSimpleName;
    /// Formatting context that formats every type as the result of `type.asClass().getName()`
    ///
    /// | Type | Full representation | Representation using this context |
    /// | - | - | - |
    /// | Class | java.lang.String | java.lang.String |
    /// | Nested Class | Entry (in java.util.Map) | java.util.Map$Entry |
    /// | Array | java.lang.String[] | [Ljava.lang.String; |
    /// | Parameterized | java.lang.List<java.lang.String> | java.util.List |
    /// | Variable | T extends java.lang.String | java.lang.String |
    /// | [TypeInfo#NONE] | (No standard representation) | java.lang.Object |
    ///
    /// @see Class#getName()
    TypeFormatContext CLASS_NAME = new ClassNameFormatContext();

    String getClassName(Class<?> c);

    /**
     * Format a type and push the result to a {@link StringBuilder}.
     *
     * @implNote Implementations are encouraged to override {@link #append(StringBuilder, TypeInfo,
     *     boolean)} , instead of this method
     * @param builder Formatted string of {@code type} will be pushed to this builder
     * @param type The type to be formatted
     */
    default void append(StringBuilder builder, TypeInfo type) {
        append(builder, type, false);
    }

    /**
     * This method is for overriding. Users are encouraged to use {@link #append(StringBuilder,
     * TypeInfo)} instead of this method.
     *
     * @param builder Formatted string of {@code type} will be pushed to this builder
     * @param type The type to be formatted
     * @param isComponent {@code true} if the {@code type} is a component of another type. For
     *     example, {@code T} in {@code T[]}, and {@code Number} in {@code Map<K, Number>}
     */
    default void append(StringBuilder builder, TypeInfo type, boolean isComponent) {
        if (type == TypeInfo.NONE) {
            builder.append(getFormattedNone());
        } else if (type.isArray()) {
            appendArray(builder, type);
        } else if (type instanceof VariableTypeInfo) {
            if (isComponent) {
                builder.append(((VariableTypeInfo) type).name());
            } else {
                appendVariable(builder, (VariableTypeInfo) type);
            }
        } else if (type instanceof ParameterizedTypeInfo) {
            appendParameterized(builder, (ParameterizedTypeInfo) type);
        } else {
            builder.append(type.toString(this));
        }
    }

    default void appendArray(StringBuilder builder, TypeInfo type) {
        append(builder, type.getComponentType(), true);
        builder.append('[').append(']');
    }

    default void appendParameterized(StringBuilder builder, ParameterizedTypeInfo type) {
        append(builder, type.rawType(), true);

        var iterator = type.params().iterator();
        if (iterator.hasNext()) {
            builder.append('<');
            append(builder, iterator.next(), true);
            while (iterator.hasNext()) {
                builder.append(',');
                builder.append(' ');
                append(builder, iterator.next(), true);
            }
            builder.append('>');
        }
    }

    default void appendVariable(StringBuilder builder, VariableTypeInfo type) {
        builder.append(type.name());
        var mainBound = type.mainBound();
        if (!mainBound.isObjectExact()) {
            builder.append(" extends ");
            append(builder, mainBound, true);
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
