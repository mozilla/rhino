package org.mozilla.javascript.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.testutils.Utils;

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
            captured.add("N." + sN.length);
        }
    }

    private static void expect(List<String> expected, String... lines) {
        Utils.runWithAllModes(
                cx -> {
                    final var methodDummy = new MethodDummy();
                    final var scope = initContext(cx, methodDummy);

                    cx.evaluateString(
                            scope, String.join("\n", lines), "NativeJavaMethodTest.js", 0, null);

                    Assertions.assertEquals(expected, methodDummy.captured);
                    return null;
                });
    }

    private static ScriptableObject initContext(Context cx, MethodDummy methodDummy) {
        cx.setLanguageVersion(Context.VERSION_ES6);
        final var scope = cx.initStandardObjects();
        ScriptableObject.putProperty(scope, "d", Context.javaToJS(methodDummy, scope));
        return scope;
    }

    private static void expectException(
            Class<? extends Exception> exception, String msgPrefix, String... lines) {
        Utils.runWithAllModes(
                cx -> {
                    final var methodDummy = new MethodDummy();
                    final var scope = initContext(cx, methodDummy);

                    final var ex =
                            Assertions.assertThrows(
                                    exception,
                                    () ->
                                            cx.evaluateString(
                                                    scope,
                                                    String.join("\n", lines),
                                                    "NativeJavaMethodTest.js",
                                                    0,
                                                    null));

                    Assertions.assertTrue(
                            ex.getMessage().startsWith(msgPrefix),
                            String.format(
                                    "'%s' does not start with '%s'", ex.getMessage(), msgPrefix));

                    return null;
                });
    }

    @Test
    void fixedArg() {
        expect(Arrays.asList("1", "2"), "d.f1('xxx');", "d.f2('x', 'y');");
    }

    @Test
    void overload() {
        expect(
                Arrays.asList("1", "1.2", "1.1", "1.2"),
                "d.f1('xxx');",
                "d.f1('x', 'y');",
                "d.f1('x', 3);",
                "d.f1('x', '3');");
    }

    @Test
    void varArg() {
        expect(
                Arrays.asList("N.0", "N.3", "N.2"),
                "d.fN('x', 'y');",
                "d.fN('x', 'y', 'extra1', 'extra2', 'extra3');",
                "d.fN('x', 'y', 'extra1', 'extra2');");
    }

    @Test
    void argsTooShortForFixedArg() {
        expectException(EvaluatorException.class, "Can't find method", "d.f2('x');");
    }

    @Test
    void argsTooShortForVarArg() {
        expectException(EvaluatorException.class, "Can't find method", "d.fN('x');");
    }

    @Test
    void varArgAndFixedArgPreference() {
        expect(
                Arrays.asList("2", "2.N", "2"),
                "d.f2('x', 'y');",
                "d.f2('x', 'y', 'overflow');",
                "d.f2('y', 'x');");
    }

    @Test
    void cursed() {
        expect(
                Arrays.asList("2.N", "2"),
                "const dum = new Packages.org.mozilla.javascript.tests.NativeJavaMethodTest$MethodDummy();\n",
                "dum.f2('x', 'y', 'overflow');",
                "dum.f2('x', 'y');",
                "d.captured.addAll(dum.captured)");
    }
}
