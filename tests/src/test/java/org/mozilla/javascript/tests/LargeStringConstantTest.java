/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/** Strings too long to be written as a single class file string constant. */
public class LargeStringConstantTest {

    private static final String LARGE = "a\\0\\u20ac".repeat(40_000);

    @Test
    public void largeStringLiteral() {
        Utils.assertWithAllModes_ES6(
                true, "var s = '" + LARGE + "'; s === 'a\\0\\u20ac'.repeat(40000)");
    }

    @Test
    public void largeTaggedTemplateLiteral() {
        Utils.assertWithAllModes_ES6(
                true,
                "function tag(s) { return s[0] === 'a\\0\\u20ac'.repeat(40000)"
                        + " && s.raw[0] === 'a\\\\0\\\\u20ac'.repeat(40000); }\n"
                        + "tag`"
                        + LARGE
                        + "`");
    }

    @Test
    public void largeFunctionSource() {
        Utils.assertWithAllModes_ES6(
                true, "function f() { return '" + LARGE + "'; }\n" + "f.toString().length > 65535");
    }
}
