/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es2025;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

public class WeakRefTest {

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
    public void weakRefConstructor() {
        assertES6(
                "var obj = { value: 42 }; var ref = new WeakRef(obj); ref instanceof WeakRef",
                true);
    }

    @Test
    public void weakRefDeref() {
        assertES6(
                "var obj = { value: 42 }; var ref = new WeakRef(obj); var derefed = ref.deref(); derefed === obj && derefed.value === 42",
                true);
    }

    @Test
    public void weakRefDerefMultipleTimes() {
        assertES6(
                "var obj = { value: 42 }; var ref = new WeakRef(obj); ref.deref() === ref.deref()",
                true);
    }

    @Test
    public void weakRefToStringTag() {
        assertES6(
                "var ref = new WeakRef({}); Object.prototype.toString.call(ref)",
                "[object WeakRef]");
    }

    // ========== ERROR CONDITION TESTS ==========

    @Test
    public void weakRefRequiresNew() {
        assertTypeError("WeakRef({})");
    }

    @Test
    public void weakRefRequiresObject() {
        assertTypeError("new WeakRef(42)");
    }

    @Test
    public void weakRefNullTarget() {
        assertTypeError("new WeakRef(null)");
    }

    @Test
    public void weakRefUndefinedTarget() {
        assertTypeError("new WeakRef(undefined)");
    }

    @Test
    public void weakRefNoArguments() {
        assertTypeError("new WeakRef()");
    }

    @Test
    public void weakRefRejectsSymbol() {
        assertTypeError("new WeakRef(Symbol('test'))");
    }

    // ========== CONSTRUCTOR TESTS - DIFFERENT OBJECT TYPES ==========

    @Test
    public void weakRefWithArray() {
        assertES6("var arr = [1,2,3]; var ref = new WeakRef(arr); ref.deref() === arr", true);
    }

    @Test
    public void weakRefWithFunction() {
        assertES6("var fn = function(){}; var ref = new WeakRef(fn); ref.deref() === fn", true);
    }

    @Test
    public void weakRefWithRegExp() {
        assertES6("var rx = /test/; var ref = new WeakRef(rx); ref.deref() === rx", true);
    }

    // ========== PROTOTYPE AND CONSTRUCTOR PROPERTIES ==========

    @Test
    public void weakRefTypeofFunction() {
        assertES6("typeof WeakRef === 'function'", true);
    }

    @Test
    public void weakRefPrototypeDeref() {
        assertES6("typeof WeakRef.prototype.deref === 'function'", true);
    }

    @Test
    public void weakRefConstructorLength() {
        assertES6("WeakRef.length === 1", true);
    }

    @Test
    public void weakRefConstructorName() {
        assertES6("WeakRef.name === 'WeakRef'", true);
    }

    @Test
    public void weakRefDerefLength() {
        assertES6("WeakRef.prototype.deref.length === 0", true);
    }

    @Test
    public void weakRefHasCorrectPrototype() {
        assertES6(
                "var ref = new WeakRef({}); Object.getPrototypeOf(ref) === WeakRef.prototype",
                true);
    }

    // ========== METHOD CALL CONTEXT TESTS ==========

    @Test
    public void weakRefDerefCallContext() {
        assertTypeError("WeakRef.prototype.deref.call({})");
    }

    @Test
    public void weakRefDerefApplyContext() {
        assertTypeError("WeakRef.prototype.deref.apply(null)");
    }

    // ========== PROPERTY DESCRIPTOR TESTS ==========

    @Test
    public void weakRefConstructorPropertyDescriptor() {
        assertES6(
                "var desc = Object.getOwnPropertyDescriptor(WeakRef.prototype, 'constructor'); desc.writable === true && desc.enumerable === false && desc.configurable === true",
                true);
    }

    @Test
    public void weakRefDerefPropertyDescriptor() {
        assertES6(
                "var desc = Object.getOwnPropertyDescriptor(WeakRef.prototype, 'deref'); desc.writable === true && desc.enumerable === false && desc.configurable === true",
                true);
    }

    @Test
    public void weakRefToStringTagDescriptor() {
        assertES6(
                "var desc = Object.getOwnPropertyDescriptor(WeakRef.prototype, Symbol.toStringTag); desc.value === 'WeakRef' && desc.writable === false && desc.enumerable === false && desc.configurable === true",
                true);
    }

    // ========== ES VERSION AVAILABILITY TESTS ==========

    @Test
    public void weakRefNotAvailableInES5() {
        assertES18("typeof WeakRef", "undefined");
    }

    @Test
    public void weakRefAvailableInES6() {
        assertES6("typeof WeakRef", "function");
    }

    // ========== STRESS AND EDGE CASES ==========

    @Test
    public void weakRefWithManyArguments() {
        assertES6(
                "var obj = {}; var ref = new WeakRef(obj, 'extra', 'args', 'ignored'); ref.deref() === obj",
                true);
    }

    @Test
    public void weakRefDerefWithArguments() {
        assertES6(
                "var obj = {}; var ref = new WeakRef(obj); ref.deref('arg1', 'arg2') === obj",
                true);
    }
}
