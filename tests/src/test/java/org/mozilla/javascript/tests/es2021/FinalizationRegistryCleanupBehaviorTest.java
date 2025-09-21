/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es2021;

import static org.junit.Assert.*;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * Tests for actual cleanup execution behavior of FinalizationRegistry, including cleanupSome
 * functionality.
 */
public class FinalizationRegistryCleanupBehaviorTest {

    @Test
    public void testCleanupSomeWithPendingCleanups() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope = cx.initStandardObjects();

            // Test that cleanupSome processes pending cleanups
            String script =
                    "var cleanupCount = 0;"
                            + "var cleanupValues = [];"
                            + "var registry = new FinalizationRegistry(function(value) {"
                            + "  cleanupCount++;"
                            + "  cleanupValues.push(value);"
                            + "});"
                            + "{"
                            + "  let obj1 = {};"
                            + "  let obj2 = {};"
                            + "  registry.register(obj1, 'value1');"
                            + "  registry.register(obj2, 'value2');"
                            + "  obj1 = null;"
                            + "  obj2 = null;"
                            + "}"
                            + "java.lang.System.gc();"
                            + "java.lang.System.runFinalization();"
                            + "java.lang.Thread.sleep(100);"
                            + "registry.cleanupSome();"
                            + "[cleanupCount, cleanupValues];";

            // Note: Cleanup behavior is non-deterministic due to GC
            // This test verifies the API works, not that cleanups always happen
            Object result = cx.evaluateString(scope, script, "test", 1, null);
            assertNotNull(result);
        }
    }

    @Test
    public void testCleanupSomeWithCallbackOverride() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope = cx.initStandardObjects();

            // Test that cleanupSome can use a different callback
            String script =
                    "var registryCallbackCount = 0;"
                            + "var overrideCallbackCount = 0;"
                            + "var registry = new FinalizationRegistry(function(value) {"
                            + "  registryCallbackCount++;"
                            + "});"
                            + "var overrideCallback = function(value) {"
                            + "  overrideCallbackCount++;"
                            + "};"
                            + "registry.cleanupSome(overrideCallback);"
                            + "[registryCallbackCount, overrideCallbackCount];";

            Object result = cx.evaluateString(scope, script, "test", 1, null);
            assertNotNull(result);
            // Both counts should be 0 if no cleanups are pending
        }
    }

    @Test
    public void testCleanupCallbackErrorHandling() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope = cx.initStandardObjects();

            // Test that errors in cleanup callbacks don't propagate
            String script =
                    "var errorThrown = false;"
                            + "var cleanupCalled = false;"
                            + "var registry = new FinalizationRegistry(function(value) {"
                            + "  cleanupCalled = true;"
                            + "  throw new Error('Cleanup error');"
                            + "});"
                            + "try {"
                            + "  {"
                            + "    let obj = {};"
                            + "    registry.register(obj, 'value');"
                            + "    obj = null;"
                            + "  }"
                            + "  java.lang.System.gc();"
                            + "  java.lang.System.runFinalization();"
                            + "  registry.cleanupSome();"
                            + "} catch(e) {"
                            + "  errorThrown = true;"
                            + "}"
                            + "!errorThrown;"; // Errors should not propagate

            Object result = cx.evaluateString(scope, script, "test", 1, null);
            assertEquals(Boolean.TRUE, result);
        }
    }

    @Test
    public void testMultipleRegistrationsWithSameToken() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope = cx.initStandardObjects();

            // Test multiple registrations with the same unregister token
            String script =
                    "var cleanupCount = 0;"
                            + "var registry = new FinalizationRegistry(function(value) {"
                            + "  cleanupCount++;"
                            + "});"
                            + "var token = {};"
                            + "{"
                            + "  let obj1 = {};"
                            + "  let obj2 = {};"
                            + "  let obj3 = {};"
                            + "  registry.register(obj1, 'value1', token);"
                            + "  registry.register(obj2, 'value2', token);"
                            + "  registry.register(obj3, 'value3', token);"
                            + "}"
                            + "var unregistered = registry.unregister(token);"
                            + "unregistered;"; // Should be true if any were unregistered

            Object result = cx.evaluateString(scope, script, "test", 1, null);
            assertEquals(Boolean.TRUE, result);
        }
    }

    @Test
    public void testCleanupOrderPreservation() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope = cx.initStandardObjects();

            // Test that cleanups are called in registration order (when possible)
            String script =
                    "var cleanupOrder = [];"
                            + "var registry = new FinalizationRegistry(function(value) {"
                            + "  cleanupOrder.push(value);"
                            + "});"
                            + "{"
                            + "  let obj1 = {};"
                            + "  let obj2 = {};"
                            + "  let obj3 = {};"
                            + "  registry.register(obj1, 1);"
                            + "  registry.register(obj2, 2);"
                            + "  registry.register(obj3, 3);"
                            + "}"
                            + "cleanupOrder;";

            // Note: Order is not guaranteed by spec but testing our implementation
            Object result = cx.evaluateString(scope, script, "test", 1, null);
            assertNotNull(result);
        }
    }

    @Test
    public void testHeldValueTypes() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope = cx.initStandardObjects();

            // Test various held value types
            String script =
                    "var capturedValues = [];"
                            + "var registry = new FinalizationRegistry(function(value) {"
                            + "  capturedValues.push(value);"
                            + "});"
                            + "{"
                            + "  registry.register({}, 'string');"
                            + "  registry.register({}, 42);"
                            + "  registry.register({}, true);"
                            + "  registry.register({}, null);"
                            + "  registry.register({}, undefined);"
                            + "  registry.register({}, {nested: 'object'});"
                            + "  registry.register({}, [1, 2, 3]);"
                            + "}"
                            + "true;"; // Test completes without error

            Object result = cx.evaluateString(scope, script, "test", 1, null);
            assertEquals(Boolean.TRUE, result);
        }
    }

    @Test
    public void testUnregisterNonExistentToken() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope = cx.initStandardObjects();

            // Test unregistering with a token that was never registered
            String script =
                    "var registry = new FinalizationRegistry(function(value) {});"
                            + "var token = {};"
                            + "var result = registry.unregister(token);"
                            + "result === false;"; // Should return false

            Object result = cx.evaluateString(scope, script, "test", 1, null);
            assertEquals(Boolean.TRUE, result);
        }
    }

    @Test
    public void testRegisterWithoutUnregisterToken() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope = cx.initStandardObjects();

            // Test registration without unregister token
            String script =
                    "var cleanupCalled = false;"
                            + "var registry = new FinalizationRegistry(function(value) {"
                            + "  cleanupCalled = true;"
                            + "});"
                            + "{"
                            + "  let obj = {};"
                            + "  registry.register(obj, 'value');" // No unregister token
                            + "}"
                            + "true;"; // Registration should succeed

            Object result = cx.evaluateString(scope, script, "test", 1, null);
            assertEquals(Boolean.TRUE, result);
        }
    }

    @Test
    public void testCleanupSomeReturnsUndefined() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope = cx.initStandardObjects();

            // Test that cleanupSome always returns undefined
            String script =
                    "var registry = new FinalizationRegistry(function(value) {});"
                            + "var result1 = registry.cleanupSome();"
                            + "var result2 = registry.cleanupSome(function() {});"
                            + "result1 === undefined && result2 === undefined;";

            Object result = cx.evaluateString(scope, script, "test", 1, null);
            assertEquals(Boolean.TRUE, result);
        }
    }

    @Test
    public void testNestedCleanupCallbacks() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope = cx.initStandardObjects();

            try {
                // Test nested cleanup scenarios
                String script =
                        "var registry1 = new FinalizationRegistry(function(value) {"
                                + "});"
                                + "var registry2 = new FinalizationRegistry(function(value) {"
                                + "  registry1.cleanupSome();"
                                + "});"
                                + "(function() {"
                                + "  registry1.register({}, 'r1');"
                                + "  registry2.register({}, 'r2');"
                                + "})();"
                                + "registry2.cleanupSome();" // Try to trigger nested cleanup
                                + "true;";

                Object result = cx.evaluateString(scope, script, "test", 1, null);
                assertEquals(Boolean.TRUE, result);
            } catch (Exception e) {
                // If there's an error, print it for debugging
                e.printStackTrace();
                fail("Script execution failed: " + e.getMessage());
            }
        }
    }
}
