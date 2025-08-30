/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mozilla.javascript.tests.es2025;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

/** Tests for ES2025 Error.isError */
public class ErrorIsErrorTest {

    @Test
    public void testErrorInstances() {
        Utils.assertWithAllModes_ES6(true, "Error.isError(new Error())");
        Utils.assertWithAllModes_ES6(true, "Error.isError(new TypeError())");
        Utils.assertWithAllModes_ES6(true, "Error.isError(new RangeError())");
        Utils.assertWithAllModes_ES6(true, "Error.isError(new ReferenceError())");
        Utils.assertWithAllModes_ES6(true, "Error.isError(new SyntaxError())");
        Utils.assertWithAllModes_ES6(true, "Error.isError(new EvalError())");
        Utils.assertWithAllModes_ES6(true, "Error.isError(new URIError())");
    }

    @Test
    public void testAggregateError() {
        final String script =
                "if (typeof AggregateError !== 'undefined') {"
                        + " Error.isError(new AggregateError([]));"
                        + "} else {"
                        + " true;"
                        + "}";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void testPrimitives() {
        Utils.assertWithAllModes_ES6(false, "Error.isError(undefined)");
        Utils.assertWithAllModes_ES6(false, "Error.isError(null)");
        Utils.assertWithAllModes_ES6(false, "Error.isError(true)");
        Utils.assertWithAllModes_ES6(false, "Error.isError(false)");
        Utils.assertWithAllModes_ES6(false, "Error.isError(0)");
        Utils.assertWithAllModes_ES6(false, "Error.isError(1)");
        Utils.assertWithAllModes_ES6(false, "Error.isError(-1)");
        Utils.assertWithAllModes_ES6(false, "Error.isError(NaN)");
        Utils.assertWithAllModes_ES6(false, "Error.isError('')");
        Utils.assertWithAllModes_ES6(false, "Error.isError('string')");
        Utils.assertWithAllModes_ES6(false, "Error.isError(Symbol())");
        Utils.assertWithAllModes_ES6(false, "Error.isError(Symbol.iterator)");
    }

    @Test
    public void testNonErrorObjects() {
        Utils.assertWithAllModes_ES6(false, "Error.isError({})");
        Utils.assertWithAllModes_ES6(false, "Error.isError([])");
        Utils.assertWithAllModes_ES6(false, "Error.isError(function() {})");
        Utils.assertWithAllModes_ES6(false, "Error.isError(new Date())");
        Utils.assertWithAllModes_ES6(false, "Error.isError(new RegExp())");
        Utils.assertWithAllModes_ES6(false, "Error.isError(new Map())");
        Utils.assertWithAllModes_ES6(false, "Error.isError(new Set())");
    }

    @Test
    public void testFakeErrors() {
        Utils.assertWithAllModes_ES6(false, "Error.isError({name: 'Error', message: 'fake'})");
        Utils.assertWithAllModes_ES6(false, "Error.isError({__proto__: Error.prototype})");
        Utils.assertWithAllModes_ES6(false, "Error.isError(Object.create(Error.prototype))");
    }

    @Test
    public void testNoArguments() {
        Utils.assertWithAllModes_ES6(false, "Error.isError()");
    }

    @Test
    public void testFunctionProperties() {
        Utils.assertWithAllModes_ES6("function", "typeof Error.isError");
        Utils.assertWithAllModes_ES6(1, "Error.isError.length");
        Utils.assertWithAllModes_ES6("isError", "Error.isError.name");
    }
}
