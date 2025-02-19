package org.mozilla.kotlin.tests;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.mozilla.kotlin.KotlinNullabilityDetector;

public class KotlinNullabilityDetectorTest {
    private final KotlinNullabilityDetector detector = new KotlinNullabilityDetector();

    @Test
    public void testKotlinFunction() {
        boolean[] nullability =
                detector.getParameterNullability(findMethod(KotlinClass.class, "function", 3));

        boolean[] expectedNullability = new boolean[] {true, false, true};
        assertThat(nullability, is(expectedNullability));
    }

    @Test
    public void testKotlinConstructor() {
        boolean[] nullability =
                detector.getParameterNullability(findConstructor(KotlinClass.class, 2));

        boolean[] expectedNullability = new boolean[] {false, true};
        assertThat(nullability, is(expectedNullability));
    }

    @Test
    public void testJavaFunction() {
        boolean[] nullability =
                detector.getParameterNullability(findMethod(JavaClass.class, "function", 2));

        boolean[] expectedNullability = new boolean[] {false, false};
        assertThat(nullability, is(expectedNullability));
    }

    @Test
    public void testJavaConstructor() {
        boolean[] nullability =
                detector.getParameterNullability(findConstructor(JavaClass.class, 2));

        boolean[] expectedNullability = new boolean[] {false, false};
        assertThat(nullability, is(expectedNullability));
    }

    @Test
    public void testKotlinOverloadedFunction() {
        List<Method> overloadedMethods =
                findMethods(KotlinClassWithOverloadedFunction.class, "function");

        assertThat(overloadedMethods.size(), is(2));
        // Since we cannot distinguish overloads with same number of params, we have to fallback to
        // no-op
        boolean[] expectedNullability = new boolean[] {false, false, false};
        overloadedMethods.forEach(
                method ->
                        assertThat(
                                detector.getParameterNullability(method), is(expectedNullability)));
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
