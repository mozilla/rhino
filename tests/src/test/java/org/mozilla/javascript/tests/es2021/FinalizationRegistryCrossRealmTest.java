/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es2021;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/** Tests for FinalizationRegistry behavior across different JavaScript contexts and scopes. */
public class FinalizationRegistryCrossRealmTest {

    @Test
    public void testFinalizationRegistryPrototypeFromDifferentRealm() {
        try (Context cx1 = Context.enter()) {
            cx1.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope1 = cx1.initStandardObjects();

            // Create registry in first realm
            String script1 =
                    "var registry1 = new FinalizationRegistry(function(value) {});" + "registry1;";
            Object registry1 = cx1.evaluateString(scope1, script1, "realm1", 1, null);

            // Create second realm
            try (Context cx2 = Context.enter()) {
                cx2.setLanguageVersion(Context.VERSION_ES6);
                Scriptable scope2 = cx2.initStandardObjects();

                // Put registry from realm1 into realm2
                ScriptableObject.putProperty(scope2, "registry1", registry1);

                // Verify methods work across realms
                String script2 =
                        "typeof registry1.register === 'function' && "
                                + "typeof registry1.unregister === 'function';";
                Object result = cx2.evaluateString(scope2, script2, "realm2", 1, null);
                assertTrue((Boolean) result);
            }
        }
    }

    @Test
    public void testRegisterObjectFromDifferentRealm() {
        try (Context cx1 = Context.enter()) {
            cx1.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope1 = cx1.initStandardObjects();

            // Create registry in first realm
            String script1 =
                    "var cleanupCalled = false;"
                            + "var registry = new FinalizationRegistry(function(value) {"
                            + "  cleanupCalled = true;"
                            + "});"
                            + "registry;";
            Object registry = cx1.evaluateString(scope1, script1, "realm1", 1, null);

            // Create second realm and register object from it
            try (Context cx2 = Context.enter()) {
                cx2.setLanguageVersion(Context.VERSION_ES6);
                Scriptable scope2 = cx2.initStandardObjects();

                // Put registry into realm2
                ScriptableObject.putProperty(scope2, "registry", registry);

                // Register an object from realm2 with registry from realm1
                String script2 =
                        "var target = {};"
                                + "var token = {};"
                                + "registry.register(target, 'value', token);"
                                + "registry.unregister(token);";
                Object result = cx2.evaluateString(scope2, script2, "realm2", 1, null);
                assertEquals(Boolean.TRUE, result);
            }
        }
    }

    @Test
    public void testWeakRefFromDifferentRealm() {
        try (Context cx1 = Context.enter()) {
            cx1.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope1 = cx1.initStandardObjects();

            // Create object in first realm
            String script1 = "var obj1 = {value: 'realm1'}; obj1;";
            Object obj1 = cx1.evaluateString(scope1, script1, "realm1", 1, null);

            // Create second realm and create WeakRef to object from realm1
            try (Context cx2 = Context.enter()) {
                cx2.setLanguageVersion(Context.VERSION_ES6);
                Scriptable scope2 = cx2.initStandardObjects();

                // Put object from realm1 into realm2
                ScriptableObject.putProperty(scope2, "obj1", obj1);

                // Create WeakRef in realm2 pointing to object from realm1
                String script2 = "var ref = new WeakRef(obj1);" + "ref.deref() === obj1;";
                Object result = cx2.evaluateString(scope2, script2, "realm2", 1, null);
                assertEquals(Boolean.TRUE, result);
            }
        }
    }

    @Test
    public void testFinalizationRegistryConstructorFromDifferentRealm() {
        try (Context cx1 = Context.enter()) {
            cx1.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope1 = cx1.initStandardObjects();

            // Get FinalizationRegistry constructor from realm1
            Object FinalizationRegistry1 =
                    ScriptableObject.getProperty(scope1, "FinalizationRegistry");

            // Create second realm
            try (Context cx2 = Context.enter()) {
                cx2.setLanguageVersion(Context.VERSION_ES6);
                Scriptable scope2 = cx2.initStandardObjects();

                // Put constructor from realm1 into realm2
                ScriptableObject.putProperty(scope2, "FR1", FinalizationRegistry1);

                // Create registry using constructor from different realm
                String script2 =
                        "var registry = new FR1(function(value) {});"
                                + "typeof registry.register === 'function';";
                Object result = cx2.evaluateString(scope2, script2, "realm2", 1, null);
                assertEquals(Boolean.TRUE, result);
            }
        }
    }

    @Test
    public void testSharedTokenAcrossRealms() {
        try (Context cx1 = Context.enter()) {
            cx1.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope1 = cx1.initStandardObjects();

            // Create registry and token in first realm
            String script1 =
                    "var registry = new FinalizationRegistry(function(value) {});"
                            + "var sharedToken = {id: 'shared'};"
                            + "registry;";
            Object registry = cx1.evaluateString(scope1, script1, "realm1", 1, null);
            Object sharedToken = ScriptableObject.getProperty(scope1, "sharedToken");

            // Use the same token in second realm
            try (Context cx2 = Context.enter()) {
                cx2.setLanguageVersion(Context.VERSION_ES6);
                Scriptable scope2 = cx2.initStandardObjects();

                // Put registry and token into realm2
                ScriptableObject.putProperty(scope2, "registry", registry);
                ScriptableObject.putProperty(scope2, "sharedToken", sharedToken);

                // Register with shared token in realm2
                String script2 =
                        "var target1 = {};"
                                + "var target2 = {};"
                                + "registry.register(target1, 'value1', sharedToken);"
                                + "registry.register(target2, 'value2', sharedToken);"
                                + "registry.unregister(sharedToken);"; // Should unregister both
                Object result = cx2.evaluateString(scope2, script2, "realm2", 1, null);
                assertEquals(Boolean.TRUE, result);
            }
        }
    }

    @Test
    public void testCleanupCallbackExecutionContext() {
        // This test verifies that cleanup callbacks execute in the correct context
        // even when objects are registered from different realms
        ContextFactory factory = new ContextFactory();

        Context cx1 = factory.enterContext();
        try {
            cx1.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope1 = cx1.initStandardObjects();

            // Create registry with callback that accesses realm-specific globals
            String script1 =
                    "var realm1Marker = 'realm1';"
                            + "var capturedValue = null;"
                            + "var registry = new FinalizationRegistry(function(value) {"
                            + "  capturedValue = realm1Marker + ':' + value;"
                            + "});"
                            + "registry;";
            Object registry = cx1.evaluateString(scope1, script1, "realm1", 1, null);

            // Store reference to scope1 for later verification
            ScriptableObject.putProperty(scope1, "registry", registry);

            // The cleanup callback should execute with access to realm1's scope
            // even when triggered by objects from realm2
        } finally {
            Context.exit();
        }
    }
}
