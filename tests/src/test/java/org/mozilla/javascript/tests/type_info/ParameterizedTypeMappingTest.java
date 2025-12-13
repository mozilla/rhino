package org.mozilla.javascript.tests.type_info;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.lc.type.*;

/**
 * @author ZZZank
 */
@SuppressWarnings("unused") // 'unused' members are scanned via reflection
public class ParameterizedTypeMappingTest<T> {

    @Test
    public void test() throws Exception {
        var testObject = new TestCases();
        for (var method : TestCases.class.getDeclaredMethods()) {
            var modifiers = method.getModifiers();
            if (!Modifier.isPublic(modifiers)
                    || Modifier.isStatic(modifiers)
                    || !method.getName().startsWith("test$")
                    || method.getReturnType() != Map.class) {
                continue;
            }

            // public Map<String, String> test$xxx(TYPE xxx)
            var rawType = method.getGenericParameterTypes()[0];
            var type = (ParameterizedTypeInfo) TypeInfoFactory.GLOBAL.create(rawType);

            var formatted = new HashMap<String, String>();
            var mapping = type.extractConsolidationMapping(TypeInfoFactory.GLOBAL);
            for (var entry : mapping.entrySet()) {
                formatted.put(entry.getKey().toString(), entry.getValue().toString());
            }

            var expected = (Map<?, ?>) method.invoke(testObject, new Object[] {null});

            Assertions.assertEquals(expected, formatted);
        }
    }

    public class TestCases {
        T useTheTOnceToSuppressIDEAWarnings;

        public Map<String, String> test$single(List<String> ignored) {
            return Map.of("E", "java.lang.String");
        }

        public Map<String, String> test$multi(Map<String, Number> ignored) {
            return Map.of("K", "java.lang.String", "V", "java.lang.Number");
        }

        public <KEY> Map<String, String> test$generic(Map<KEY, Number> ignored) {
            return Map.of("K", "KEY", "V", "java.lang.Number");
        }

        public <KEY extends CharSequence> Map<String, String> test$bounded(
                Map<KEY, Number> ignored) {
            return Map.of("K", "KEY extends java.lang.CharSequence", "V", "java.lang.Number");
        }

        public Map<String, String> test$ownerType(
                ParameterizedTypeMappingTest<String>.TestCases ignored) {
            return Map.of("T", "java.lang.String");
        }

        public Map<String, String> test$ownerType2(
                ParameterizedTypeMappingTest<String>.TestCase2<Number> ignored) {
            return Map.of("T", "java.lang.String", "T2", "java.lang.Number");
        }
    }

    public class TestCase2<T2> {
        T t;
        T2 t2;
    }
}
