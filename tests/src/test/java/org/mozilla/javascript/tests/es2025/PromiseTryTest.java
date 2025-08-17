/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es2025;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

public class PromiseTryTest {

    @Test
    public void promiseTryBasic() {
        final String script = "typeof Promise.try === 'function'";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void promiseTrySyncReturnValue() {
        final String script = "var p = Promise.try(() => 42);" + "p instanceof Promise";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void promiseTrySyncThrow() {
        final String script =
                "var p = Promise.try(() => { throw new Error('test'); });" + "p instanceof Promise";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void promiseTryWithArguments() {
        final String script =
                "var result = null;"
                        + "Promise.try((a, b) => { result = a + b; }, 1, 2);"
                        + "result";
        Utils.assertWithAllModes_ES6(3, script);
    }

    @Test
    public void promiseTryNonFunction() {
        final String script =
                "try {"
                        + "  Promise.try(42);"
                        + "  'should not reach here';"
                        + "} catch(e) {"
                        + "  e instanceof TypeError;"
                        + "}";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void promiseTryReturnsPromise() {
        final String script =
                "var p = Promise.try(() => Promise.resolve(42));" + "p instanceof Promise";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void promiseTryNonObjectThis() {
        final String script =
                "try {"
                        + "  Promise.try.call(null, () => 42);"
                        + "  'should not reach here';"
                        + "} catch(e) {"
                        + "  e instanceof TypeError && e.message.includes('Expected argument of type object');"
                        + "}";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void promiseTryCustomConstructor() {
        final String script =
                "var CustomPromise = function(executor) {"
                        + "  return new Promise(executor);"
                        + "};"
                        + "var p = Promise.try.call(CustomPromise, () => 42);"
                        + "p instanceof Promise";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void promiseTryThisValue() {
        final String script =
                "var thisValue = null;"
                        + "Promise.try(function() { thisValue = this; });"
                        + "thisValue === undefined";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void promiseTryMultipleArgs() {
        final String script =
                "var args = null;"
                        + "Promise.try(function() { args = Array.from(arguments); }, 'a', 'b', 'c');"
                        + "args.join(',')";
        Utils.assertWithAllModes_ES6("a,b,c", script);
    }
}
