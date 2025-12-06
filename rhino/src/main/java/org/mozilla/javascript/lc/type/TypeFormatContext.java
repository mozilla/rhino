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
    /// | Array | java.lang.String[] | String[] |
    /// | Parameterized | java.lang.List<java.lang.String> | List<String> |
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
    /// | Array | java.lang.String[] | java.lang.String[] |
    /// | Parameterized | java.lang.List<java.lang.String> | java.util.List |
    /// | Variable | T extends java.lang.String | java.lang.String |
    /// | [TypeInfo#NONE] | (No standard representation) | java.lang.Object |
    TypeFormatContext CLASS_NAME = new ClassNameFormatContext();

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
