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
        MemberBox memberBox = MemberBox.of(getTestClassMethod("function1"), TypeInfoFactory.GLOBAL);
        // assertThat(memberBox.argNullability, is(new boolean[] {}));
    }

    @Test
    public void testNullableDetectorForMethodWithOneArg() {
        MemberBox memberBox = MemberBox.of(getTestClassMethod("function2"), TypeInfoFactory.GLOBAL);
        assertThat(memberBox.isArgNullable(0), is(true));
    }

    @Test
    public void testNullableDetectorForMethodWithSeveralArgs() {
        MemberBox memberBox = MemberBox.of(getTestClassMethod("function3"), TypeInfoFactory.GLOBAL);
        assertThat(memberBox.isArgNullable(0), is(true));
        assertThat(memberBox.isArgNullable(1), is(true));
        assertThat(memberBox.isArgNullable(2), is(true));
        assertThat(memberBox.isArgNullable(3), is(true));
    }

    @Test
    public void testNullableDetectorForConstructorWithoutArgs() {
        MemberBox memberBox = MemberBox.of(getTestClassConstructor(0), TypeInfoFactory.GLOBAL);
        //  assertThat(memberBox.argNullability, is(new boolean[] {}));
    }

    @Test
    public void testNullableDetectorForConstructorWithOneArg() {
        MemberBox memberBox = MemberBox.of(getTestClassConstructor(1), TypeInfoFactory.GLOBAL);
        MemberBox.of(getTestClassMethod("function3"), TypeInfoFactory.GLOBAL);
        assertThat(memberBox.isArgNullable(0), is(true));
    }

    @Test
    public void testNullableDetectorForConstructorWithSeveralArgs() {
        MemberBox memberBox = MemberBox.of(getTestClassConstructor(4), TypeInfoFactory.GLOBAL);
        MemberBox.of(getTestClassMethod("function3"), TypeInfoFactory.GLOBAL);
        assertThat(memberBox.isArgNullable(0), is(true));
        assertThat(memberBox.isArgNullable(1), is(false));
        assertThat(memberBox.isArgNullable(2), is(true));
        assertThat(memberBox.isArgNullable(3), is(false));
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
