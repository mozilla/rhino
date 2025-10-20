/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

/** Tests for ThrowTypeError support. */
public class ThrowTypeErrorTest {

    @Test
    public void isFunction() {
        String code =
                "let args = function() { 'use strict'; return arguments; }();"
                        + "let desc = Object.getOwnPropertyDescriptor(args, 'callee');"
                        + "let ThrowTypeError = desc.get"
                        + "'' + typeof ThrowTypeError";

        Utils.assertWithAllModes_ES6("function", code);
    }

    @Test
    public void isExtensible() {
        String code =
                "let args = function() { 'use strict'; return arguments; }();"
                        + "let ThrowTypeError = Object.getOwnPropertyDescriptor(args, 'callee').get;"
                        + "Object.isExtensible(ThrowTypeError)";

        Utils.assertWithAllModes_ES6(false, code);
    }

    @Test
    public void isSealed() {
        String code =
                "let args = function() { 'use strict'; return arguments; }();"
                        + "let ThrowTypeError = Object.getOwnPropertyDescriptor(args, 'callee').get;"
                        + "Object.isSealed(ThrowTypeError)";

        Utils.assertWithAllModes_ES6(true, code);
    }

    @Test
    public void isFrozen() {
        String code =
                "let args = function() { 'use strict'; return arguments; }();"
                        + "let ThrowTypeError = Object.getOwnPropertyDescriptor(args, 'callee').get;"
                        + "Object.isFrozen(ThrowTypeError)";

        Utils.assertWithAllModes_ES6(true, code);
    }

    @Test
    public void length() {
        String code =
                "let args = function() { 'use strict'; return arguments; }();"
                        + "let ThrowTypeError = Object.getOwnPropertyDescriptor(args, 'callee').get;"
                        + "  let res = '';"
                        + "  let desc = Object.getOwnPropertyDescriptor(ThrowTypeError, 'length');"
                        + "  res += desc.value;\n"
                        + "  res += ' W-' + desc.writable;\n"
                        + "  res += ' E-' + desc.enumerable;\n"
                        + "  res += ' C-' + desc.configurable;\n"
                        + "res";

        Utils.assertWithAllModes_ES6("0 W-false E-false C-false", code);
    }

    @Test
    public void name() {
        String code =
                "let args = function() { 'use strict'; return arguments; }();"
                        + "let ThrowTypeError = Object.getOwnPropertyDescriptor(args, 'callee').get;"
                        + "  let res = '';"
                        + "  let desc = Object.getOwnPropertyDescriptor(ThrowTypeError, 'name');"
                        + "  res += '#' + desc.value + '#';\n"
                        + "  res += ' W-' + desc.writable;\n"
                        + "  res += ' E-' + desc.enumerable;\n"
                        + "  res += ' C-' + desc.configurable;\n"
                        + "res";

        Utils.assertWithAllModes_ES6("## W-false E-false C-false", code);
    }

    @Test
    public void prototype() {
        String code =
                "let args = function() { 'use strict'; return arguments; }();"
                        + "let ThrowTypeError = Object.getOwnPropertyDescriptor(args, 'callee').get;"
                        + "Function.prototype === Object.getPrototypeOf(ThrowTypeError)";

        Utils.assertWithAllModes_ES6(true, code);
    }

    @Test
    public void ownPropertyNames() {
        String code =
                "let args = function() { 'use strict'; return arguments; }();"
                        + "let ThrowTypeError = Object.getOwnPropertyDescriptor(args, 'callee').get;"
                        + "'' + Object.getOwnPropertyNames(ThrowTypeError)";

        Utils.assertWithAllModes_ES6("length,name", code);
    }

    @Test
    public void noCaller() {
        String code =
                "let args = function() { 'use strict'; return arguments; }();"
                        + "let ThrowTypeError = Object.getOwnPropertyDescriptor(args, 'callee').get;"
                        + "Object.prototype.hasOwnProperty.call(ThrowTypeError, 'caller')";

        Utils.assertWithAllModes_ES6(false, code);
    }

    @Test
    public void noArguments() {
        String code =
                "let args = function() { 'use strict'; return arguments; }();"
                        + "let ThrowTypeError = Object.getOwnPropertyDescriptor(args, 'callee').get;"
                        + "Object.prototype.hasOwnProperty.call(ThrowTypeError, 'arguments')";

        Utils.assertWithAllModes_ES6(false, code);
    }

    @Test
    public void toStringCall() {
        String code =
                "let args = function() { 'use strict'; return arguments; }();"
                        + "let ThrowTypeError = Object.getOwnPropertyDescriptor(args, 'callee').get;"
                        + "ThrowTypeError.toString()";

        Utils.assertWithAllModes_ES6("function () {\n\t[native code]\n}\n", code);
    }
}
