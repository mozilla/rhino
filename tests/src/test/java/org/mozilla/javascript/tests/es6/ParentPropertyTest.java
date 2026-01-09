/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 * Tests for the __parent__ property.
 */
package org.mozilla.javascript.tests.es6;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

public class ParentPropertyTest {

    @Test
    public void parentGet() {
        // __parent__ was removed in 2010 from the browsers
        // https://whereswalden.com/2010/05/07/spidermonkey-change-du-jour-the-special-__parent__-property-has-been-removed/
        String script = "var a = {};" + "'' + a.__parent__;";

        Utils.assertWithAllModes_1_8("[object topLevel]", script);
        Utils.assertWithAllModes_ES6("undefined", script);
    }
}
