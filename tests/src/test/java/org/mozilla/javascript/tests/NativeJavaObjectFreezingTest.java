/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class NativeJavaObjectFreezingTest {
    // NativeJavaObject

    @Test
    public void cannotAddNewPropertiesToNativeJavaObjects() {
        runTest(
                (cx, scope) -> {
                    TestBean bean = new TestBean("abc");
                    NativeJavaObject object = new NativeJavaObject(scope, bean, TestBean.class);

                    EvaluatorException error =
                            assertThrows(
                                    EvaluatorException.class, () -> object.put("age", object, 40));
                    assertTrue(
                            error.toString()
                                    .contains(
                                            "has no public instance field or method named \"age\""));
                });
    }

    @Test
    public void cannotAddNewPropertiesToNativeJavaObjectsViaJs() {
        runTest(
                (cx, scope) -> {
                    String source =
                            "var o = new Packages."
                                    + TestBean.class.getName()
                                    + "('abc');\n"
                                    + "o.age = 40;";
                    EvaluatorException error =
                            assertThrows(
                                    EvaluatorException.class,
                                    () -> cx.evaluateString(scope, source, "source", 1, null));
                    assertTrue(
                            error.toString()
                                    .contains(
                                            "has no public instance field or method named \"age\""));
                });
    }

    @Test
    public void cannotDeletePropertiesOfNativeJavaObjects() {
        runTest(
                (cx, scope) -> {
                    TestBean bean = new TestBean("abc");
                    NativeJavaObject object = new NativeJavaObject(scope, bean, TestBean.class);
                    object.delete("name");

                    Object propertyValue = object.get("name", object);
                    assertTrue(propertyValue instanceof NativeJavaObject);
                    assertEquals(
                            "Property still exists",
                            "abc",
                            ((NativeJavaObject) propertyValue).unwrap());
                });
    }

    @Test
    public void cannotDeletePropertiesOfNativeJavaObjectsViaJs() {
        runTest(
                (cx, scope) -> {
                    String source =
                            "var o = new Packages."
                                    + TestBean.class.getName()
                                    + "('abc');\n"
                                    + "delete o.name;\n"
                                    + "o.name";
                    Object result = cx.evaluateString(scope, source, "source", 1, null);
                    assertTrue(result instanceof NativeJavaObject);
                    assertEquals(
                            "Property still exists", "abc", ((NativeJavaObject) result).unwrap());
                });
    }

    @Test
    public void nativeJavaObjectCanBeFrozen() {
        runTest(
                (cx, scope) -> {
                    TestBean bean = new TestBean("abc");
                    NativeJavaObject object = new NativeJavaObject(scope, bean, TestBean.class);
                    object.freezeObject();
                    object.put("name", object, "def");

                    Object propertyValue = object.get("name", object);
                    assertTrue(propertyValue instanceof NativeJavaObject);
                    assertEquals(
                            "Property was not changed",
                            "abc",
                            ((NativeJavaObject) propertyValue).unwrap());
                    assertEquals("Wrapped object's field was not changed", "abc", bean.name);
                });
    }

    @Test
    public void nativeJavaObjectCanBeFrozenViaJsNonStrictMode() {
        runTest(
                (cx, scope) -> {
                    String source =
                            "var o = new Packages."
                                    + TestBean.class.getName()
                                    + "('abc');\n"
                                    + "Object.freeze(o);\n"
                                    + "o.name = 'def';\n"
                                    + "o.name";
                    Object result = cx.evaluateString(scope, source, "source", 1, null);
                    assertTrue(result instanceof NativeJavaObject);
                    assertEquals(
                            "Property was not changed",
                            "abc",
                            ((NativeJavaObject) result).unwrap());
                });
    }

    @Test
    public void nativeJavaObjectCanBeFrozenViaJsStrictMode() {
        runTest(
                (cx, scope) -> {
                    String source =
                            "'use strict';\n"
                                    + "var o = new Packages."
                                    + TestBean.class.getName()
                                    + "('abc');\n"
                                    + "Object.freeze(o);\n"
                                    + "o.name = 'def';";
                    EcmaError error =
                            assertThrows(
                                    EcmaError.class,
                                    () -> cx.evaluateString(scope, source, "source", 1, null));
                    assertTrue(
                            "error is: " + error,
                            error.toString()
                                    .contains(
                                            "ReferenceError: Assignment to \"name\" on frozen object in strict mode"));
                });
    }

    @Test
    public void cannotUseDefinePropertyOnNativeJavaObject() {
        runTest(
                (cx, scope) -> {
                    String source =
                            "var o = new Packages."
                                    + TestBean.class.getName()
                                    + "('abc');\n"
                                    + "Object.defineProperty(o, 'name', {value: 'def'});\n";
                    EcmaError error =
                            assertThrows(
                                    EcmaError.class,
                                    () -> cx.evaluateString(scope, source, "source", 1, null));
                    assertTrue(error.toString().contains("Expected argument of type object"));
                });
    }

    // Subclasses - NativeJavaClass

    @Test
    public void canFreezeNativeJavaClassViaJsNonStrictMode() {
        runTest(
                (cx, scope) -> {
                    String source =
                            "var c = Packages."
                                    + TestBean.class.getName()
                                    + ";\n"
                                    + "Object.freeze(c);\n"
                                    + "c.aStatic = 'def';\n"
                                    + "c.aStatic";
                    Object result = cx.evaluateString(scope, source, "source", 1, null);
                    assertTrue(result instanceof NativeJavaObject);
                    assertEquals(
                            "Property was not changed",
                            "abc",
                            ((NativeJavaObject) result).unwrap());
                });
    }

    @Test
    public void canFreezeNativeJavaClassViaJsStrictMode() {
        runTest(
                (cx, scope) -> {
                    String source =
                            "'use strict';\n"
                                    + "var c = Packages."
                                    + TestBean.class.getName()
                                    + ";\n"
                                    + "Object.freeze(c);\n"
                                    + "c.aStatic = 'def';\n"
                                    + "c.aStatic";
                    EcmaError error =
                            assertThrows(
                                    EcmaError.class,
                                    () -> cx.evaluateString(scope, source, "source", 1, null));
                    assertTrue(
                            "error is: " + error,
                            error.toString()
                                    .contains(
                                            "ReferenceError: Assignment to \"aStatic\" on frozen object in strict mode"));
                });
    }

    // Subclasses - NativeJavaMap

    @Test
    public void canFreezeNativeJavaMapStringKeysViaJsNonStrictMode() {
        runTest(
                (cx, scope) -> {
                    String source =
                            "var m = new Packages.java.util.HashMap();\n"
                                    + "m.name = 'abc';\n"
                                    + "Object.freeze(m);\n"
                                    + "m.name = 'def';\n"
                                    + "m.name";
                    Object result = cx.evaluateString(scope, source, "source", 1, null);
                    assertTrue(result instanceof NativeJavaObject);
                    assertEquals(
                            "Property was not created",
                            "abc",
                            ((NativeJavaObject) result).unwrap());
                });
    }

    @Test
    public void canFreezeNativeJavaMapStringKeysViaJsStrictMode() {
        runTest(
                (cx, scope) -> {
                    String source =
                            "'use strict';\n"
                                    + "var m = new Packages.java.util.HashMap();\n"
                                    + "m.name = 'abc';\n"
                                    + "Object.freeze(m);\n"
                                    + "m.name = 'def';\n"
                                    + "m.name";
                    EcmaError error =
                            assertThrows(
                                    EcmaError.class,
                                    () -> cx.evaluateString(scope, source, "source", 1, null));
                    assertTrue(
                            "error is: " + error,
                            error.toString()
                                    .contains(
                                            "ReferenceError: Assignment to \"name\" on frozen object in strict mode"));
                });
    }

    @Test
    public void canFreezeNativeJavaMapIntegerKeysViaJsNonStrictMode() {
        runTest(
                (cx, scope) -> {
                    String source =
                            "var m = new Packages.java.util.HashMap();\n"
                                    + "m[42] = 'abc';\n"
                                    + "Object.freeze(m);\n"
                                    + "m[42] = 'def';\n"
                                    + "m[42]";
                    Object result = cx.evaluateString(scope, source, "source", 1, null);
                    assertTrue(result instanceof NativeJavaObject);
                    assertEquals(
                            "Property was not created",
                            "abc",
                            ((NativeJavaObject) result).unwrap());
                });
    }

    @Test
    public void canFreezeNativeJavaMapIntegerKeysViaJsStrictMode() {
        runTest(
                (cx, scope) -> {
                    String source =
                            "'use strict';\n"
                                    + "var m = new Packages.java.util.HashMap();\n"
                                    + "m[42] = 'abc';\n"
                                    + "Object.freeze(m);\n"
                                    + "m[42] = 'def';\n"
                                    + "m[42]";
                    EcmaError error =
                            assertThrows(
                                    EcmaError.class,
                                    () -> cx.evaluateString(scope, source, "source", 1, null));
                    assertTrue(
                            "error is: " + error,
                            error.toString()
                                    .contains(
                                            "ReferenceError: Assignment to \"42\" on frozen object in strict mode"));
                });
    }

    // Subclasses - NativeJavaList

    @Test
    public void canFreezeNativeJavaListViaJsNonStrictMode() {
        runTest(
                (cx, scope) -> {
                    String source =
                            "var l = new Packages.java.util.ArrayList();\n"
                                    + "l.add('abc');"
                                    + "Object.freeze(l);\n"
                                    + "l[0] = 'def';\n"
                                    + "l[0]";
                    Object result = cx.evaluateString(scope, source, "source", 1, null);
                    assertTrue(result instanceof NativeJavaObject);
                    assertEquals(
                            "Property was not changed",
                            "abc",
                            ((NativeJavaObject) result).unwrap());
                });
    }

    @Test
    public void canFreezeNativeJavaListViaJsStrictMode() {
        runTest(
                (cx, scope) -> {
                    String source =
                            "'use strict';\n"
                                    + "var l = new Packages.java.util.ArrayList();\n"
                                    + "l.add('abc');"
                                    + "Object.freeze(l);\n"
                                    + "l[0] = 'def';\n"
                                    + "l[0]";
                    EcmaError error =
                            assertThrows(
                                    EcmaError.class,
                                    () -> cx.evaluateString(scope, source, "source", 1, null));
                    assertTrue(
                            "error is: " + error,
                            error.toString()
                                    .contains(
                                            "ReferenceError: Assignment to \"0\" on frozen object in strict mode"));
                });
    }

    @Test
    public void canFreezeNativeJavaListLengthViaJsNonStrictMode() {
        runTest(
                (cx, scope) -> {
                    String source =
                            "var l = new Packages.java.util.ArrayList();\n"
                                    + "l.length = 1;"
                                    + "Object.freeze(l);\n"
                                    + "l.length = 2;\n"
                                    + "l.length";
                    Object result = cx.evaluateString(scope, source, "source", 1, null);
                    assertTrue(result instanceof Number);
                    assertEquals("Property was not changed", 1, ((Number) result).intValue());
                });
    }

    @Test
    public void canFreezeNativeJavaListLengthViaJsStrictMode() {
        runTest(
                (cx, scope) -> {
                    String source =
                            "'use strict';\n"
                                    + "var l = new Packages.java.util.ArrayList();\n"
                                    + "l.length = 1;"
                                    + "Object.freeze(l);\n"
                                    + "l.length = 2;\n"
                                    + "l.length";
                    EcmaError error =
                            assertThrows(
                                    EcmaError.class,
                                    () -> cx.evaluateString(scope, source, "source", 1, null));
                    assertTrue(
                            "error is: " + error,
                            error.toString()
                                    .contains(
                                            "ReferenceError: Assignment to \"length\" on frozen object in strict mode"));
                });
    }

    // Subclasses - NativeJavaArray

    @Test
    public void canFreezeNativeJavaArrayViaJsNonStrictMode() {
        runTest(
                (cx, scope) -> {
                    String source =
                            "var l = new Packages.java.util.ArrayList();\n"
                                    + "l.add('abc');\n"
                                    + "var a = l.toArray();\n"
                                    + "Object.freeze(a);\n"
                                    + "a[0] = 'def';\n"
                                    + "a[0]";
                    Object result = cx.evaluateString(scope, source, "source", 1, null);
                    assertTrue(result instanceof NativeJavaObject);
                    assertEquals(
                            "Property was not changed",
                            "abc",
                            ((NativeJavaObject) result).unwrap());
                });
    }

    @Test
    public void canFreezeNativeJavaArrayViaJsStrictMode() {
        runTest(
                (cx, scope) -> {
                    String source =
                            "'use strict';\n"
                                    + "var l = new Packages.java.util.ArrayList();\n"
                                    + "l.add('abc');\n"
                                    + "var a = l.toArray();\n"
                                    + "Object.freeze(a);\n"
                                    + "a[0] = 'def';\n"
                                    + "a[0]";
                    EcmaError error =
                            assertThrows(
                                    EcmaError.class,
                                    () -> cx.evaluateString(scope, source, "source", 1, null));
                    assertTrue(
                            "error is: " + error,
                            error.toString()
                                    .contains(
                                            "ReferenceError: Assignment to \"0\" on frozen object in strict mode"));
                });
    }

    // Trivial class to check property mutation

    public static class TestBean {
        public static String aStatic = "abc";
        public String name;

        public TestBean(String name) {
            this.name = name;
        }
    }

    // Factory and helper method

    private final ContextFactory contextFactoryWithMapAccess =
            new ContextFactory() {
                @Override
                protected boolean hasFeature(Context cx, int featureIndex) {
                    if (featureIndex == Context.FEATURE_ENABLE_JAVA_MAP_ACCESS) {
                        return true;
                    }
                    return super.hasFeature(cx, featureIndex);
                }
            };

    @FunctionalInterface
    private interface TestRunner {
        void run(Context cx, Scriptable scope);
    }

    private void runTest(TestRunner testRunner) {
        contextFactoryWithMapAccess.call(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();
                    testRunner.run(cx, scope);
                    return null;
                });
    }
}
