package org.mozilla.javascript.tests.type_info;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.lc.type.TypeFormatContext;
import org.mozilla.javascript.lc.type.TypeInfo;
import org.mozilla.javascript.lc.type.TypeInfoFactory;

/**
 * @author ZZZank
 */
public class TypeFormatContextTest<T> {

    @Test
    public void defaultContext() throws Exception {
        var cx = TypeFormatContext.DEFAULT;
        for (var method : collectTestMethods()) {
            var rawType = method.getGenericParameterTypes()[0];
            var formatted = TypeInfoFactory.GLOBAL.create(rawType).toString(cx);

            @SuppressWarnings("unchecked")
            var expectedByContext =
                    (Map<TypeFormatContext, String>) method.invoke(null, new Object[] {null});
            Assertions.assertEquals(
                    expectedByContext.get(cx),
                    formatted,
                    () ->
                            String.format(
                                    "Testing type '%s' on context 'DEFAULT'\nExpected: %s\nActual: '%s'",
                                    rawType, expectedByContext.get(cx), formatted));
        }
    }

    @Test
    public void simpleContext() throws Exception {
        var cx = TypeFormatContext.SIMPLE;
        for (var method : collectTestMethods()) {
            var rawType = method.getGenericParameterTypes()[0];
            var formatted = TypeInfoFactory.GLOBAL.create(rawType).toString(cx);

            @SuppressWarnings("unchecked")
            var expectedByContext =
                    (Map<TypeFormatContext, String>) method.invoke(null, new Object[] {null});
            Assertions.assertEquals(
                    expectedByContext.get(cx),
                    formatted,
                    () ->
                            String.format(
                                    "Testing type '%s' on context 'SIMPLE'\nExpected: %s\nActual: '%s'",
                                    rawType, expectedByContext.get(cx), formatted));
        }
    }

    @Test
    public void classNameContext() throws Exception {
        var cx = TypeFormatContext.CLASS_NAME;
        for (var method : collectTestMethods()) {
            var rawType = method.getGenericParameterTypes()[0];
            var formatted = TypeInfoFactory.GLOBAL.create(rawType).toString(cx);

            @SuppressWarnings("unchecked")
            var expectedByContext =
                    (Map<TypeFormatContext, String>) method.invoke(null, new Object[] {null});
            Assertions.assertEquals(
                    expectedByContext.get(cx),
                    formatted,
                    () ->
                            String.format(
                                    "Testing type '%s' on context 'CLASS_NAME'\nExpected: %s\nActual: '%s'",
                                    rawType, expectedByContext.get(cx), formatted));
        }
    }

    private Iterable<Method> collectTestMethods() {
        return Arrays.stream(TestCases.class.getDeclaredMethods())
                        .filter(method -> !method.isSynthetic())
                        .filter(
                                method -> {
                                    var modifier = method.getModifiers();
                                    return Modifier.isPublic(modifier)
                                            && Modifier.isStatic(modifier);
                                })
                ::iterator;
    }

    @Test
    public void noneType() {
        Assertions.assertEquals("?", TypeInfo.NONE.toString(TypeFormatContext.DEFAULT));
        Assertions.assertEquals("?", TypeInfo.NONE.toString(TypeFormatContext.SIMPLE));
        Assertions.assertEquals(
                "java.lang.Object", TypeInfo.NONE.toString(TypeFormatContext.CLASS_NAME));
    }

    @SuppressWarnings("unused")
    interface TestCases {
        static Map<TypeFormatContext, String> clazz(String ignored) {
            return Map.of(
                    TypeFormatContext.DEFAULT, "java.lang.String",
                    TypeFormatContext.SIMPLE, "String",
                    TypeFormatContext.CLASS_NAME, "java.lang.String");
        }

        static Map<TypeFormatContext, String> nested(Map.Entry<?, ?> ignored) {
            return Map.of(
                    TypeFormatContext.DEFAULT, "java.util.Map$Entry<?, ?>",
                    TypeFormatContext.SIMPLE, "Entry<?, ?>",
                    TypeFormatContext.CLASS_NAME, "java.util.Map$Entry");
        }

        static Map<TypeFormatContext, String> array(Number[] ignored) {
            return Map.of(
                    TypeFormatContext.DEFAULT, "java.lang.Number[]",
                    TypeFormatContext.SIMPLE, "Number[]",
                    TypeFormatContext.CLASS_NAME, "[Ljava.lang.Number;");
        }

        static <T> Map<TypeFormatContext, String> variable(T ignored) {
            return Map.of(
                    TypeFormatContext.DEFAULT, "T",
                    TypeFormatContext.SIMPLE, "T",
                    TypeFormatContext.CLASS_NAME, "java.lang.Object");
        }

        static <T extends Number> Map<TypeFormatContext, String> boundedVariable(T ignored) {
            return Map.of(
                    TypeFormatContext.DEFAULT, "T extends java.lang.Number",
                    TypeFormatContext.SIMPLE, "T extends Number",
                    TypeFormatContext.CLASS_NAME, "java.lang.Number");
        }

        static <T extends Enum<T>> Map<TypeFormatContext, String> recursivelyBoundedVariable(
                T ignored) {
            return Map.of(
                    TypeFormatContext.DEFAULT, "T extends java.lang.Enum<T>",
                    TypeFormatContext.SIMPLE, "T extends Enum<T>",
                    TypeFormatContext.CLASS_NAME, "java.lang.Enum");
        }

        static Map<TypeFormatContext, String> parameterized(List<String> ignored) {
            return Map.of(
                    TypeFormatContext.DEFAULT, "java.util.List<java.lang.String>",
                    TypeFormatContext.SIMPLE, "List<String>",
                    TypeFormatContext.CLASS_NAME, "java.util.List");
        }

        static <K> Map<TypeFormatContext, String> parameterized(Map<K, Number> ignored) {
            return Map.of(
                    TypeFormatContext.DEFAULT, "java.util.Map<K, java.lang.Number>",
                    TypeFormatContext.SIMPLE, "Map<K, Number>",
                    TypeFormatContext.CLASS_NAME, "java.util.Map");
        }

        static <K> Map<TypeFormatContext, String> outerParameterized(
                TypeFormatContextTest<String>.OwnerTypeTestCase ignored) {
            return Map.of(
                    TypeFormatContext.DEFAULT,
                            "org.mozilla.javascript.tests.type_info.TypeFormatContextTest$OwnerTypeTestCase",
                    TypeFormatContext.SIMPLE, "OwnerTypeTestCase",
                    TypeFormatContext.CLASS_NAME,
                            "org.mozilla.javascript.tests.type_info.TypeFormatContextTest$OwnerTypeTestCase");
        }
    }

    class OwnerTypeTestCase {
        T t;
    }
}
