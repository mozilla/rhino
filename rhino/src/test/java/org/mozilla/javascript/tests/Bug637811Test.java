/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.testutils.Utils;

/**
 * @author André Bargull
 */
public class Bug637811Test {

    @Test
    public void test() {
        Utils.assertWithAllModes(
                Utils.contextFactoryWithFeatures(
                        Context.FEATURE_STRICT_MODE, Context.FEATURE_WARNING_AS_ERROR),
                Context.VERSION_ES6,
                null,
                Undefined.instance,
                "var x = 0; bar: while (x < 0) { x = x + 1; }");
    }
}
