/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

/**
 * Test for setting a property defined in prototype chain.
 *
 * @author Ronald Brill
 */
public class PutPropertyTest {

    @Test
    public void setPropKnownAtPrototype() throws Exception {
        final String script =
                "var WithArrayPrototype = function(array) {\n"
                        + "  this.length = array.length;\n"
                        + "  return this;\n"
                        + "}\n"
                        + "var nlp = WithArrayPrototype.prototype = [];\n"
                        + "var test = new WithArrayPrototype(['abc']);\n"
                        + "'' + nlp.length + ' # ' + test.length";

        Utils.assertWithAllModes("0 # 1", script);
    }
}
