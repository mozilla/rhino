/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.testutils.Utils;

/**
 * Tests for NativeJavaCode — the JSCode<JSFunction> implementation for Java methods exposed to
 * JavaScript.
 */
public class NativeJavaCodeTest {

    /** Helper class for testing Java method invocation from JavaScript. */
    public static class JavaMethodDummy {
        public final List<String> captured = new ArrayList<>();

        public String greet(String name) {
            captured.add("greet");
            return "Hello, " + name;
        }

        public int add(int a, int b) {
            captured.add("add");
            return a + b;
        }

        public void doNothing() {
            captured.add("doNothing");
        }

        public static int staticAdd(int a, int b) {
            return a + b;
        }
    }

    @Test
    public void testSimpleMethodCall() {
        Utils.runWithAllModes(
                cx -> {
                    final var dummy = new JavaMethodDummy();
                    final var scope = cx.initStandardObjects();
                    ScriptableObject.putProperty(scope, "obj", Context.javaToJS(dummy, scope));

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "var x = obj.greet('World'); x;",
                                    "NativeJavaCodeTest.js",
                                    0,
                                    null);
                    assertEquals(NativeJavaObject.class, result.getClass());
                    assertEquals("Hello, World", ((NativeJavaObject) result).unwrap());
                    assertEquals("greet", dummy.captured.get(0));
                    return null;
                });
    }

    @Test
    public void testIntegerReturn() {
        Utils.runWithAllModes(
                cx -> {
                    final var dummy = new JavaMethodDummy();
                    final var scope = cx.initStandardObjects();
                    ScriptableObject.putProperty(scope, "obj", Context.javaToJS(dummy, scope));

                    Object result =
                            cx.evaluateString(
                                    scope, "obj.add(2, 3);", "NativeJavaCodeTest.js", 0, null);

                    assertEquals(5, ((Number) result).intValue());
                    assertEquals("add", dummy.captured.get(0));
                    return null;
                });
    }

    @Test
    public void testVoidReturn() {
        Utils.runWithAllModes(
                cx -> {
                    final var dummy = new JavaMethodDummy();
                    final var scope = cx.initStandardObjects();
                    ScriptableObject.putProperty(scope, "obj", Context.javaToJS(dummy, scope));

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "obj.doNothing(); 'done';",
                                    "NativeJavaCodeTest.js",
                                    0,
                                    null);

                    assertEquals("done", result);
                    assertEquals("doNothing", dummy.captured.get(0));
                    return null;
                });
    }

    @Test
    public void testStaticMethod() {
        Utils.runWithAllModes(
                cx -> {
                    final var scope = cx.initStandardObjects();
                    ScriptableObject.putProperty(
                            scope,
                            "JavaMethodDummy",
                            new org.mozilla.javascript.NativeJavaClass(
                                    scope, JavaMethodDummy.class));

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "JavaMethodDummy.staticAdd(5, 7);",
                                    "NativeJavaCodeTest.js",
                                    0,
                                    null);

                    assertEquals(12, ((Number) result).intValue());
                    return null;
                });
    }
}
