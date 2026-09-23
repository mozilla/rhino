/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/** Tests for the global {@code escape} function. */
public class GlobalEscapeTest {

    @Test
    public void escapeKeepsUnescapedPrefix() {
        Utils.assertWithAllModes("ab%20cd", "escape('ab cd')");
    }

    @Test
    public void escapeLongerUnescapedPrefix() {
        Utils.assertWithAllModes("hello%20world%21", "escape('hello world!')");
    }

    @Test
    public void escapeLeadingEscapedChar() {
        Utils.assertWithAllModes("%20abc", "escape(' abc')");
    }

    @Test
    public void escapeAllUnescaped() {
        Utils.assertWithAllModes("abcABC123", "escape('abcABC123')");
    }

    @Test
    public void escapeUnicode() {
        Utils.assertWithAllModes("a%u1234b", "escape('a\\u1234b')");
    }
}
