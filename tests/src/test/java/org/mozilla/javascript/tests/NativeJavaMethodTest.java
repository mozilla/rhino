package org.mozilla.javascript.tests;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author ZZZank
 */
public class NativeJavaMethodTest {

    public static class MethodDummy {
        public final List<String> captured = new ArrayList<>();

        public void f1(String s) {
            captured.add("1");
        }

        public void f1(String s, int forOverload) {
            captured.add("1.1");
        }

        public void f1(String s, String forOverload) {
            captured.add("1.2");
        }

        public void f2(String s1, String s2) {
            captured.add("2");
        }

        public void f2(String s1, String s2, String... sN) {
            captured.add("2.N");
        }

        public void fN(String s1, String s2, String... sN) {
            captured.add("N");
        }
    }

    private static final String INIT = "const d = new Packages.org.mozilla.javascript.tests.NativeJavaMethodTest$MethodDummy();\n";

    @Test
    void fixedArg() {
        Utils.assertWithAllModes_ES6(Arrays.asList("1", "2"), INIT
            + "d.f1('xxx');"
            + "d.f2('x', 'y');"
            + "d.captured");
    }

    @Test
    void overload() {
        Utils.assertWithAllModes_ES6(Arrays.asList("1", "1.2", "1.1", "1.2"), INIT
            + "d.f1('xxx');"
            + "d.f1('x', 'y');"
            + "d.f1('x', 3);"
            + "d.f1('x', '3');"
            + "d.captured");
    }

    @Test
    void varArg() {
        Utils.assertWithAllModes_ES6(Arrays.asList("N"), INIT
            + "d.fN('x', 'y', 'overflow');"
            + "d.captured");
    }

    @Test
    void argsTooShortForVarArg() {
        Utils.assertEvaluatorExceptionES6("Can't find method", INIT
            + "d.fN('x');"
            + "d.captured");
    }

    @Test
    void varArgAndFixedArgPreference() {
        // TODO: the second f2() should use the fixed arg variant
        Utils.assertWithAllModes_ES6(Arrays.asList("2.N", "2"), INIT
            + "d.f2('x', 'y', 'overflow');"
            + "d.f2('x', 'y');"
            + "d.captured");
    }
}
