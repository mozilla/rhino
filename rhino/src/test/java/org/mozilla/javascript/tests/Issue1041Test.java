/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.TopLevel;

public class Issue1041Test {
    @Test
    public void quantifierWithMax0() {
        try (Context cx = Context.enter()) {
            TopLevel scope = cx.initStandardObjects();
            Boolean result =
                    (Boolean) cx.evaluateString(scope, "/ab{0}c/.test('abc')", "<eval>", 1, null);
            Assertions.assertFalse(result);

            result = (Boolean) cx.evaluateString(scope, "/ab{0}c/.test('ac')", "<eval>", 1, null);
            Assertions.assertTrue(result);
        }
    }
}
