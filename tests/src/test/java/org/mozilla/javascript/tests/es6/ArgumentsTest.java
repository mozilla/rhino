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

    @Test
    public void argumentsCallee() {
        String code =
                "\"use strict\";\n"
                        + "function foo() {\n"
                        + "  let desc = Object.getOwnPropertyDescriptor(arguments, 'callee');\n"
                        + "  var res = '';\n"
                        + "  res += typeof desc.get + '/' + desc.get.name;\n"
                        + "  res += ' | '\n"
                        + "  res += typeof desc.set + '/' + desc.set.name;\n"
                        + "  res += ' | '\n"
                        + "  res += desc.get === desc.set;\n"
                        + "  return res;\n"
                        + "}\n"
                        + "foo()\n";

        Utils.assertWithAllModes_ES6("function/ | function/ | true", code);
    }

    @Test
    public void argumentsCalleeDifferentFunctions() {
        String code =
                "\"use strict\";\n"
                        + "function foo1() {\n"
                        + "  return Object.getOwnPropertyDescriptor(arguments, 'callee');\n"
                        + "}\n"
                        + "function foo2() {\n"
                        + "  return Object.getOwnPropertyDescriptor(arguments, 'callee');\n"
                        + "}\n"
                        + "let desc1 = foo1();\n"
                        + "let desc2 = foo2();\n"
                        + "let res = '';\n"
                        + "res += desc1.get === desc2.get;\n"
                        + "res += ' | '\n"
                        + "res += desc1.set === desc2.set;\n";

        Utils.assertWithAllModes_ES6("true | true", code);
    }

    @Test
    public void argumentsCaller() {
        // this no longer works in real browsers because
        // Object.getOwnPropertyDescriptor(arguments, "caller") returns undefined
        String code =
                "\"use strict\";\n"
                        + "function foo() {\n"
                        + "  let desc = Object.getOwnPropertyDescriptor(arguments, 'caller');\n"
                        + "  var res = '';\n"
                        + "  res += typeof desc.get + '/' + desc.get.name;\n"
                        + "  res += ' | '\n"
                        + "  res += typeof desc.set + '/' + desc.set.name;\n"
                        + "  res += ' | '\n"
                        + "  res += desc.get === desc.set;\n"
                        + "  return res;\n"
                        + "}\n"
                        + "foo()\n";

        Utils.assertWithAllModes_ES6("function/ | function/ | true", code);
    }
}
