package org.mozilla.javascript;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
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

    private static void assertNullabilityMatch(NullabilityDetector.NullabilityAccessor nullabilityAccessor, boolean... expected) {
        for (int i = 0; i < expected.length; i++) {
            Assertions.assertEquals(nullabilityAccessor.isNullable(i), expected[i]);
        }
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
