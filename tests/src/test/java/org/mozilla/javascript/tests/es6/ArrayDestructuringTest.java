/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import org.junit.Test;
import org.mozilla.javascript.tests.Utils;

public class ArrayDestructuringTest {

    /** Test for issue #662. There was a ClassCastException. */
    @Test
    public void strangeCase() {
        Utils.assertWithAllModes("", "var a = ''; [a.x] = ''; a");
    }

    /** Test for issue #662. */
    @Test
    public void strangeCase2() {
        Utils.assertWithAllModes("", "[1..h]=''");
    }

    /** Test for issue #662. */
    @Test
    public void strangeCase3() {
        Utils.assertWithAllModes(123, "[0..toString.h] = [123]; Number.prototype.toString.h");
    }
}
