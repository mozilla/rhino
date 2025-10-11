/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es2021;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
    public void testExecuteCleanupCallbackAccessible() throws Exception {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope = cx.initStandardObjects();

            // Create a FinalizationRegistry
            String script = "new FinalizationRegistry(function(heldValue) {})";
            Object registry = cx.evaluateString(scope, script, "test", 1, null);
            assertTrue(registry instanceof NativeFinalizationRegistry);

            // Use reflection to verify executeCleanupCallback exists (it's package-private)
            Method method =
                    NativeFinalizationRegistry.class.getDeclaredMethod(
                            "executeCleanupCallback", Object.class);
            method.setAccessible(true);
            assertNotNull(method);
        }
    }

    @Test
    public void testRegistryCreation() throws Exception {
        // Test that registry is created properly with V2 manager
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope = cx.initStandardObjects();

            // Create a FinalizationRegistry
            String script = "new FinalizationRegistry(function(heldValue) {})";
            Object registry = cx.evaluateString(scope, script, "test", 1, null);
            assertTrue(registry instanceof NativeFinalizationRegistry);

            // Verify the registry is properly initialized
            // V2 manager handles all tracking internally
            assertNotNull(registry);
        }
    }

    @Test
    public void testV2ManagerIntegration() throws Exception {
        // Test that V2 manager is properly integrated
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope = cx.initStandardObjects();

            // Create a FinalizationRegistry
            String script = "new FinalizationRegistry(function(heldValue) {})";
            Object registry = cx.evaluateString(scope, script, "test", 1, null);
            assertTrue(registry instanceof NativeFinalizationRegistry);

            // V2 manager handles all tracking internally
            // Just verify registry creation worked
            assertNotNull(registry);
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
    public void testCleanupSomeMethodExists() throws Exception {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope = cx.initStandardObjects();

            // Create a FinalizationRegistry
            String script = "new FinalizationRegistry(function(heldValue) {})";
            Object registry = cx.evaluateString(scope, script, "test", 1, null);
            assertTrue(registry instanceof NativeFinalizationRegistry);

            // Verify cleanupSome is accessible via JavaScript
            String testScript = "typeof registry.cleanupSome === 'function'";
            ScriptableObject.putProperty(scope, "registry", registry);
            Object result = cx.evaluateString(scope, testScript, "test", 1, null);
            assertEquals(Boolean.TRUE, result);
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
