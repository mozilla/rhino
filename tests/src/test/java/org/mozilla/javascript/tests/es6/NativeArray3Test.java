/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import org.junit.Test;
import org.mozilla.javascript.tests.Utils;

/** Tests for NativeArray support. */
public class NativeArray3Test {

    @Test
    public void iteratorPrototype() {
        String code = "Array.prototype.values === [][Symbol.iterator]";

        Utils.assertWithAllOptimizationLevelsES6(true, code);
    }

    @Test
    public void iteratorInstances() {
        String code = "[1, 2][Symbol.iterator] === [][Symbol.iterator]";

        Utils.assertWithAllOptimizationLevelsES6(true, code);
    }

    @Test
    public void iteratorPrototypeName() {
        String code = "Array.prototype.values.name;";

        Utils.assertWithAllOptimizationLevelsES6("values", code);
    }

    @Test
    public void iteratorInstanceName() {
        String code = "[][Symbol.iterator].name;";

        Utils.assertWithAllOptimizationLevelsES6("values", code);
    }

    @Test
    public void redefineIterator() {
        String code =
                "var res = '';\n"
                        + "var arr = ['hello', 'world'];\n"
                        + "res += arr[Symbol.iterator].toString().includes('return i;');\n"
                        + "res += ' - ';\n"
                        + "arr[Symbol.iterator] = function () { return i; };\n"
                        + "res += arr[Symbol.iterator].toString().includes('return i;');\n"
                        + "res += ' - ';\n"
                        + "delete arr[Symbol.iterator];\n"
                        + "res += arr[Symbol.iterator].toString().includes('return i;');\n"
                        + "res;";

        Utils.assertWithAllOptimizationLevelsES6("false - true - false", code);
    }
}
