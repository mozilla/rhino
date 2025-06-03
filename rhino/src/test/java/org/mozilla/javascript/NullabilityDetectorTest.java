package org.mozilla.javascript;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.Test;
import org.mozilla.javascript.lc.type.TypeInfoFactory;

public class NullabilityDetectorTest {
    @Test
    public void testNullableDetectorForMethodWithoutArgs() {
        MemberBox memberBox =
                new MemberBox(getTestClassMethod("function1"), TypeInfoFactory.GLOBAL);
        assertThat(memberBox.argNullability, is(new boolean[] {}));
    }

    @Test
    public void testNullableDetectorForMethodWithOneArg() {
        MemberBox memberBox =
                new MemberBox(getTestClassMethod("function2"), TypeInfoFactory.GLOBAL);
        assertThat(memberBox.argNullability, is(new boolean[] {true}));
    }

    @Test
    public void testNullableDetectorForMethodWithSeveralArgs() {
        MemberBox memberBox =
                new MemberBox(getTestClassMethod("function3"), TypeInfoFactory.GLOBAL);
        assertThat(memberBox.argNullability, is(new boolean[] {true, true, true, true}));
    }

    @Test
    public void testNullableDetectorForConstructorWithoutArgs() {
        MemberBox memberBox = new MemberBox(getTestClassConstructor(0), TypeInfoFactory.GLOBAL);
        assertThat(memberBox.argNullability, is(new boolean[] {}));
    }

    @Test
    public void testNullableDetectorForConstructorWithOneArg() {
        MemberBox memberBox = new MemberBox(getTestClassConstructor(1), TypeInfoFactory.GLOBAL);
        assertThat(memberBox.argNullability, is(new boolean[] {true}));
    }

    @Test
    public void testNullableDetectorForConstructorWithSeveralArgs() {
        MemberBox memberBox = new MemberBox(getTestClassConstructor(4), TypeInfoFactory.GLOBAL);
        assertThat(memberBox.argNullability, is(new boolean[] {true, false, true, false}));
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
