package org.mozilla.javascript.tests.es2021;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.*;

/**
 * White box tests for NativeFinalizationRegistry cleanup methods. These tests use reflection to
 * directly test the private cleanup methods that are normally only triggered by garbage collection.
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
    public void testProcessCleanupDirectly() throws Exception {
        // Create a FinalizationRegistry with a test cleanup callback
        String script =
                "var cleanupCalled = false;"
                        + "var cleanupValue = null;"
                        + "var registry = new FinalizationRegistry(function(value) {"
                        + "  cleanupCalled = true;"
                        + "  cleanupValue = value;"
                        + "});"
                        + "var target = {};"
                        + "registry.register(target, 'test-value');"
                        + "registry;";

        NativeFinalizationRegistry registry =
                (NativeFinalizationRegistry) cx.evaluateString(scope, script, "test", 1, null);

        // Get the private fields using reflection
        Field registrationsField =
                NativeFinalizationRegistry.class.getDeclaredField("registrations");
        registrationsField.setAccessible(true);
        ConcurrentHashMap<?, ?> registrations =
                (ConcurrentHashMap<?, ?>) registrationsField.get(registry);

        // Get the first weak reference
        Object weakRef = registrations.keySet().iterator().next();

        // Get the processCleanup method using reflection
        Method processCleanup =
                NativeFinalizationRegistry.class.getDeclaredMethod(
                        "processCleanup", weakRef.getClass());
        processCleanup.setAccessible(true);

        // Call processCleanup directly
        processCleanup.invoke(registry, weakRef);

        // Verify the cleanup callback was called
        Object cleanupCalled = scope.get("cleanupCalled", scope);
        assertEquals(Boolean.TRUE, cleanupCalled);

        Object cleanupValue = scope.get("cleanupValue", scope);
        assertEquals("test-value", cleanupValue);
    }

    @Test
    public void testExecuteCleanupCallbackWithContext() throws Exception {
        // Create a FinalizationRegistry
        String script =
                "var errorCaught = false;"
                        + "var registry = new FinalizationRegistry(function(value) {"
                        + "  if (value === 'throw') {"
                        + "    throw new Error('Test error');"
                        + "  }"
                        + "});"
                        + "registry;";

        NativeFinalizationRegistry registry =
                (NativeFinalizationRegistry) cx.evaluateString(scope, script, "test", 1, null);

        // Get the executeCleanupCallback method
        Method executeCleanupCallback =
                NativeFinalizationRegistry.class.getDeclaredMethod(
                        "executeCleanupCallback", Object.class);
        executeCleanupCallback.setAccessible(true);

        // Test normal execution
        executeCleanupCallback.invoke(registry, "normal-value");

        // Test error handling - should not throw
        try {
            executeCleanupCallback.invoke(registry, "throw");
            // Should not throw an exception - errors are caught and reported
        } catch (Exception e) {
            fail("executeCleanupCallback should catch and report errors, not rethrow them");
        }
    }

    @Test
    public void testExecuteCleanupCallbackWithoutContext() throws Exception {
        // Exit the current context to test the Context.enter() path
        Context.exit();

        try {
            // Create registry while context is active
            cx = Context.enter();
            String script =
                    "var callbackExecuted = false;"
                            + "var registry = new FinalizationRegistry(function(value) {"
                            + "  callbackExecuted = true;"
                            + "});"
                            + "registry;";

            NativeFinalizationRegistry registry =
                    (NativeFinalizationRegistry) cx.evaluateString(scope, script, "test", 1, null);
            Context.exit();

            // Now call executeCleanupCallback without an active context
            Method executeCleanupCallback =
                    NativeFinalizationRegistry.class.getDeclaredMethod(
                            "executeCleanupCallback", Object.class);
            executeCleanupCallback.setAccessible(true);

            // This should use try-with-resources to enter a new context
            executeCleanupCallback.invoke(registry, "test-value");

            // Verify callback was executed
            cx = Context.enter();
            Object callbackExecuted = scope.get("callbackExecuted", scope);
            assertEquals(Boolean.TRUE, callbackExecuted);
        } finally {
            if (Context.getCurrentContext() == null) {
                cx = Context.enter();
            }
        }
    }

    @Test
    public void testCallCleanupCallbackErrorHandling() throws Exception {
        // Create a registry with a callback that throws
        String script =
                "var registry = new FinalizationRegistry(function(value) {"
                        + "  throw new TypeError('Intentional error');"
                        + "});"
                        + "registry;";

        NativeFinalizationRegistry registry =
                (NativeFinalizationRegistry) cx.evaluateString(scope, script, "test", 1, null);

        // Get the callCleanupCallback method
        Method callCleanupCallback =
                NativeFinalizationRegistry.class.getDeclaredMethod(
                        "callCleanupCallback", Context.class, Object.class);
        callCleanupCallback.setAccessible(true);

        // Should not throw - errors are caught and reported as warnings
        try {
            callCleanupCallback.invoke(registry, cx, "test-value");
            // Success - error was caught and reported
        } catch (Exception e) {
            fail("callCleanupCallback should catch RhinoExceptions and report as warnings");
        }
    }

    @Test
    public void testProcessPendingCleanupsWithMultipleEntries() throws Exception {
        // Create a registry and register multiple objects
        String script =
                "var cleanupCount = 0;"
                        + "var cleanupValues = [];"
                        + "var registry = new FinalizationRegistry(function(value) {"
                        + "  cleanupCount++;"
                        + "  cleanupValues.push(value);"
                        + "});"
                        + "var obj1 = {};"
                        + "var obj2 = {};"
                        + "var obj3 = {};"
                        + "var token1 = {};"
                        + "var token2 = {};"
                        + "var token3 = {};"
                        + "registry.register(obj1, 'value1', token1);"
                        + "registry.register(obj2, 'value2', token2);"
                        + "registry.register(obj3, 'value3', token3);"
                        + "registry;";

        NativeFinalizationRegistry registry =
                (NativeFinalizationRegistry) cx.evaluateString(scope, script, "test", 1, null);

        // Get access to private fields
        Field registrationsField =
                NativeFinalizationRegistry.class.getDeclaredField("registrations");
        registrationsField.setAccessible(true);
        ConcurrentHashMap<?, ?> registrations =
                (ConcurrentHashMap<?, ?>) registrationsField.get(registry);

        // Process each cleanup
        Method processCleanup =
                NativeFinalizationRegistry.class.getDeclaredMethod(
                        "processCleanup", registrations.keySet().iterator().next().getClass());
        processCleanup.setAccessible(true);

        for (Object weakRef : registrations.keySet().toArray()) {
            processCleanup.invoke(registry, weakRef);
        }

        // Verify all callbacks were called
        Object cleanupCount = scope.get("cleanupCount", scope);
        assertEquals(3.0, ((Number) cleanupCount).doubleValue(), 0.001);

        Object cleanupValues = scope.get("cleanupValues", scope);
        assertTrue(cleanupValues instanceof NativeArray);
        NativeArray valuesArray = (NativeArray) cleanupValues;
        assertEquals(3, valuesArray.size());
    }

    @Test
    public void testUnregisterTokenRemoval() throws Exception {
        // Create a registry with token-based registrations
        String script =
                "var registry = new FinalizationRegistry(function(value) {});"
                        + "var obj1 = {};"
                        + "var obj2 = {};"
                        + "var token = {};"
                        + "registry.register(obj1, 'value1', token);"
                        + "registry.register(obj2, 'value2', token);"
                        + "registry;";

        NativeFinalizationRegistry registry =
                (NativeFinalizationRegistry) cx.evaluateString(scope, script, "test", 1, null);

        // Get the tokenMap field
        Field tokenMapField = NativeFinalizationRegistry.class.getDeclaredField("tokenMap");
        tokenMapField.setAccessible(true);
        ConcurrentHashMap<?, ?> tokenMap = (ConcurrentHashMap<?, ?>) tokenMapField.get(registry);

        // Verify token is in the map with 2 references
        assertEquals(1, tokenMap.size());
        Object refs = tokenMap.values().iterator().next();
        assertTrue(refs instanceof java.util.Set);
        assertEquals(2, ((java.util.Set<?>) refs).size());

        // Now unregister using the token
        cx.evaluateString(scope, "registry.unregister(token);", "test", 1, null);

        // Verify token was removed from map
        assertEquals(0, tokenMap.size());
    }
}
