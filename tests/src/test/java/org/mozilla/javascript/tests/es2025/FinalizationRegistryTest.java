/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es2025;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

public class FinalizationRegistryTest {

    // ========== UTILITY METHODS ==========

    private void assertES6(String script, Object expected) {
        Utils.assertWithAllModes_ES6(expected, script);
    }

    private void assertES18(String script, Object expected) {
        Utils.assertWithAllModes_1_8(expected, script);
    }

    private void assertTypeError(String script) {
        assertES6("try { " + script + "; false; } catch(e) { e instanceof TypeError; }", true);
    }

    // ========== BASIC FUNCTIONALITY TESTS ==========

    @Test
    public void finalizationRegistryConstructor() {
        assertES6(
                "var registry = new FinalizationRegistry(function(){}); registry instanceof FinalizationRegistry",
                true);
    }

    @Test
    public void finalizationRegistryRegisterBasic() {
        assertES6(
                "var registry = new FinalizationRegistry(function(){}); var obj = {}; registry.register(obj, 'cleanup data'); true",
                true);
    }

    @Test
    public void finalizationRegistryRegisterWithToken() {
        assertES6(
                "var registry = new FinalizationRegistry(function(){}); var obj = {}; var token = {}; registry.register(obj, 'cleanup data', token); true",
                true);
    }

    @Test
    public void finalizationRegistryToStringTag() {
        assertES6(
                "var registry = new FinalizationRegistry(function(){}); Object.prototype.toString.call(registry)",
                "[object FinalizationRegistry]");
    }

    @Test
    public void finalizationRegistryRegisterReturnsUndefined() {
        assertES6(
                "var registry = new FinalizationRegistry(function(){}); var result = registry.register({}, 'cleanup data'); result === undefined",
                true);
    }

    // ========== ERROR CONDITION TESTS ==========

    @Test
    public void finalizationRegistryRequiresNew() {
        assertTypeError("FinalizationRegistry(function(){})");
    }

    @Test
    public void finalizationRegistryRequiresCallback() {
        assertTypeError("new FinalizationRegistry()");
    }

    @Test
    public void finalizationRegistryCallbackMustBeFunction() {
        assertTypeError("new FinalizationRegistry('not a function')");
    }

    @Test
    public void finalizationRegistryRegisterInvalidTarget() {
        assertES6(
                "var registry = new FinalizationRegistry(function(){}); try { registry.register(null, 'cleanup data'); false; } catch(e) { e instanceof TypeError; }",
                true);
    }

    @Test
    public void finalizationRegistryRegisterPrimitiveTarget() {
        assertES6(
                "var registry = new FinalizationRegistry(function(){}); try { registry.register(42, 'cleanup data'); false; } catch(e) { e instanceof TypeError; }",
                true);
    }

    @Test
    public void finalizationRegistryRegisterSymbolTarget() {
        assertES6(
                "var registry = new FinalizationRegistry(function(){}); try { registry.register(Symbol('test'), 'cleanup data'); false; } catch(e) { e instanceof TypeError; }",
                true);
    }

    @Test
    public void finalizationRegistryRegisterSameTarget() {
        assertES6(
                "var registry = new FinalizationRegistry(function(){}); var obj = {}; try { registry.register(obj, obj); false; } catch(e) { e instanceof TypeError; }",
                true);
    }

    @Test
    public void finalizationRegistryRegisterWithoutHeldValue() {
        assertES6(
                "var registry = new FinalizationRegistry(function(){}); try { registry.register({}); false; } catch(e) { e instanceof TypeError; }",
                true);
    }

    // ========== UNREGISTER TESTS ==========

    @Test
    public void finalizationRegistryUnregisterWithoutToken() {
        assertES6(
                "var registry = new FinalizationRegistry(function(){}); registry.unregister()",
                false);
    }

    @Test
    public void finalizationRegistryUnregisterNonExistentToken() {
        assertES6(
                "var registry = new FinalizationRegistry(function(){}); registry.unregister({})",
                false);
    }

    @Test
    public void finalizationRegistryUnregisterExistingToken() {
        assertES6(
                "var registry = new FinalizationRegistry(function(){}); var obj = {}; var token = {}; registry.register(obj, 'cleanup data', token); registry.unregister(token)",
                true);
    }

    @Test
    public void finalizationRegistryUnregisterMultipleWithSameToken() {
        assertES6(
                "var registry = new FinalizationRegistry(function(){}); var obj1 = {}; var obj2 = {}; var token = {}; registry.register(obj1, 'data1', token); registry.register(obj2, 'data2', token); registry.unregister(token)",
                true);
    }

    // ========== DIFFERENT OBJECT TYPES TESTS ==========

    @Test
    public void finalizationRegistryWithArray() {
        assertES6(
                "var registry = new FinalizationRegistry(function(){}); registry.register([], 'array data'); true",
                true);
    }

    @Test
    public void finalizationRegistryWithFunction() {
        assertES6(
                "var registry = new FinalizationRegistry(function(){}); registry.register(function(){}, 'function data'); true",
                true);
    }

    @Test
    public void finalizationRegistryWithRegExp() {
        assertES6(
                "var registry = new FinalizationRegistry(function(){}); registry.register(/test/, 'regexp data'); true",
                true);
    }

    @Test
    public void finalizationRegistryWithDate() {
        assertES6(
                "var registry = new FinalizationRegistry(function(){}); registry.register(new Date(), 'date data'); true",
                true);
    }

    @Test
    public void finalizationRegistryMultipleRegistrations() {
        assertES6(
                "var registry = new FinalizationRegistry(function(){}); var obj1 = {}; var obj2 = {}; var obj3 = {}; registry.register(obj1, 'data1'); registry.register(obj2, 'data2'); registry.register(obj3, 'data3'); true",
                true);
    }

    // ========== PROTOTYPE AND CONSTRUCTOR PROPERTIES ==========

    @Test
    public void finalizationRegistryTypeofFunction() {
        assertES6("typeof FinalizationRegistry === 'function'", true);
    }

    @Test
    public void finalizationRegistryPrototypeMethods() {
        assertES6(
                "typeof FinalizationRegistry.prototype.register === 'function' && typeof FinalizationRegistry.prototype.unregister === 'function'",
                true);
    }

    @Test
    public void finalizationRegistryConstructorLength() {
        assertES6("FinalizationRegistry.length === 1", true);
    }

    @Test
    public void finalizationRegistryConstructorName() {
        assertES6("FinalizationRegistry.name === 'FinalizationRegistry'", true);
    }

    @Test
    public void finalizationRegistryRegisterLength() {
        assertES6("FinalizationRegistry.prototype.register.length === 2", true);
    }

    @Test
    public void finalizationRegistryUnregisterLength() {
        assertES6("FinalizationRegistry.prototype.unregister.length === 1", true);
    }

    @Test
    public void finalizationRegistryHasCorrectPrototype() {
        assertES6(
                "var registry = new FinalizationRegistry(function(){}); Object.getPrototypeOf(registry) === FinalizationRegistry.prototype",
                true);
    }

    // ========== METHOD CALL CONTEXT TESTS ==========

    @Test
    public void finalizationRegistryMethodsOnWrongThis() {
        assertTypeError("FinalizationRegistry.prototype.register.call({}, {}, 'data')");
    }

    @Test
    public void finalizationRegistryUnregisterWrongThis() {
        assertTypeError("FinalizationRegistry.prototype.unregister.call({}, 'token')");
    }

    // ========== PROPERTY DESCRIPTOR TESTS ==========

    @Test
    public void finalizationRegistryRegisterPropertyDescriptor() {
        assertES6(
                "var desc = Object.getOwnPropertyDescriptor(FinalizationRegistry.prototype, 'register'); desc.writable === true && desc.enumerable === false && desc.configurable === true",
                true);
    }

    @Test
    public void finalizationRegistryUnregisterPropertyDescriptor() {
        assertES6(
                "var desc = Object.getOwnPropertyDescriptor(FinalizationRegistry.prototype, 'unregister'); desc.writable === true && desc.enumerable === false && desc.configurable === true",
                true);
    }

    @Test
    public void finalizationRegistryToStringTagDescriptor() {
        assertES6(
                "var desc = Object.getOwnPropertyDescriptor(FinalizationRegistry.prototype, Symbol.toStringTag); desc.value === 'FinalizationRegistry' && desc.writable === false && desc.enumerable === false && desc.configurable === true",
                true);
    }

    // ========== ES VERSION AVAILABILITY TESTS ==========

    @Test
    public void finalizationRegistryNotAvailableInES5() {
        assertES18("typeof FinalizationRegistry", "undefined");
    }

    @Test
    public void finalizationRegistryAvailableInES6() {
        assertES6("typeof FinalizationRegistry", "function");
    }

    // ========== STRESS AND EDGE CASES ==========

    @Test
    public void finalizationRegistryWithManyArguments() {
        assertES6(
                "var registry = new FinalizationRegistry(function(){}, 'extra', 'args', 'ignored'); registry instanceof FinalizationRegistry",
                true);
    }

    @Test
    public void finalizationRegistryRegisterWithExtraArguments() {
        assertES6(
                "var registry = new FinalizationRegistry(function(){}); var obj = {}; registry.register(obj, 'cleanup data', 'token', 'extra', 'args'); true",
                true);
    }

    @Test
    public void finalizationRegistryUnregisterWithExtraArguments() {
        assertES6(
                "var registry = new FinalizationRegistry(function(){}); registry.unregister('token', 'extra', 'args')",
                false);
    }
}