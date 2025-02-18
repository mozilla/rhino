/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 * Tests for Function.prototype.apply method
 */
package org.mozilla.javascript.tests.es5;

import org.junit.Test;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.testutils.Utils;

public class FunctionApplyArrayLikeArguments {

    @Test
    public void arrayLikeArgumentsOfFunctionApply() {
        Utils.assertWithAllModes(
                Undefined.instance,
                "function test() { return arguments[0]; } test.apply(this, {});");

        Utils.assertWithAllModes(
                "banana",
                "function test() { return arguments[0]; } test.apply(this, {'length':1, '0':'banana'});");

        Utils.assertWithAllModes(
                "lala",
                "function test() { return arguments[0]; } test.apply(this, {'length':'1', '0':'lala'});");

        Utils.assertEcmaError(
                "TypeError: second argument to Function.prototype.apply must be an array",
                "function test() { return arguments[0]; } test.apply(2,2);");

        Utils.assertWithAllModes(
                Undefined.instance,
                "function test() { return arguments[0]; } test.apply(this,{'length':'abc', '0':'banana'});");

        Utils.assertWithAllModes(
                Undefined.instance,
                "function test() { return arguments[0]; } test.apply(this,{'length':function(){return 1;}, '0':'banana'});");
    }
}
