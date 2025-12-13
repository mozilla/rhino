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
    public void test() throws Exception {
        testImpl(TypeFormatContext.DEFAULT, "DEFAULT");
        testImpl(TypeFormatContext.SIMPLE, "SIMPLE");
        testImpl(TypeFormatContext.CLASS_NAME, "CLASS_NAME");
    }

    @Test
    public void noneType() {
        Assertions.assertEquals("?", TypeInfo.NONE.toString(TypeFormatContext.DEFAULT));
        Assertions.assertEquals("?", TypeInfo.NONE.toString(TypeFormatContext.SIMPLE));
        Assertions.assertEquals(
                "java.lang.Object", TypeInfo.NONE.toString(TypeFormatContext.CLASS_NAME));
    }

    private static void testImpl(TypeFormatContext context, String contextName) throws Exception {
        Iterable<Method> methods =
                Arrays.stream(TestCases.class.getDeclaredMethods())
                                .filter(method1 -> !method1.isSynthetic())
                                .filter(
                                        method1 -> {
                                            var modifier = method1.getModifiers();
                                            return Modifier.isPublic(modifier)
                                                    && Modifier.isStatic(modifier);
                                        })
                        ::iterator;
        for (var method : methods) {
            var rawType = method.getGenericParameterTypes()[0];
            var type = TypeInfoFactory.GLOBAL.create(rawType);

            @SuppressWarnings("unchecked")
            var expectedByContext =
                    (Map<TypeFormatContext, String>) method.invoke(null, type.createDefaultValue());
            var formatted = type.toString(context);
            Assertions.assertEquals(
                    expectedByContext.get(context),
                    formatted,
                    () ->
                            String.format(
                                    "Testing type '%s' on context '%s'\nExpected: %s\nActual: '%s'",
                                    rawType,
                                    contextName,
                                    expectedByContext.get(context),
                                    formatted));
        }
    }

    @SuppressWarnings("unused")
    interface TestCases {
        static Map<TypeFormatContext, String> clazz(String ignored) {
            return buildResult("java.lang.String", "String", "java.lang.String");
        }

        static Map<TypeFormatContext, String> primitive(char ignored) {
            return buildResult("char", "char", "char");
        }

        static Map<TypeFormatContext, String> nested(Map.Entry<?, ?> ignored) {
            return buildResult("java.util.Map$Entry<?, ?>", "Entry<?, ?>", "java.util.Map$Entry");
        }

        static Map<TypeFormatContext, String> array(Number[] ignored) {
            return buildResult("java.lang.Number[]", "Number[]", "[Ljava.lang.Number;");
        }

        static Map<TypeFormatContext, String> primitiveArray(char[] ignored) {
            return buildResult("char[]", "char[]", "[C");
        }

        static <T> Map<TypeFormatContext, String> variable(T ignored) {
            return buildResult("T", "T", "java.lang.Object");
        }

        static <T extends Number> Map<TypeFormatContext, String> boundedVariable(T ignored) {
            return buildResult(
                    "T extends java.lang.Number", "T extends Number", "java.lang.Number");
        }

        static <T extends Enum<T>> Map<TypeFormatContext, String> recursivelyBoundedVariable(
                T ignored) {
            return buildResult(
                    "T extends java.lang.Enum<T>", "T extends Enum<T>", "java.lang.Enum");
        }

        static Map<TypeFormatContext, String> parameterized(List<String> ignored) {
            return buildResult(
                    "java.util.List<java.lang.String>", "List<String>", "java.util.List");
        }

        static <K> Map<TypeFormatContext, String> parameterized(Map<K, Number> ignored) {
            return buildResult(
                    "java.util.Map<K, java.lang.Number>", "Map<K, Number>", "java.util.Map");
        }

        static <K> Map<TypeFormatContext, String> outerParameterized(
                TypeFormatContextTest<String>.OwnerTypeTestCase ignored) {
            return buildResult(
                    "org.mozilla.javascript.tests.type_info.TypeFormatContextTest$OwnerTypeTestCase",
                    "OwnerTypeTestCase",
                    "org.mozilla.javascript.tests.type_info.TypeFormatContextTest$OwnerTypeTestCase");
        }

        private static Map<TypeFormatContext, String> buildResult(
                String defaultContext, String simpleContext, String classNameContext) {
            return Map.of(
                    TypeFormatContext.DEFAULT, defaultContext,
                    TypeFormatContext.SIMPLE, simpleContext,
                    TypeFormatContext.CLASS_NAME, classNameContext);
        }
    }

    class OwnerTypeTestCase {
        @SuppressWarnings("unused")
        T t;
    }
}
