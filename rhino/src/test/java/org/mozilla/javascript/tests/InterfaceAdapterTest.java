/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Wrapper;

/*
 * This testcase tests the support of converting javascript functions to
 * functional interface adapters
 *
 */
public class InterfaceAdapterTest {

    public static String joinString(String a) {
        return a + "|";
    }

    private void testIt(String js, Object expected) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    final ScriptableObject scope = cx.initStandardObjects();
                    scope.put("list", scope, createList());
                    Object o = cx.evaluateString(scope, js, "testNativeFunction.js", 1, null);
                    if (o instanceof Wrapper) {
                        o = ((Wrapper) o).unwrap();
                    }
                    assertEquals(expected, o);

                    return null;
                });
    }

    private List<String> createList() {
        List<String> list = new ArrayList<>();
        list.add("foo");
        list.add("bar");
        return list;
    }

    @Test
    public void nativeFunctionAsConsumer() {
        String js = "var ret = '';\n" + "list.forEach(function(elem) {  ret += elem });\n" + "ret";

        testIt(js, "foobar");
    }

    @Test
    public void arrowFunctionAsConsumer() {
        String js = "var ret = '';\n" + "list.forEach(elem => ret += elem);\n" + "ret";

        testIt(js, "foobar");
    }

    @Test
    public void boundFunctionAsConsumer() {
        String js =
                "var ret = '';\n"
                        + "list.forEach(((c, elem) => ret += elem + c).bind(null, ','));\n"
                        + "ret";

        testIt(js, "foo,bar,");
    }

    @Test
    public void javaMethodAsConsumer() {
        String js =
                "var ret = '';\n"
                        + "list.stream().map(org.mozilla.javascript.tests.InterfaceAdapterTest.joinString)\n"
                        + ".forEach(elem => ret += elem);\n"
                        + "ret";

        testIt(js, "foo|bar|");
    }

    @Test
    public void arrowFunctionAsComparator() {
        String js = "list";
        testIt(js, Arrays.asList("foo", "bar"));

        js = "list.sort((a,b) => a > b ? 1:-1)\n" + "list";
        testIt(js, Arrays.asList("bar", "foo"));
    }

    @Test
    public void nativeFunctionAsComparator() {
        String js = "list";
        testIt(js, Arrays.asList("foo", "bar"));

        js = "list.sort(function(a,b) { return a > b ? 1:-1 })\n" + "list";
        testIt(js, Arrays.asList("bar", "foo"));
    }

    public interface EmptyInterface {}

    public static void receiveEmptyInterface(EmptyInterface i) {}

    @Test
    public void functionAsEmptyInterface() {
        String js =
                "org.mozilla.javascript.tests.InterfaceAdapterTest.receiveEmptyInterface(a => a);\n"
                        + "list";
        assertThrows(EvaluatorException.class, () -> testIt(js, Arrays.asList("foo", "bar")));
    }

    public interface OneMethodInterface {
        String a();
    }

    public static String receiveOneMethodInterface(OneMethodInterface i) {
        return i.a();
    }

    @Test
    public void functionAsOneMethodInterface() {
        String js =
                "org.mozilla.javascript.tests.InterfaceAdapterTest.receiveOneMethodInterface(() => 'ok');";
        testIt(js, "ok");
    }

    public interface TwoMethodsInterface {
        void a();

        void b();
    }

    public static void receiveTwoMethodsInterface(TwoMethodsInterface i) {}

    @Test
    public void functionAsTwoMethodsInterface() {
        String js =
                "org.mozilla.javascript.tests.InterfaceAdapterTest.receiveTwoMethodsInterface(a => a);\n"
                        + "list";
        assertThrows(EvaluatorException.class, () -> testIt(js, Arrays.asList("foo", "bar")));
    }

    public interface TwoMethodsWithExtendsInterface extends OneMethodInterface {
        void b();
    }

    public static void receiveTwoMethodsWithExtendsInterface(TwoMethodsWithExtendsInterface i) {}

    @Test
    public void functionAsTwoMethodsWithExtendsInterface() {
        String js =
                "org.mozilla.javascript.tests.InterfaceAdapterTest.receiveTwoMethodsWithExtendsInterface(a => a);\n"
                        + "list";
        assertThrows(EvaluatorException.class, () -> testIt(js, Arrays.asList("foo", "bar")));
    }

    public interface OneDefaultMethodInterface {
        default String a() {
            return "ng";
        }
    }

    public static String receiveOneDefaultMethodInterface(OneDefaultMethodInterface i) {
        return i.a();
    }

    @Test
    public void functionAsOneDefaultMethodInterface() {
        String js =
                "org.mozilla.javascript.tests.InterfaceAdapterTest.receiveOneDefaultMethodInterface(() => 'ok');";
        testIt(js, "ok");
    }

    public interface TwoDefaultMethodsInterface {
        default void a() {}

        default void b() {}
    }

    public static void receiveTwoDefaultMethodsInterface(TwoDefaultMethodsInterface i) {}

    @Test
    public void functionAsTwoDefaultMethodsInterface() {
        String js =
                "org.mozilla.javascript.tests.InterfaceAdapterTest.receiveTwoDefaultMethodsInterface(a => a);\n"
                        + "list";
        assertThrows(EvaluatorException.class, () -> testIt(js, Arrays.asList("foo", "bar")));
    }

    public interface TwoSameNameDefaultMethodsInterface {
        default String a(int i) {
            return "ng";
        }

        default String a(String s) {
            return "ng";
        }
    }

    public static String receiveTwoSameNameDefaultMethodsInterface(
            TwoSameNameDefaultMethodsInterface i) {
        return i.a(1);
    }

    @Test
    public void functionAsTwoSameNameDefaultMethodsInterface() {
        String js =
                "org.mozilla.javascript.tests.InterfaceAdapterTest.receiveTwoSameNameDefaultMethodsInterface(i => 'ok,' + i);";
        testIt(js, "ok,1");
    }

    public interface OneAbstructOneDefaultSameNameMethodInterface {
        String a(int i);

        default String a(String s) {
            return "ng";
        }
    }

    public static String receiveOneAbstructOneDefaultSameNameMethodInterface(
            OneAbstructOneDefaultSameNameMethodInterface i) {
        return i.a(2);
    }

    @Test
    public void functionAsOneAbstructOneDefaultSameNameMethodInterface() {
        String js =
                "org.mozilla.javascript.tests.InterfaceAdapterTest.receiveOneAbstructOneDefaultSameNameMethodInterface(i => 'ok,' + i);";
        testIt(js, "ok,2");
    }

    public interface AddDefaultMethodWithExtendsInterface extends OneMethodInterface {
        default void b() {}
    }

    public static String receiveAddDefaultMethodWithExtendsInterface(
            AddDefaultMethodWithExtendsInterface i) {
        return i.a();
    }

    @Test
    public void functionAsAddDefaultMethodWithExtendsInterface() {
        String js =
                "org.mozilla.javascript.tests.InterfaceAdapterTest.receiveAddDefaultMethodWithExtendsInterface(() => 'ok');";
        testIt(js, "ok");
    }
}
