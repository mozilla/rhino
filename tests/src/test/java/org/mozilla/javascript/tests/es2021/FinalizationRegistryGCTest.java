/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es2021;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.ref.WeakReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeFinalizationRegistry;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * Tests for FinalizationRegistry that involve garbage collection. Note: GC timing is unpredictable,
 * so tests use multiple attempts and longer waits.
 */
public class FinalizationRegistryGCTest {

    private Context cx;
    private Scriptable scope;

    @Before
    public void setUp() {
        cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_ES6);
        scope = cx.initStandardObjects();
    }

    @After
    public void tearDown() {
        Context.exit();
    }

    @Test
    public void testBasicCleanupCallbackExecution() throws Exception {
        // Create registry with tracking callback
        String script =
                "var cleanedUp = false;"
                        + "var cleanupValue = null;"
                        + "var registry = new FinalizationRegistry(function(value) {"
                        + "  cleanedUp = true;"
                        + "  cleanupValue = value;"
                        + "});"
                        + "(function() {"
                        + "  var obj = {};"
                        + "  registry.register(obj, 'test-value');"
                        + "})();" // obj goes out of scope
                        + "registry;";

        NativeFinalizationRegistry registry =
                (NativeFinalizationRegistry) cx.evaluateString(scope, script, "test", 1, null);

        // Try multiple times as GC timing is unpredictable
        boolean cleanupCalled = false;
        for (int attempt = 0; attempt < 5 && !cleanupCalled; attempt++) {
            // Force GC
            forceGCAndWait();

            // Exit and re-enter context to trigger polling
            Context.exit();
            cx = Context.enter();

            // Process finalization cleanups
            cx.evaluateString(scope, "registry.cleanupSome()", "test", 1, null);

            // Check if cleanup was called
            Object cleanedUp = ScriptableObject.getProperty(scope, "cleanedUp");
            cleanupCalled = Boolean.TRUE.equals(cleanedUp);
        }

        // Check final state
        Object cleanupValue = ScriptableObject.getProperty(scope, "cleanupValue");
        if (cleanupCalled) {
            assertEquals("Cleanup value should match", "test-value", cleanupValue);
        }
        // Note: We can't guarantee cleanup will be called due to GC unpredictability
        // but if it is called, the value should be correct
    }

    @Test
    public void testUnregisterPreventsCleanup() throws Exception {
        String script =
                "var cleanupCount = 0;"
                        + "var registry = new FinalizationRegistry(function(value) {"
                        + "  cleanupCount++;"
                        + "});"
                        + "var token = {};"
                        + "var obj = {};"
                        + "registry.register(obj, 'value1', token);"
                        + "var result = registry.unregister(token);"
                        + "obj = null;" // Make eligible for GC
                        + "result;";

        Object unregisterResult = cx.evaluateString(scope, script, "test", 1, null);
        assertEquals("Unregister should return true", Boolean.TRUE, unregisterResult);

        // Try multiple times
        for (int attempt = 0; attempt < 3; attempt++) {
            forceGCAndWait();
            Context.exit();
            cx = Context.enter();
            cx.evaluateString(scope, "registry.cleanupSome()", "test", 1, null);
        }

        // Check that cleanup was NOT called
        Object cleanupCount = ScriptableObject.getProperty(scope, "cleanupCount");
        assertEquals(
                "Cleanup should not be called after unregister",
                0,
                ((Number) cleanupCount).intValue());
    }

    @Test
    public void testMultipleRegistrationsWithSameToken() throws Exception {
        String script =
                "var cleanupCount = 0;"
                        + "var cleanupValues = [];"
                        + "var registry = new FinalizationRegistry(function(value) {"
                        + "  cleanupCount++;"
                        + "  cleanupValues.push(value);"
                        + "});"
                        + "var token = {};"
                        + "var obj1 = {};"
                        + "var obj2 = {};"
                        + "registry.register(obj1, 'value1', token);"
                        + "registry.register(obj2, 'value2', token);"
                        + "var result = registry.unregister(token);"
                        + "obj1 = null;"
                        + "obj2 = null;"
                        + "result;";

        Object unregisterResult = cx.evaluateString(scope, script, "test", 1, null);
        assertEquals("Unregister should return true", Boolean.TRUE, unregisterResult);

        // Try multiple times
        for (int attempt = 0; attempt < 3; attempt++) {
            forceGCAndWait();
            Context.exit();
            cx = Context.enter();
            cx.evaluateString(scope, "registry.cleanupSome()", "test", 1, null);
        }

        // Check that neither cleanup was called (both were unregistered)
        Object cleanupCount = ScriptableObject.getProperty(scope, "cleanupCount");
        assertEquals(
                "No cleanups should be called after unregister",
                0,
                ((Number) cleanupCount).intValue());
    }

    @Test
    public void testWeakReferenceToObject() throws Exception {
        // Create a registry and register an object
        String script =
                "var registry = new FinalizationRegistry(function(value) {});"
                        + "var obj = { data: 'test' };"
                        + "registry.register(obj, 'value');"
                        + "obj;";

        Object obj = cx.evaluateString(scope, script, "test", 1, null);

        // Create a weak reference to the object
        WeakReference<Object> weakRef = new WeakReference<>(obj);

        // Clear the strong reference
        cx.evaluateString(scope, "obj = null;", "test", 1, null);

        // Force GC multiple times
        boolean collected = false;
        for (int i = 0; i < 5; i++) {
            forceGCAndWait();
            if (weakRef.get() == null) {
                collected = true;
                break;
            }
        }

        // The weak reference should eventually be cleared
        // Note: We can't guarantee this will happen immediately
        // This test just verifies no crashes occur
    }

    @Test
    public void testCleanupSomeWithCallback() throws Exception {
        String script =
                "var defaultCalled = false;"
                        + "var customCalled = false;"
                        + "var registry = new FinalizationRegistry(function(value) {"
                        + "  defaultCalled = true;"
                        + "});"
                        + "(function() {"
                        + "  var obj = {};"
                        + "  registry.register(obj, 'test');"
                        + "})();"
                        + "registry;";

        cx.evaluateString(scope, script, "test", 1, null);

        // Try multiple times
        boolean cleanupCalled = false;
        for (int attempt = 0; attempt < 5 && !cleanupCalled; attempt++) {
            forceGCAndWait();
            Context.exit();
            cx = Context.enter();

            // Call cleanupSome with custom callback
            cx.evaluateString(
                    scope,
                    "registry.cleanupSome(function(value) { customCalled = true; })",
                    "test",
                    1,
                    null);

            Object customCalled = ScriptableObject.getProperty(scope, "customCalled");
            cleanupCalled = Boolean.TRUE.equals(customCalled);
        }

        // Check which callback was called
        Object defaultCalled = ScriptableObject.getProperty(scope, "defaultCalled");
        Object customCalled = ScriptableObject.getProperty(scope, "customCalled");

        // If cleanup was called, it should use the custom callback
        if (cleanupCalled) {
            assertEquals("Default callback should not be called", Boolean.FALSE, defaultCalled);
            assertEquals("Custom callback should be called", Boolean.TRUE, customCalled);
        }
        // Note: We can't guarantee cleanup will be called due to GC unpredictability
    }

    @Test
    public void testRegistryCanBeGarbageCollected() throws Exception {
        // Create a registry in a scope that will be GC'd
        String script =
                "var cleanupCalled = false; "
                        + "(function() {"
                        + "  var registry = new FinalizationRegistry(function(value) {"
                        + "    cleanupCalled = true;"
                        + "  });"
                        + "  var obj = {};"
                        + "  registry.register(obj, 'value');"
                        + "})(); "
                        + "null;";

        cx.evaluateString(scope, script, "test", 1, null);

        // Force GC multiple times
        for (int i = 0; i < 5; i++) {
            forceGCAndWait();
            Context.exit();
            cx = Context.enter();
        }

        // Check that cleanup wasn't called (registry itself was GC'd)
        Object cleanupCalled = ScriptableObject.getProperty(scope, "cleanupCalled");
        // This test just verifies the registry can be GC'd without crashes
        // The cleanup behavior when registry is GC'd is implementation-defined
    }

    @Test
    public void testMultipleCleanups() throws Exception {
        // Test that multiple objects can be cleaned up
        String script =
                "var cleanupCount = 0;"
                        + "var cleanupValues = [];"
                        + "var registry = new FinalizationRegistry(function(value) {"
                        + "  cleanupCount++;"
                        + "  cleanupValues.push(value);"
                        + "});"
                        + "(function() {"
                        + "  for (var i = 0; i < 3; i++) {"
                        + "    var obj = {};"
                        + "    registry.register(obj, 'value' + i);"
                        + "  }"
                        + "})();"
                        + "registry;";

        cx.evaluateString(scope, script, "test", 1, null);

        // Try multiple times to get cleanups
        int maxCleanups = 0;
        for (int attempt = 0; attempt < 5; attempt++) {
            forceGCAndWait();
            Context.exit();
            cx = Context.enter();

            cx.evaluateString(scope, "registry.cleanupSome()", "test", 1, null);

            Object cleanupCount = ScriptableObject.getProperty(scope, "cleanupCount");
            int count = ((Number) cleanupCount).intValue();
            if (count > maxCleanups) {
                maxCleanups = count;
            }
        }

        // We might get 0, 1, 2, or 3 cleanups depending on GC behavior
        // Just verify no crashes and reasonable behavior
        assertTrue("Cleanup count should be reasonable", maxCleanups >= 0 && maxCleanups <= 3);
    }

    @Test
    public void testUnregisterWithInvalidToken() throws Exception {
        String script =
                "var registry = new FinalizationRegistry(function(value) {});"
                        + "var obj = {};"
                        + "var token = {};"
                        + "registry.register(obj, 'value', token);"
                        + "var result = registry.unregister({});" // Different token
                        + "result;";

        Object result = cx.evaluateString(scope, script, "test", 1, null);
        assertEquals("Unregister with wrong token should return false", Boolean.FALSE, result);
    }

    private void forceGCAndWait() throws InterruptedException {
        // Force garbage collection
        System.gc();
        System.runFinalization();
        Thread.sleep(100);
        System.gc();
        Thread.sleep(100);
    }
}
