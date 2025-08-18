/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/** */
package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;

public class ContextCloseTest {

    @Test
    public void testContextCloseIdempotent() throws Exception {
        Context cx1 = Context.enter();
        assertEquals(cx1, Context.getCurrentContext());

        Context cx2 = Context.enter();
        assertEquals(cx1, Context.getCurrentContext());
        cx2.close();

        assertEquals(cx1, Context.getCurrentContext());

        cx2.close();
        cx2.close();

        assertEquals(cx1, Context.getCurrentContext());

        cx1.close();
        assertEquals(null, Context.getCurrentContext());

        cx1.close();
        cx1.close();
    }
}
