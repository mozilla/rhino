package org.mozilla.javascript;

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
        assertNullabilityMatch(memberBox.argNullability);
    }

    @Test
    public void testNullableDetectorForMethodWithOneArg() {
        MemberBox memberBox =
                new MemberBox(getTestClassMethod("function2"), TypeInfoFactory.GLOBAL);
        assertNullabilityMatch(memberBox.argNullability, true);
    }

    @Test
    public void testNullableDetectorForMethodWithSeveralArgs() {
        MemberBox memberBox =
                new MemberBox(getTestClassMethod("function3"), TypeInfoFactory.GLOBAL);
        assertNullabilityMatch(memberBox.argNullability, true, true, true, true);
    }

    @Test
    public void testNullableDetectorForConstructorWithoutArgs() {
        MemberBox memberBox = new MemberBox(getTestClassConstructor(0), TypeInfoFactory.GLOBAL);
        assertNullabilityMatch(memberBox.argNullability);
    }

    @Test
    public void testNullableDetectorForConstructorWithOneArg() {
        MemberBox memberBox = new MemberBox(getTestClassConstructor(1), TypeInfoFactory.GLOBAL);
        assertNullabilityMatch(memberBox.argNullability, true);
    }

    @Test
    public void testNullableDetectorForConstructorWithSeveralArgs() {
        MemberBox memberBox = new MemberBox(getTestClassConstructor(4), TypeInfoFactory.GLOBAL);
        assertNullabilityMatch(memberBox.argNullability, true, false, true, false);
    }

    @Test
    public void testNullabilityCompressor() {
        for (var nullabilityPolicy :
                List.<NullabilityAccessor>of(i -> i % 2 != 0, i -> false, i -> true)) {
            for (var paramCount : new int[] {1, 2, 5, 12, 31, 32, 33, 34, 56, 78}) {
                var toTest = new boolean[paramCount];
                for (var i = 0; i < toTest.length; i++) {
                    toTest[i] = nullabilityPolicy.isNullable(i);
                }
                assertNullabilityMatch(NullabilityAccessor.compress(toTest), toTest);
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
