/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es2021;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeFinalizationRegistry;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

/**
 * Internal tests for FinalizationRegistry using reflection to test private methods. These are
 * "white box" tests as requested by reviewer.
 */
public class FinalizationRegistryInternalTest {

    @Test
    public void testProcessPendingCleanupsMethod() throws Exception {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope = cx.initStandardObjects();

            // Create a FinalizationRegistry
            String script = "new FinalizationRegistry(function(heldValue) {})";
            Object registry = cx.evaluateString(scope, script, "test", 1, null);
            assertTrue(registry instanceof NativeFinalizationRegistry);

            // Use reflection to access processPendingCleanups method
            Method method =
                    NativeFinalizationRegistry.class.getDeclaredMethod("processPendingCleanups");
            method.setAccessible(true);

            // Should not throw exception
            method.invoke(registry);
        }
    }

    @Test
    public void testRegistrationsMapInitialization() throws Exception {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope = cx.initStandardObjects();

            // Create a FinalizationRegistry
            String script = "new FinalizationRegistry(function(heldValue) {})";
            Object registry = cx.evaluateString(scope, script, "test", 1, null);
            assertTrue(registry instanceof NativeFinalizationRegistry);

            // Use reflection to access registrations field
            Field field = NativeFinalizationRegistry.class.getDeclaredField("registrations");
            field.setAccessible(true);
            Object registrations = field.get(registry);

            assertNotNull(registrations);
            assertTrue(registrations instanceof ConcurrentHashMap);
            assertEquals(0, ((ConcurrentHashMap<?, ?>) registrations).size());
        }
    }

    @Test
    public void testTokenMapInitialization() throws Exception {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope = cx.initStandardObjects();

            // Create a FinalizationRegistry
            String script = "new FinalizationRegistry(function(heldValue) {})";
            Object registry = cx.evaluateString(scope, script, "test", 1, null);
            assertTrue(registry instanceof NativeFinalizationRegistry);

            // Use reflection to access tokenMap field
            Field field = NativeFinalizationRegistry.class.getDeclaredField("tokenMap");
            field.setAccessible(true);
            Object tokenMap = field.get(registry);

            assertNotNull(tokenMap);
            assertTrue(tokenMap instanceof ConcurrentHashMap);
            assertEquals(0, ((ConcurrentHashMap<?, ?>) tokenMap).size());
        }
    }

    @Test
    public void testCleanupCallbackStorage() throws Exception {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope = cx.initStandardObjects();

            // Create a FinalizationRegistry
            String script =
                    "var callback = function(heldValue) {}; new FinalizationRegistry(callback)";
            Object registry = cx.evaluateString(scope, script, "test", 1, null);
            assertTrue(registry instanceof NativeFinalizationRegistry);

            // Use reflection to access cleanupCallback field
            Field field = NativeFinalizationRegistry.class.getDeclaredField("cleanupCallback");
            field.setAccessible(true);
            Object cleanupCallback = field.get(registry);

            assertNotNull(cleanupCallback);
            assertTrue(cleanupCallback instanceof Function);
        }
    }

    @Test
    public void testExecuteCleanupCallbackMethod() throws Exception {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope = cx.initStandardObjects();

            // Create a FinalizationRegistry with a callback that sets a flag
            String script =
                    "var called = false;"
                            + "var callback = function(heldValue) { called = true; };"
                            + "new FinalizationRegistry(callback)";
            Object registry = cx.evaluateString(scope, script, "test", 1, null);
            assertTrue(registry instanceof NativeFinalizationRegistry);

            // Use reflection to call executeCleanupCallback
            Method method =
                    NativeFinalizationRegistry.class.getDeclaredMethod(
                            "executeCleanupCallback", Object.class);
            method.setAccessible(true);
            method.invoke(registry, "test value");

            // Check if callback was called
            Object called = ScriptableObject.getProperty(scope, "called");
            assertEquals(Boolean.TRUE, called);
        }
    }

    @Test
    public void testPerformCleanupSomeMethod() throws Exception {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope = cx.initStandardObjects();

            // Create a FinalizationRegistry
            String script = "new FinalizationRegistry(function(heldValue) {})";
            Object registry = cx.evaluateString(scope, script, "test", 1, null);
            assertTrue(registry instanceof NativeFinalizationRegistry);

            // Use reflection to access performCleanupSome method
            Method method =
                    NativeFinalizationRegistry.class.getDeclaredMethod(
                            "performCleanupSome", Context.class, Function.class);
            method.setAccessible(true);

            // Should not throw exception
            method.invoke(registry, cx, null);
        }
    }

    @Test
    public void testIsValidTargetMethod() throws Exception {
        // Use reflection to test the static isValidTarget method
        Method method =
                NativeFinalizationRegistry.class.getDeclaredMethod("isValidTarget", Object.class);
        method.setAccessible(true);

        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope = cx.initStandardObjects();

            // Test with valid object
            Object obj = cx.evaluateString(scope, "({})", "test", 1, null);
            assertTrue((Boolean) method.invoke(null, obj));

            // Test with undefined
            assertEquals(false, method.invoke(null, Undefined.instance));

            // Test with null
            assertEquals(false, method.invoke(null, new Object[] {null}));

            // Test with primitive wrapped as Object
            assertEquals(false, method.invoke(null, "string"));
            assertEquals(false, method.invoke(null, 42));
            assertEquals(false, method.invoke(null, true));
        }
    }
}
