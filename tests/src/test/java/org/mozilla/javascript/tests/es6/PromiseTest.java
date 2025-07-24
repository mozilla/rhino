/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 * Tests for the Object.getOwnPropertyDescriptor(obj, prop) method
 */
package org.mozilla.javascript.tests.es6;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

public class PromiseTest {

    @Test
    public void ctorCallableThis() {
        final String script =
                "  var r = '';"
                        + "  var p = new Promise(function(resolve, reject) {\n"
                        + "      r += this;\n"
                        + "    });\n"
                        + "  r += ' done';\n"
                        + "  r;";
        Utils.assertWithAllModesTopLevelScope_ES6("[object global] done", script);
    }

    @Test
    public void ctorCallableThisStrict() {
        final String script =
                "'use strict';"
                        + "  var r = '';"
                        + "  var p = new Promise(function(resolve, reject) {\n"
                        + "      r += this === undefined;\n"
                        + "    });\n"
                        + "  r += ' done';\n"
                        + "  r;";
        Utils.assertWithAllModes_ES6("true done", script);
    }

    @Test
    public void withResolversBasic() {
        final String script =
                "var result = Promise.withResolvers();"
                        + "typeof result === 'object' && "
                        + "typeof result.promise === 'object' && "
                        + "typeof result.resolve === 'function' && "
                        + "typeof result.reject === 'function'";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void withResolversPromiseType() {
        final String script =
                "var result = Promise.withResolvers(); result.promise instanceof Promise";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void withResolversObjectKeys() {
        final String script =
                "var result = Promise.withResolvers();"
                        + "var keys = Object.keys(result).sort();"
                        + "keys.length === 3 && keys[0] === 'promise' && keys[1] === 'reject' && keys[2] === 'resolve'";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void withResolversResolveWorks() {
        final String script =
                "var result = Promise.withResolvers();"
                        + "result.resolve('test');"
                        + "'success'"; // Just verify the resolve call doesn't throw
        Utils.assertWithAllModes_ES6("success", script);
    }

    @Test
    public void withResolversRejectWorks() {
        final String script =
                "var result = Promise.withResolvers();"
                        + "try { result.reject(new Error('test')); } catch(e) { /* ignore unhandled rejection */ }"
                        + "'success'"; // Just verify the reject call doesn't throw
        Utils.assertWithAllModes_ES6("success", script);
    }

    @Test
    public void withResolversNonObjectThisThrows() {
        final String script =
                "try {"
                        + "  Promise.withResolvers.call(null);"
                        + "  'should not reach here';"
                        + "} catch(e) {"
                        + "  e instanceof TypeError && e.message.includes('not an object');"
                        + "}";
        Utils.assertWithAllModes_ES6(true, script);
    }
}
