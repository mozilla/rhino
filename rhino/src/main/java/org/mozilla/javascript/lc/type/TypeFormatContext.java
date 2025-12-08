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
     *   <tr><td>Primitive Class</td><td>char</td><td>char</td></tr>
     *   <tr><td>Nested Class</td><td>Entry (in java.util.Map)</td><td>java.util.Map$Entry</td></tr>
     *   <tr><td>Array</td><td>java.lang.String[]</td><td>java.lang.String[]</td></tr>
     *   <tr><td>Primitive Array</td><td>char[]</td><td>char[]</td></tr>
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
     *   <tr><td>Primitive Class</td><td>char</td><td>char</td></tr>
     *   <tr><td>Nested Class</td><td>Entry (in java.util.Map)</td><td>Entry</td></tr>
     *   <tr><td>Array</td><td>java.lang.String[]</td><td>String[]</td></tr>
     *   <tr><td>Primitive Array</td><td>char[]</td><td>char[]</td></tr>
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
     *   <tr><td>Primitive Class</td><td>char</td><td>char</td></tr>
     *   <tr><td>Nested Class</td><td>Entry (in java.util.Map)</td><td>java.util.Map$Entry</td></tr>
     *   <tr><td>Array</td><td>java.lang.String[]</td><td>[Ljava.lang.String;</td></tr>
     *   <tr><td>Primitive Array</td><td>char[]</td><td>[C</td></tr>
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
     * Format a type and push the result to provided {@link StringBuilder}.
     *
     * @implNote Implementations are encouraged to override {@link #append(StringBuilder, TypeInfo,
     *     boolean)} , instead of this method
     * @param builder Formatted string of {@code type} will be pushed to this builder
     * @param type The type to be formatted
     */
    default void append(StringBuilder builder, TypeInfo type) {
        append(builder, type, true);
    }

    /**
     * Format a type and push the result to provided {@link StringBuilder}.
     *
     * @param builder Formatted string of {@code type} will be pushed to this builder
     * @param type The type to be formatted
     * @param declaring {@code true} if the context should format the result as if the type is being declared instead of being used. For example, in {@code E extends Enum<T>}, this param is {@code true} for the first E, and {@code false} for the second, nested E
     */
    default void append(StringBuilder builder, TypeInfo type, boolean declaring) {
        if (type == TypeInfo.NONE) {
            builder.append(getFormattedNone());
        } else if (type.isArray()) {
            appendArray(builder, type);
        } else if (type instanceof VariableTypeInfo) {
            appendVariable(builder, (VariableTypeInfo) type, declaring);
        } else if (type instanceof ParameterizedTypeInfo) {
            appendParameterized(builder, (ParameterizedTypeInfo) type);
        } else {
            builder.append(type.toString(this));
        }
    }

    /**
     * @param type {@link TypeInfo#isArray()} will always be {@code true} for this object
     */
    default void appendArray(StringBuilder builder, TypeInfo type) {
        append(builder, type.getComponentType(), false);
        builder.append('[').append(']');
    }

    default void appendParameterized(StringBuilder builder, ParameterizedTypeInfo type) {
        append(builder, type.rawType(), false);

        var iterator = type.params().iterator();
        if (iterator.hasNext()) {
            builder.append('<');
            append(builder, iterator.next(), false);
            while (iterator.hasNext()) {
                builder.append(',');
                builder.append(' ');
                append(builder, iterator.next(), false);
            }
            builder.append('>');
        }
    }

    /**
     * @param declaring {@code true} if the context should format the result as if the type is being declared instead of being used. For example, in {@code E extends Enum<T>}, {@code declaring} is {@code true} for the first E, and {@code false} for the second, nested E
     */
    default void appendVariable(StringBuilder builder, VariableTypeInfo type, boolean declaring) {
        builder.append(type.name());
        if (declaring) {
            var mainBound = type.mainBound();
            if (!mainBound.isObjectExact()) {
                builder.append(" extends ");
                append(builder, mainBound, false);
            }
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
