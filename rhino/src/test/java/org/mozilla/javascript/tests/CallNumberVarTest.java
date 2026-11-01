/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

public class CallNumberVarTest {

    @Test
    public void callNumberVar() {
        Utils.assertWithAllModes_ES6(
                true,
                "function f() { var x = 1; try { x(); } catch (e) { return e instanceof TypeError; } }"
                        + " f();");
    }

    @Test
    public void optionalCallNumberVar() {
        Utils.assertWithAllModes_ES6(
                true,
                "function f() { var x = 1; try { x?.(); } catch (e) { return e instanceof TypeError; } }"
                        + " f();");
    }
}
