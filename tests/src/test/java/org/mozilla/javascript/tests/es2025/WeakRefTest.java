/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es2025;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

public class WeakRefTest {

    private void assertTypeError(String script) {
        Utils.assertWithAllModes_ES6(
                "try { " + script + "; false; } catch(e) { e instanceof TypeError; }", true);
    }

    @Test
    public void weakRefConstructor() {
        Utils.assertWithAllModes_ES6(
                "var obj = { value: 42 }; var ref = new WeakRef(obj); ref instanceof WeakRef",
                true);
    }

    @Test
    public void weakRefDeref() {
        String script =
                "var obj = { value: 42 }; "
                        + "var ref = new WeakRef(obj); "
                        + "var derefed = ref.deref(); "
                        + "derefed === obj && derefed.value === 42";
        Utils.assertWithAllModes_ES6(script, true);
    }

    @Test
    public void weakRefDerefMultipleTimes() {
        Utils.assertWithAllModes_ES6(
                "var obj = { value: 42 }; var ref = new WeakRef(obj); ref.deref() === ref.deref()",
                true);
    }

    @Test
    public void weakRefToStringTag() {
        Utils.assertWithAllModes_ES6(
                "var ref = new WeakRef({}); Object.prototype.toString.call(ref)",
                "[object WeakRef]");
    }

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

    @Test
    public void weakRefWithArray() {
        Utils.assertWithAllModes_ES6(
                "var arr = [1,2,3]; var ref = new WeakRef(arr); ref.deref() === arr", true);
    }

    @Test
    public void weakRefWithFunction() {
        Utils.assertWithAllModes_ES6(
                "var fn = function(){}; var ref = new WeakRef(fn); ref.deref() === fn", true);
    }

    @Test
    public void weakRefWithRegExp() {
        Utils.assertWithAllModes_ES6(
                "var rx = /test/; var ref = new WeakRef(rx); ref.deref() === rx", true);
    }

    @Test
    public void weakRefTypeofFunction() {
        Utils.assertWithAllModes_ES6("typeof WeakRef === 'function'", true);
    }

    @Test
    public void weakRefPrototypeDeref() {
        Utils.assertWithAllModes_ES6("typeof WeakRef.prototype.deref === 'function'", true);
    }

    @Test
    public void weakRefConstructorLength() {
        Utils.assertWithAllModes_ES6("WeakRef.length === 1", true);
    }

    @Test
    public void weakRefConstructorName() {
        Utils.assertWithAllModes_ES6("WeakRef.name === 'WeakRef'", true);
    }

    @Test
    public void weakRefDerefLength() {
        Utils.assertWithAllModes_ES6("WeakRef.prototype.deref.length === 0", true);
    }

    @Test
    public void weakRefHasCorrectPrototype() {
        Utils.assertWithAllModes_ES6(
                "var ref = new WeakRef({}); Object.getPrototypeOf(ref) === WeakRef.prototype",
                true);
    }

    @Test
    public void weakRefDerefCallContext() {
        assertTypeError("WeakRef.prototype.deref.call({})");
    }

    @Test
    public void weakRefDerefApplyContext() {
        assertTypeError("WeakRef.prototype.deref.apply(null)");
    }

    @Test
    public void weakRefConstructorPropertyDescriptor() {
        Utils.assertWithAllModes_ES6(
                "var desc = Object.getOwnPropertyDescriptor(WeakRef.prototype, 'constructor'); desc.writable === true && desc.enumerable === false && desc.configurable === true",
                true);
    }

    @Test
    public void weakRefDerefPropertyDescriptor() {
        Utils.assertWithAllModes_ES6(
                "var desc = Object.getOwnPropertyDescriptor(WeakRef.prototype, 'deref'); desc.writable === true && desc.enumerable === false && desc.configurable === true",
                true);
    }

    @Test
    public void weakRefToStringTagDescriptor() {
        Utils.assertWithAllModes_ES6(
                "var desc = Object.getOwnPropertyDescriptor(WeakRef.prototype, Symbol.toStringTag); desc.value === 'WeakRef' && desc.writable === false && desc.enumerable === false && desc.configurable === true",
                true);
    }

    @Test
    public void weakRefNotAvailableInES5() {
        Utils.assertWithAllModes_1_8("typeof WeakRef", "undefined");
    }

    @Test
    public void weakRefAvailableInES6() {
        Utils.assertWithAllModes_ES6("typeof WeakRef", "function");
    }

    @Test
    public void weakRefWithManyArguments() {
        Utils.assertWithAllModes_ES6(
                "var obj = {}; var ref = new WeakRef(obj, 'extra', 'args', 'ignored'); ref.deref() === obj",
                true);
    }

    @Test
    public void weakRefDerefWithArguments() {
        Utils.assertWithAllModes_ES6(
                "var obj = {}; var ref = new WeakRef(obj); ref.deref('arg1', 'arg2') === obj",
                true);
    }
}
