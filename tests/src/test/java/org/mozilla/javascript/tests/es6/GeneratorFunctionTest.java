/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

/*
 * Tests for GeneratorFunction
 */
public class GeneratorFunctionTest {

    @Test
    public void arguments() {
        String code =
                "function* foo() { yield 'Hello!'; };\n"
                        + "'' + Object.getOwnPropertyDescriptor(foo, 'arguments');";
        Utils.assertWithAllModes_ES6("undefined", code);
    }

    @Test
    public void argumentsAccess() {
        String code =
                "function* foo() { yield 'Hello!'; };\n"
                        + "let res = '';\n"
                        + "try { foo.arguments; res += 'no ex'; } catch (e) { res += e.message; };\n"
                        + "try { foo.arguments = 7; res += ' no ex'; } catch (e) { res += ' ' + e.message; };";
        // todo Utils.assertWithAllModes_ES6("This operation is not allowed. This operation is not
        // allowed.", code);
        Utils.assertWithAllModes_ES6("no ex no ex", code);
    }
}
