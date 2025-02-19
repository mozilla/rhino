/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

/** Tests for Arguments support. */
public class ArgumentsTest {

    @Test
    public void argumentsSymbolIterator() {
        String code =
                "function foo() {"
                        + "  return arguments[Symbol.iterator] === Array.prototype.values;"
                        + "}"
                        + "foo()";

        Utils.assertWithAllModes_ES6(true, code);
    }

    @Test
    public void argumentsSymbolIterator2() {
        String code =
                "function foo() {"
                        + "  return arguments[Symbol.iterator] === [][Symbol.iterator];"
                        + "}"
                        + "foo()";

        Utils.assertWithAllModes_ES6(true, code);
    }

    @Test
    public void argumentsForOf() {
        String code =
                "function foo() {"
                        + "  var res = '';"
                        + "  for (arg of arguments) {"
                        + "    res += arg;"
                        + "  }"
                        + "  return res;"
                        + "}"
                        + "foo(1, 2, 3, 5)";

        Utils.assertWithAllModes_ES6("1235", code);
    }
}
