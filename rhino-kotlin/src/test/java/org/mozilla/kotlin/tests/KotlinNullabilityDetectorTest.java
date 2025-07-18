package org.mozilla.kotlin.tests;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mozilla.javascript.NullabilityDetector;
import org.mozilla.kotlin.KotlinNullabilityDetector;

public class KotlinNullabilityDetectorTest {
    private final KotlinNullabilityDetector detector = new KotlinNullabilityDetector();

    @Test
    public void testKotlinFunction() {
        var nullability =
                detector.getParameterNullability(findMethod(KotlinClass.class, "function", 3));

        assertNullabilityMatch(nullability, true, false, true);
    }

    @Test
    public void testKotlinConstructor() {
        var nullability = detector.getParameterNullability(findConstructor(KotlinClass.class, 2));

        assertNullabilityMatch(nullability, false, true);
    }

    @Test
    public void testJavaFunction() {
        var nullability =
                detector.getParameterNullability(findMethod(JavaClass.class, "function", 2));

        assertNullabilityMatch(nullability, false, false);
    }

    @Test
    public void testJavaConstructor() {
        var nullability = detector.getParameterNullability(findConstructor(JavaClass.class, 2));

        assertNullabilityMatch(nullability, false, false);
    }

    @Test
    public void testKotlinOverloadedFunction() {
        List<Method> overloadedMethods =
                findMethods(KotlinClassWithOverloadedFunction.class, "function");

        assertThat(overloadedMethods.size(), is(2));
        // Since we cannot distinguish overloads with same number of params, we have to fallback to
        // no-op
        boolean[] expectedNullability = new boolean[] {false, false, false};
        for (var overloadedMethod : overloadedMethods) {
            assertNullabilityMatch(
                    detector.getParameterNullability(overloadedMethod), expectedNullability);
        }
    }

    private static void assertNullabilityMatch(
            NullabilityDetector.NullabilityAccessor nullabilityAccessor, boolean... expected) {
        var actual = new boolean[expected.length];
        for (int i = 0; i < actual.length; i++) {
            actual[i] = nullabilityAccessor.isNullable(i);
        }

        Assertions.assertArrayEquals(expected, actual);
    }

    private Method findMethod(Class<?> clazz, String methodName, int paramCount) {
        return Arrays.stream(clazz.getMethods())
                .filter(
                        method ->
                                method.getName().equals(methodName)
                                        && method.getParameterTypes().length == paramCount)
                .findFirst()
                .orElse(null);
    }

    private Constructor<?> findConstructor(Class<?> clazz, int paramCount) {
        return Arrays.stream(clazz.getConstructors())
                .filter(constructor -> constructor.getParameterTypes().length == paramCount)
                .findFirst()
                .orElse(null);
    }

    private List<Method> findMethods(Class<?> clazz, String methodName) {
        return Arrays.stream(clazz.getMethods())
                .filter(method -> method.getName().equals(methodName))
                .collect(Collectors.toList());
    }
}
