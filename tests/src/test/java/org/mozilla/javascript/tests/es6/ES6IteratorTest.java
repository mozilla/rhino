/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.tests.Utils;

public class ES6IteratorTest {

    @Test
    public void valueDone() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final Scriptable scope = cx.initStandardObjects();

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "  var res = '';\n"
                                            + "  var arr = ['x'];\n"
                                            + "  var arrIter = arr[Symbol.iterator]();\n"
                                            + "  for (var p in arrIter.next()) {\n"
                                            + "    res = res + p + ' ';\n"
                                            + "  }\n",
                                    "test",
                                    1,
                                    null);
                    // this is the order used by all current browsers
                    assertEquals("value done ", result);

                    return null;
                });
    }
}
