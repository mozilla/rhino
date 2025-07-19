package org.mozilla.javascript.lc.java;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mozilla.javascript.NullabilityDetector.NullabilityAccessor;
import org.mozilla.javascript.lc.type.TypeInfoFactory;

public class NullabilityDetectorTest {
    @Test
    public void testNullableDetectorForMethodWithoutArgs() {
        MemberBox memberBox =
                new MemberBox(getTestClassMethod("function1"), TypeInfoFactory.GLOBAL);
        assertNullabilityMatch(memberBox.getArgNullability());
    }

    @Test
    public void testNullableDetectorForMethodWithOneArg() {
        MemberBox memberBox =
                new MemberBox(getTestClassMethod("function2"), TypeInfoFactory.GLOBAL);
        assertNullabilityMatch(memberBox.getArgNullability(), true);
    }

    @Test
    public void testNullableDetectorForMethodWithSeveralArgs() {
        MemberBox memberBox =
                new MemberBox(getTestClassMethod("function3"), TypeInfoFactory.GLOBAL);
        assertNullabilityMatch(memberBox.getArgNullability(), true, true, true, true);
    }

    @Test
    public void testNullableDetectorForConstructorWithoutArgs() {
        MemberBox memberBox = new MemberBox(getTestClassConstructor(0), TypeInfoFactory.GLOBAL);
        assertNullabilityMatch(memberBox.getArgNullability());
    }

    @Test
    public void testNullableDetectorForConstructorWithOneArg() {
        MemberBox memberBox = new MemberBox(getTestClassConstructor(1), TypeInfoFactory.GLOBAL);
        assertNullabilityMatch(memberBox.getArgNullability(), true);
    }

    @Test
    public void testNullableDetectorForConstructorWithSeveralArgs() {
        MemberBox memberBox = new MemberBox(getTestClassConstructor(4), TypeInfoFactory.GLOBAL);
        assertNullabilityMatch(memberBox.getArgNullability(), true, false, true, false);
    }

    @Test
    public void testNullabilityCompressor() {
        for (var nullabilityPolicy :
                List.<NullabilityAccessor>of(i -> i % 2 != 0, i -> false, i -> true)) {
            for (var paramCount : new int[] {0, 1, 2, 5, 12, 31, 32, 33, 34, 56, 78}) {
                var toTest = new boolean[paramCount];
                for (var i = 0; i < toTest.length; i++) {
                    toTest[i] = nullabilityPolicy.isNullable(i);
                }
                var compressed = NullabilityAccessor.compress(toTest);

                assertNullabilityMatch(compressed, toTest);
                for (var invalidInputs :
                        new int[] {-2, -1, paramCount, paramCount + 1, paramCount * 2}) {
                    try {
                        compressed.isNullable(invalidInputs);
                        // no exception -> valid
                    } catch (IndexOutOfBoundsException ignored) {
                        // IndexOutOfBounds -> valid
                    } catch (Throwable e) {
                        // exceptions not listed in javadoc -> invalid
                        Assertions.fail(
                                "NullabilityAccessor threw an exception that is not IndexOutOfBoundsException",
                                e);
                    }
                }
            }
        }
    }

    private static void assertNullabilityMatch(
            NullabilityAccessor nullabilityAccessor, boolean... expected) {
        var actual = new boolean[expected.length];
        for (int i = 0; i < actual.length; i++) {
            actual[i] = nullabilityAccessor.isNullable(i);
        }

        Assertions.assertArrayEquals(expected, actual);
    }

    private Method getTestClassMethod(String methodName) {
        return Arrays.stream(NullabilityDetectorTestClass.class.getMethods())
                .filter(method -> method.getName().equals(methodName))
                .findFirst()
                .orElse(null);
    }

    private Constructor<?> getTestClassConstructor(int paramCount) {
        return Arrays.stream(NullabilityDetectorTestClass.class.getConstructors())
                .filter(constructor -> constructor.getParameterTypes().length == paramCount)
                .findFirst()
                .orElse(null);
    }
}
