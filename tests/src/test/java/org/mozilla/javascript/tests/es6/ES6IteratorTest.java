/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import org.junit.Test;
import org.mozilla.javascript.tests.Utils;

public class ES6IteratorTest {

    @Test
    public void valueDone() {
        String code =
                "  var res = '';\n"
                        + "  var arr = ['x'];\n"
                        + "  var arrIter = arr[Symbol.iterator]();\n"
                        + "  for (var p in arrIter.next()) {\n"
                        + "    res = res + p + ' ';\n"
                        + "  }\n";

        Utils.assertWithAllModes_ES6("value done ", code);
    }
}
