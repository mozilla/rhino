package org.mozilla.javascript.lc.type;

import org.mozilla.javascript.lc.type.impl.ClassNameFormatContext;

/**
 * @author ZZZank
 */
public interface TypeFormatContext {
    /**
     * Full feature formatting context
     *
     * <table>
     *   <tr><th>Type</th><th>Full representation</th><th>Representation using this context</th></tr>
     *   <tr><td>Class</td><td>java.lang.String</td><td>java.lang.String</td></tr>
     *   <tr><td>Nested Class</td><td>Entry (in java.util.Map)</td><td>java.util.Map$Entry</td></tr>
     *   <tr><td>Array</td><td>java.lang.String[]</td><td>java.lang.String[]</td></tr>
     *   <tr><td>Parameterized</td><td>java.lang.List&lt;java.lang.String&gt;</td><td>java.util.List&lt;java.lang.String&gt;</td></tr>
     *   <tr><td>Variable</td><td>T extends java.lang.String</td><td>T extends java.lang.String</td></tr>
     *   <tr><td>{@link TypeInfo#NONE}</td><td>(No standard representation)</td><td>?</td></tr>
     * </table>
     */
    TypeFormatContext DEFAULT = Class::getName;

    /**
     * Full feature formatting context with class name simplified
     *
     * <table>
     *   <tr><th>Type</th><th>Full representation</th><th>Representation using this context</th></tr>
     *   <tr><td>Class</td><td>java.lang.String</td><td>String</td></tr>
     *   <tr><td>Nested Class</td><td>Entry (in java.util.Map)</td><td>Entry</td></tr>
     *   <tr><td>Array</td><td>java.lang.String[]</td><td>String[]</td></tr>
     *   <tr><td>Parameterized</td><td>java.lang.List&lt;java.lang.String&gt;</td><td>List&lt;String&gt;</td></tr>
     *   <tr><td>Variable</td><td>T extends java.lang.String</td><td>T extends String</td></tr>
     *   <tr><td>{@link TypeInfo#NONE}</td><td>(No standard representation)</td><td>?</td></tr>
     * </table>
     *
     * @see Class#getSimpleName()
     */
    TypeFormatContext SIMPLE = Class::getSimpleName;

    /**
     * Formatting context that formats every type as the result of {@code type.asClass().getName()}
     *
     * <table>
     *   <tr><th>Type</th><th>Full representation</th><th>Representation using this context</th></tr>
     *   <tr><td>Class</td><td>java.lang.String</td><td>java.lang.String</td></tr>
     *   <tr><td>Nested Class</td><td>Entry (in java.util.Map)</td><td>java.util.Map$Entry</td></tr>
     *   <tr><td>Array</td><td>java.lang.String[]</td><td>[Ljava.lang.String;</td></tr>
     *   <tr><td>Parameterized</td><td>java.lang.List&lt;java.lang.String&gt;</td><td>java.util.List</td></tr>
     *   <tr><td>Variable</td><td>T extends java.lang.String</td><td>java.lang.String</td></tr>
     *   <tr><td>{@link TypeInfo#NONE}</td><td>(No standard representation)</td><td>java.lang.Object</td></tr>
     * </table>
     *
     * @see Class#getName()
     */
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
